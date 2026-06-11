package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber

class ExpenseRepositoryImpl(
    private val cloudExpenseDataSource: CloudExpenseDataSource,
    private val localExpenseDataSource: LocalExpenseDataSource,
    private val authenticationService: AuthenticationService,
    private val cloudStorageDataSource: CloudStorageDataSource,
    private val receiptStorageService: ReceiptStorageService,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    /**
     * Tracks active cloud subscription Jobs per groupId.
     * Prevents duplicate Firestore snapshot listeners from accumulating
     * when onStart fires multiple times (e.g., config changes, tab switches,
     * WhileSubscribed resubscriptions, flatMapLatest restarts).
     */
    private val cloudSubscriptionJobs = ConcurrentHashMap<String, Job>()

    /**
     * Tracks active receipt upload Jobs per expenseId.
     * Prevents duplicate/concurrent uploads of the same receipt.
     */
    private val receiptUploadJobs = ConcurrentHashMap<String, Job>()

    override suspend fun addExpense(groupId: String, expense: Expense) {
        val expenseWithMetadata = buildExpenseWithMetadata(groupId, expense)

        // Save to local first - UI updates instantly via Flow
        localExpenseDataSource.saveExpense(expenseWithMetadata)

        // Sync to cloud in background
        syncScope.launch {
            try {
                cloudExpenseDataSource.addExpense(groupId, expenseWithMetadata)
                localExpenseDataSource.updateSyncStatus(expenseWithMetadata.id, SyncStatus.SYNCED)
                Timber.d("Expense synced to cloud: ${expenseWithMetadata.id}")
                scheduleReceiptUpload(expenseWithMetadata)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Only downgrade to SYNC_FAILED if the snapshot listener has not already
                // confirmed the entity as SYNCED (guards against the ACK-loss race condition).
                val currentStatus = localExpenseDataSource.getExpenseById(expenseWithMetadata.id)?.syncStatus
                if (currentStatus == SyncStatus.PENDING_SYNC) {
                    localExpenseDataSource.updateSyncStatus(expenseWithMetadata.id, SyncStatus.SYNC_FAILED)
                }
                Timber.w(e, "Failed to sync expense to cloud")
            }
        }
    }

    /**
     * Saves a cash-funded expense with optimistic-locking conflict detection.
     *
     * Room-first: the expense is always saved immediately as PENDING_SYNC.
     * Then a **synchronous** Firestore transaction verifies that no concurrent write
     * has modified any consumed withdrawal since the FIFO preview was computed.
     *
     * - **Online + no conflict:** transaction commits → Room status updated to SYNCED → returns `true`.
     * - **Online + conflict:** Room write is rolled back → [CashConflictException] re-thrown.
     * - **Offline / network error:** expense stays SYNC_FAILED → returns `false`.
     *   The caller must NOT update withdrawal remaining amounts on `false`, since the
     *   Firestore transaction never ran; doing so would deduct withdrawals in the cloud
     *   without a matching expense document.
     */
    override suspend fun addCashExpense(
        groupId: String,
        expense: Expense,
        expectedRemainingAmounts: Map<String, Long>
    ): Boolean {
        val expenseWithMetadata = buildExpenseWithMetadata(groupId, expense)

        // Room-first (offline-first principle)
        localExpenseDataSource.saveExpense(expenseWithMetadata)

        // Synchronous Firestore transaction — conflict detection
        try {
            cloudExpenseDataSource.addExpenseWithCashPreconditions(
                groupId,
                expenseWithMetadata,
                expectedRemainingAmounts
            )
            localExpenseDataSource.updateSyncStatus(expenseWithMetadata.id, SyncStatus.SYNCED)
            Timber.d("Cash expense committed via Firestore transaction: ${expenseWithMetadata.id}")
            scheduleReceiptUpload(expenseWithMetadata)
            return true
        } catch (e: CashConflictException) {
            // Concurrent modification detected — rollback local write and surface to caller
            localExpenseDataSource.deleteExpense(expenseWithMetadata.id)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline or network error — keep the Room write so the user can proceed
            // offline-first; mark as SYNC_FAILED.
            // Return false so the caller does NOT deduct withdrawals in Room+cloud independently
            // (which would break cloud atomicity: withdrawals deducted without expense committed).
            val currentStatus = localExpenseDataSource.getExpenseById(expenseWithMetadata.id)?.syncStatus
            if (currentStatus == SyncStatus.PENDING_SYNC) {
                localExpenseDataSource.updateSyncStatus(expenseWithMetadata.id, SyncStatus.SYNC_FAILED)
            }
            Timber.w(e, "Firestore transaction failed for cash expense, keeping local state for offline retry")
            return false
        }
    }

    override suspend fun getExpenseById(expenseId: String): Expense? = localExpenseDataSource.getExpenseById(expenseId)

    override fun getExpenseByIdFlow(expenseId: String): Flow<Expense?> = localExpenseDataSource.getExpenseByIdFlow(
        expenseId
    )

    override suspend fun deleteExpense(groupId: String, expenseId: String) {
        val expense = localExpenseDataSource.getExpenseById(expenseId)
        val attachment = expense?.receiptAttachment

        // Delete from local first - UI updates instantly via Flow
        localExpenseDataSource.deleteExpense(expenseId)

        // Delete local file immediately (best-effort, non-fatal)
        if (attachment != null && attachment.localUri.isNotBlank()) {
            deleteLocalReceiptFileBestEffort(receiptStorageService, attachment.localUri)
        }

        // Always queue cloud deletion, even for PENDING_SYNC entities.
        // Firestore SDK guarantees write ordering: the queued SET (from addExpense)
        // executes before this DELETE when connectivity is restored.
        val remoteUrl = attachment?.remoteUrl?.takeIf { it.isNotBlank() }
        launchCloudExpenseDeletion(
            syncScope = syncScope,
            cloudExpenseDataSource = cloudExpenseDataSource,
            cloudStorageDataSource = cloudStorageDataSource,
            groupId = groupId,
            expenseId = expenseId,
            remoteUrl = remoteUrl
        )
    }

    /**
     * Returns a Flow of expenses for a group from local storage.
     * On start, subscribes to real-time cloud changes for multi-user sync.
     *
     * Uses a single shared subscription per groupId: any existing cloud listener
     * for this group is cancelled before starting a new one, preventing duplicate
     * snapshot listeners from accumulating across flatMapLatest restarts,
     * config changes, or WhileSubscribed resubscriptions.
     */
    override fun getGroupExpensesFlow(groupId: String): Flow<List<Expense>> =
        localExpenseDataSource.getExpensesByGroupIdFlow(groupId)
            .onStart {
                // Cancel any previous cloud subscription for this group to prevent duplicates.
                cloudSubscriptionJobs[groupId]?.cancel()
                cloudSubscriptionJobs[groupId] = syncScope.launch {
                    subscribeToCloudChanges(groupId)
                }
            }

    /**
     * Subscribes to real-time Firestore snapshot changes for a group's expenses.
     *
     * The Firestore snapshotListener fires whenever ANY user adds, modifies, or
     * deletes an expense in this group. Each snapshot represents the complete
     * authoritative state of the collection.
     *
     * We use [replaceExpensesForGroup] with a merge reconciliation strategy
     * (upsert remote + selective delete of stale) to safely reconcile the
     * local DB with the cloud snapshot. This handles:
     * - Additions by other users → new items appear locally
     * - Deletions by other users → stale items are removed locally
     * - Modifications by other users → items are updated locally
     * - Locally-created expenses not yet synced → preserved (not deleted)
     *
     * After reconciliation, [confirmPendingSyncExpenses] attempts to verify
     * any PENDING_SYNC items against the server. This handles the
     * PENDING_SYNC → SYNCED transition when the device comes back online
     * after an app restart (where the syncScope coroutine that would normally
     * call updateSyncStatus(SYNCED) was killed before completing).
     *
     * The Room Flow re-emits automatically after each reconciliation,
     * keeping the UI in sync across all devices in near real-time.
     */
    private suspend fun subscribeToCloudChanges(groupId: String) {
        try {
            cloudExpenseDataSource.getExpensesByGroupIdFlow(groupId)
                .collect { remoteExpenses ->
                    try {
                        Timber.d("Real-time sync: ${remoteExpenses.size} expenses for group $groupId")
                        localExpenseDataSource.replaceExpensesForGroup(groupId, remoteExpenses)
                        confirmPendingSyncExpenses(groupId)
                        retryPendingReceiptUploads(groupId)
                    } catch (e: Exception) {
                        Timber.w(e, "Error reconciling expenses from cloud snapshot")
                    }
                }
        } catch (e: Exception) {
            Timber.w(e, "Error subscribing to cloud expense changes, using local cache")
        }
    }

    /**
     * Attempts to confirm PENDING_SYNC expenses by verifying their existence on the server.
     *
     * Called after each reconciliation cycle. When the device is online and Firestore
     * has confirmed the pending write, the server verification succeeds and the
     * expense transitions to SYNCED. When offline, the verification throws and the
     * expense remains PENDING_SYNC.
     *
     * This mechanism handles the case where the app is killed before the syncScope
     * coroutine in addExpense() can call updateSyncStatus(SYNCED). On app restart,
     * the snapshot listener fires, reconciliation restores PENDING_SYNC (Step 3),
     * and this method then verifies and transitions confirmed items to SYNCED.
     */
    private suspend fun confirmPendingSyncExpenses(groupId: String) {
        val pendingIds = localExpenseDataSource.getPendingSyncExpenseIds(groupId)
        if (pendingIds.isEmpty()) return

        for (id in pendingIds) {
            try {
                if (cloudExpenseDataSource.verifyExpenseOnServer(groupId, id)) {
                    localExpenseDataSource.updateSyncStatus(id, SyncStatus.SYNCED)
                    Timber.d("Confirmed expense sync: $id")
                }
            } catch (e: Exception) {
                // Server unreachable — keep as PENDING_SYNC
                Timber.d(e, "Cannot confirm expense $id — server unreachable")
            }
        }
    }

    /**
     * Scans for expenses in the group that have a local receipt but no remote URL,
     * and schedules background uploads for them. This acts as a retry mechanism
     * if the initial upload was interrupted or failed.
     */
    private suspend fun retryPendingReceiptUploads(groupId: String) {
        val expenseIds = localExpenseDataSource.getExpenseIdsByGroup(groupId)
        for (id in expenseIds) {
            val expense = localExpenseDataSource.getExpenseById(id) ?: continue
            scheduleReceiptUpload(expense)
        }
    }

    private fun scheduleReceiptUpload(expense: Expense) {
        val attachment = expense.receiptAttachment ?: return
        if (attachment.localUri.isBlank() || !attachment.remoteUrl.isNullOrBlank()) return

        val existingJob = receiptUploadJobs[expense.id]
        if (existingJob != null && existingJob.isActive) return

        receiptUploadJobs[expense.id] = syncScope.launch {
            try {
                uploadReceiptInBackground(expense)
            } finally {
                receiptUploadJobs.remove(expense.id)
            }
        }
    }

    override suspend fun updateReceiptRemoteUrl(expenseId: String, remoteUrl: String) {
        localExpenseDataSource.updateReceiptRemoteUrl(expenseId, remoteUrl)
    }

    override suspend fun updateReceiptLocalUri(expenseId: String, localUri: String) {
        localExpenseDataSource.updateReceiptLocalUri(expenseId, localUri)
    }

    /**
     * Uploads the receipt to Firebase Cloud Storage if the expense has an attachment with a
     * local path but no remote URL yet.  Called inside the syncScope after the Firestore write
     * succeeds so that connectivity issues during upload do not block the expense save.
     *
     * On success:
     * 1. Persists the download URL to Room.
     * 2. Reloads the updated expense from Room and **re-syncs it to Firestore** so other
     *    devices immediately see the attachment. Without step 2 the expense would already
     *    be SYNCED and no subsequent cloud write would carry the remote URL.
     */
    private suspend fun uploadReceiptInBackground(expense: Expense) {
        val attachment = expense.receiptAttachment ?: return
        if (!attachment.remoteUrl.isNullOrBlank()) return // already uploaded

        try {
            val remoteUrl = cloudStorageDataSource.uploadReceipt(
                expenseId = expense.id,
                localPath = attachment.localUri,
                mimeType = attachment.mimeType
            )
            localExpenseDataSource.updateReceiptRemoteUrl(expense.id, remoteUrl)
            Timber.d("Receipt uploaded for expense ${expense.id}: $remoteUrl")

            // Re-sync to Firestore so the attachment appears for other users/devices.
            val updatedExpense = localExpenseDataSource.getExpenseById(expense.id)
            if (updatedExpense != null) {
                cloudExpenseDataSource.addExpense(expense.groupId, updatedExpense)
                Timber.d("Receipt URL synced to Firestore for expense ${expense.id}")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Non-fatal: the expense is already saved. The next addExpense retry (e.g. after the
            // device comes back online) will re-attempt the upload via uploadReceiptInBackground.
            Timber.w(e, "Failed to upload receipt for expense ${expense.id}")
        }
    }

    /**
     * Builds an expense with repository-side metadata applied:
     * a stable ID, the groupId, [SyncStatus.PENDING_SYNC], and local timestamps.
     */
    private fun buildExpenseWithMetadata(groupId: String, expense: Expense): Expense {
        val expenseId = expense.id.ifBlank { UUID.randomUUID().toString() }
        val currentUserId = authenticationService.currentUserId() ?: ""
        val currentTimestamp = LocalDateTime.now()
        return expense.copy(
            id = expenseId,
            groupId = groupId,
            createdBy = expense.createdBy.ifBlank { currentUserId },
            createdAt = expense.createdAt ?: currentTimestamp,
            lastUpdatedAt = currentTimestamp,
            syncStatus = SyncStatus.PENDING_SYNC
        )
    }
}

private suspend fun deleteLocalReceiptFileBestEffort(
    receiptStorageService: ReceiptStorageService,
    localUri: String
) {
    try {
        receiptStorageService.deleteLocalFile(localUri)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.w(e, "Failed to delete local receipt file: $localUri")
    }
}

private fun launchCloudExpenseDeletion(
    syncScope: CoroutineScope,
    cloudExpenseDataSource: CloudExpenseDataSource,
    cloudStorageDataSource: CloudStorageDataSource,
    groupId: String,
    expenseId: String,
    remoteUrl: String?
) {
    syncScope.launch {
        try {
            cloudExpenseDataSource.deleteExpense(groupId, expenseId)
            Timber.d("Expense deletion synced to cloud: $expenseId")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Failed to sync expense deletion to cloud, will retry later")
        }

        if (remoteUrl != null) {
            try {
                cloudStorageDataSource.deleteReceipt(expenseId)
                Timber.d("Cloud receipt deletion synced to cloud: $expenseId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to delete remote receipt for expense $expenseId")
            }
        }
    }
}
