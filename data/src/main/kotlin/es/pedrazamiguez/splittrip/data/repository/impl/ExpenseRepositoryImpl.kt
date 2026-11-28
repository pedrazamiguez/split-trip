package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.core.performance.PerformanceTraces
import es.pedrazamiguez.splittrip.data.sync.DeltaSyncParams
import es.pedrazamiguez.splittrip.data.sync.KeyedSubscriptionTracker
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.data.sync.syncGroupCollectionDelta
import es.pedrazamiguez.splittrip.data.sync.trackSubscription
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSyncCheckpointDataSource
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.MembershipRemovalEvent
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("TooManyFunctions", "LongParameterList")
class ExpenseRepositoryImpl(
    private val cloudExpenseDataSource: CloudExpenseDataSource,
    private val localExpenseDataSource: LocalExpenseDataSource,
    private val authenticationService: AuthenticationService,
    private val cloudStorageDataSource: CloudStorageDataSource,
    private val receiptStorageService: ReceiptStorageService,
    private val performanceMonitor: PerformanceMonitor,
    private val localGroupDataSource: LocalGroupDataSource,
    private val localSyncCheckpointDataSource: LocalSyncCheckpointDataSource,
    private val remainderDistributionService: RemainderDistributionService,
    private val syncTeardownCoordinator: SyncTeardownCoordinator,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRepository {

    private val syncScope = CoroutineScope(ioDispatcher)

    /**
     * Tracks active cloud subscription Jobs per groupId.
     * Prevents duplicate Firestore snapshot listeners from accumulating
     * when onStart fires multiple times (e.g., config changes, tab switches,
     * WhileSubscribed resubscriptions, flatMapLatest restarts).
     */
    private val cloudSubscriptionTracker = KeyedSubscriptionTracker()
    private val syncedArchivedGroupIds: MutableSet<String> = ConcurrentHashMap.newKeySet()

    init {
        syncTeardownCoordinator.registerAction {
            cloudSubscriptionTracker.cancelAll()
            syncedArchivedGroupIds.clear()
        }
    }

    /**
     * Tracks active receipt upload Jobs per expenseId.
     * Prevents duplicate/concurrent uploads of the same receipt.
     */
    private val receiptUploadJobs = ConcurrentHashMap<String, Job>()

    override suspend fun addExpense(groupId: String, expense: Expense) {
        performanceMonitor.traceAsync(PerformanceTraces.EXPENSE_ADD) {
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
        return performanceMonitor.traceAsync(PerformanceTraces.EXPENSE_ADD_CASH) {
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
                true
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
                false
            }
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
            .trackSubscription(cloudSubscriptionTracker, groupId, syncScope) {
                val syncParams = DeltaSyncParams(
                    groupId = groupId,
                    collectionName = SyncCollectionName.EXPENSES,
                    checkpointDataSource = localSyncCheckpointDataSource,
                    isGroupArchived = {
                        localGroupDataSource.getGroupById(groupId)?.status == GroupStatus.ARCHIVED
                    },
                    syncedArchivedGroupIds = syncedArchivedGroupIds,
                    fetchFullSnapshot = { cloudExpenseDataSource.fetchExpensesFullSnapshot(groupId) },
                    fetchDeltaOnce = { watermark ->
                        cloudExpenseDataSource.fetchExpensesDelta(groupId, watermark)
                    },
                    observeDeltaFlow = { watermark ->
                        cloudExpenseDataSource.observeExpensesDeltaFlow(groupId, watermark)
                    },
                    replaceLocal = { remoteExpenses ->
                        localExpenseDataSource.replaceExpensesForGroup(groupId, remoteExpenses)
                    },
                    applyDeltaLocal = { upserts, deletedIds ->
                        localExpenseDataSource.applyExpensesDeltaForGroup(groupId, upserts, deletedIds)
                    },
                    getPendingIds = { localExpenseDataSource.getPendingSyncExpenseIds(groupId) },
                    verifyOnServer = { id -> cloudExpenseDataSource.verifyExpenseOnServer(groupId, id) },
                    markSynced = { id -> localExpenseDataSource.updateSyncStatus(id, SyncStatus.SYNCED) },
                    entityLabel = "expense",
                    logContext = "for group $groupId",
                    performanceMonitor = performanceMonitor,
                    verifyParentExists = { localGroupDataSource.getGroupById(groupId) != null },
                    onAfterReconcile = { retryPendingReceiptUploads(groupId) }
                )

                if (localGroupDataSource.getGroupById(groupId)?.status == GroupStatus.ARCHIVED) {
                    syncGroupCollectionDelta(syncParams)
                } else {
                    coroutineScope {
                        launch {
                            observeAndProcessMembershipRemovals(groupId)
                        }
                        syncGroupCollectionDelta(syncParams)
                    }
                }
            }

    /**
     * Scans for expenses in the group that have a local receipt but no remote URL,
     * and schedules background uploads for them. This acts as a retry mechanism
     * if the initial upload was interrupted or failed.
     */
    private suspend fun retryPendingReceiptUploads(groupId: String) {
        val pendingExpenses = localExpenseDataSource.getPendingReceiptExpenses(groupId)
        for (expense in pendingExpenses) {
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

    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod")
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

    private suspend fun observeAndProcessMembershipRemovals(groupId: String) {
        localGroupDataSource.getUnprocessedMembershipRemovalEventsFlow(groupId)
            .collect { events ->
                for (event in events) {
                    processMembershipRemoval(groupId, event)
                }
            }
    }

    private suspend fun processMembershipRemoval(
        groupId: String,
        event: MembershipRemovalEvent
    ) {
        val expenses = localExpenseDataSource.getExpensesByGroupIdFlow(groupId).first()

        for (expense in expenses) {
            val splits = expense.splits
            val departedSplit = splits.find { it.userId == event.userId }

            if (departedSplit != null && departedSplit.amountCents > 0) {
                performReSplitForExpense(groupId, expense, departedSplit, event.userId)
            }
        }

        localGroupDataSource.markMembershipRemovalEventProcessed(event.id)
        Timber.d("Membership removal event processed locally: ${event.id}")
    }

    private suspend fun performReSplitForExpense(
        groupId: String,
        expense: Expense,
        departedSplit: ExpenseSplit,
        leftUserId: String
    ) {
        val debtToRedistribute = departedSplit.amountCents
        val remainingSplits = expense.splits.filter { it.userId != leftUserId }
        if (remainingSplits.isEmpty()) return

        val remainingTotalCents = remainingSplits.sumOf { it.amountCents }
        val newTotalCents = remainingTotalCents + debtToRedistribute

        val amounts = remainingSplits.map { it.amountCents }
        val isExcluded = remainingSplits.map { it.isExcluded }

        val newAmounts = remainderDistributionService.rescaleAmounts(
            originalTotal = remainingTotalCents,
            newTotal = newTotalCents,
            amounts = amounts,
            isExcluded = isExcluded
        )

        val newPercentages = calculateNewPercentages(expense, newAmounts, newTotalCents)
        val updatedSplits = buildUpdatedSplits(expense, departedSplit, remainingSplits, newAmounts, newPercentages)

        val updatedExpense = expense.copy(
            splits = updatedSplits,
            lastUpdatedAt = LocalDateTime.now(),
            syncStatus = SyncStatus.PENDING_SYNC
        )

        localExpenseDataSource.saveExpense(updatedExpense)
        syncReSplitExpenseToCloud(groupId, updatedExpense)
    }

    private fun calculateNewPercentages(
        expense: Expense,
        newAmounts: List<Long>,
        newTotalCents: Long
    ): List<BigDecimal>? {
        return if (expense.splitType == SplitType.PERCENT) {
            remainderDistributionService.distributePercentages(
                remainingPercentage = BigDecimal("100.00"),
                amounts = newAmounts,
                totalCents = newTotalCents
            )
        } else {
            null
        }
    }

    private fun buildUpdatedSplits(
        expense: Expense,
        departedSplit: ExpenseSplit,
        remainingSplits: List<ExpenseSplit>,
        newAmounts: List<Long>,
        newPercentages: List<BigDecimal>?
    ): List<ExpenseSplit> {
        val updatedSplits = mutableListOf<ExpenseSplit>()

        updatedSplits.add(
            departedSplit.copy(
                amountCents = 0L,
                percentage = if (expense.splitType == SplitType.PERCENT) {
                    BigDecimal.ZERO
                } else {
                    null
                },
                isExcluded = true
            )
        )

        remainingSplits.forEachIndexed { index, split ->
            val newAmount = newAmounts[index]
            val newPercentage = newPercentages?.get(index)
            updatedSplits.add(
                split.copy(
                    amountCents = newAmount,
                    percentage = newPercentage
                )
            )
        }

        return updatedSplits
    }

    private fun syncReSplitExpenseToCloud(groupId: String, updatedExpense: Expense) {
        syncScope.launch {
            try {
                cloudExpenseDataSource.addExpense(groupId, updatedExpense)
                localExpenseDataSource.updateSyncStatus(updatedExpense.id, SyncStatus.SYNCED)
                Timber.d("Re-split expense synced to cloud: ${updatedExpense.id}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val currentStatus = localExpenseDataSource.getExpenseById(updatedExpense.id)?.syncStatus
                if (currentStatus == SyncStatus.PENDING_SYNC) {
                    localExpenseDataSource.updateSyncStatus(updatedExpense.id, SyncStatus.SYNC_FAILED)
                }
                Timber.e(e, "Failed to sync re-split expense ${updatedExpense.id}")
            }
        }
    }
}
