package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY createdAtMillis DESC")
    fun getExpensesByGroupIdFlow(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE id = :expenseId")
    fun getExpenseByIdFlow(expenseId: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getExpensesByGroupId(groupId: String): List<ExpenseEntity>

    @Upsert
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Upsert
    suspend fun insertExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :expenseId")
    suspend fun deleteExpense(expenseId: String)

    /**
     * Updates the sync status of a single expense.
     * Used to transition between PENDING_SYNC → SYNCED or SYNC_FAILED after cloud sync.
     */
    @Query("UPDATE expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("DELETE FROM expenses WHERE groupId = :groupId")
    suspend fun deleteExpensesByGroupId(groupId: String)

    @Query("SELECT id FROM expenses WHERE groupId = :groupId")
    suspend fun getExpenseIdsByGroupId(groupId: String): List<String>

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()

    /**
     * Returns sync status metadata for expenses in a group that are NOT fully synced.
     * Used during cloud snapshot reconciliation to preserve PENDING_SYNC / SYNC_FAILED
     * statuses that would otherwise be overwritten by the upsert (which defaults to SYNCED).
     */
    @Query("SELECT id, syncStatus FROM expenses WHERE groupId = :groupId AND syncStatus != 'SYNCED'")
    suspend fun getUnsyncedExpenseStatuses(groupId: String): List<SyncStatusEntry>

    /**
     * Returns IDs of expenses in a group that are still waiting for server confirmation.
     * Used after reconciliation to attempt server verification and transition to SYNCED.
     */
    @Query("SELECT id FROM expenses WHERE groupId = :groupId AND syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncExpenseIds(groupId: String): List<String>

    /**
     * Deletes expenses whose IDs are in the provided list.
     * Used to selectively remove stale expenses during sync reconciliation.
     */
    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun deleteExpensesByIds(ids: List<String>)

    @Query("UPDATE expenses SET receiptRemoteUrl = :remoteUrl WHERE id = :expenseId")
    suspend fun updateReceiptRemoteUrl(expenseId: String, remoteUrl: String)

    /**
     * Reconciles local expenses for a group with the authoritative cloud snapshot.
     *
     * Uses a merge strategy instead of destructive delete+insert:
     * 1. Capture non-SYNCED statuses (PENDING_SYNC / SYNC_FAILED) before upsert
     * 2. Upsert all remote expenses (adds new, updates existing — sets syncStatus to SYNCED)
     * 3. Restore ALL non-SYNCED statuses captured in step 1
     * 4. Delete only stale synced expenses (not in remote set AND fully synced)
     *
     * This preserves locally-created expenses that haven't synced to the cloud yet.
     *
     * **Why we always restore non-SYNCED statuses (even for items in the remote set):**
     * Firestore's `MetadataChanges.INCLUDE` fires snapshots that include pending
     * local writes (latency compensation). These items appear in the remote set
     * but have NOT been confirmed by the server. If we skip restoration for items
     * in the remote set, the upsert's default SYNCED status would overwrite
     * PENDING_SYNC — hiding the sync indicator. The PENDING_SYNC → SYNCED
     * transition is handled exclusively by the repository's explicit
     * `updateSyncStatus()` call after server confirmation (via
     * `confirmPendingSyncExpenses()` or the sync in `addExpense()`).
     */
    @Transaction
    suspend fun replaceExpensesForGroup(groupId: String, expenses: List<ExpenseEntity>) {
        // Capture existing local expenses to preserve their local receipt URIs.
        // Firestore documents do not contain on-device file paths, so an upsert of remote state
        // would otherwise overwrite and wipe out localUri references.
        val existingExpenses = getExpensesByGroupId(groupId).associateBy { it.id }
        val mergedExpenses = expenses.map { remote ->
            val local = existingExpenses[remote.id]
            if (local != null) {
                remote.copy(
                    // receiptLocalUri is only stored on-device, so always preserve the local path.
                    receiptLocalUri = local.receiptLocalUri?.takeIf { it.isNotBlank() } ?: remote.receiptLocalUri,
                    // For remote-backed columns, prefer the incoming remote updates, falling back
                    // to the local cache only if the remote value is blank/null/0.
                    receiptRemoteUrl = remote.receiptRemoteUrl?.takeIf { it.isNotBlank() } ?: local.receiptRemoteUrl,
                    receiptMimeType = remote.receiptMimeType?.takeIf { it.isNotBlank() } ?: local.receiptMimeType,
                    receiptCapturedAtMillis = if (remote.receiptCapturedAtMillis != 0L) {
                        remote.receiptCapturedAtMillis
                    } else {
                        local.receiptCapturedAtMillis
                    }
                )
            } else {
                remote
            }
        }

        // Step 1: Capture non-SYNCED statuses before the upsert overwrites them
        val unsyncedStatuses = getUnsyncedExpenseStatuses(groupId)
        val unsyncedIds = unsyncedStatuses.map { it.id }.toSet()

        val remoteIds = mergedExpenses.map { it.id }.toSet()
        val localIds = getExpenseIdsByGroupId(groupId)

        // Step 2: Upsert remote expenses (sets syncStatus to SYNCED for all)
        insertExpenses(mergedExpenses)

        // Step 3: Restore ALL non-SYNCED statuses that were captured before the upsert.
        // The upsert sets syncStatus to SYNCED for all items (including those that were
        // PENDING_SYNC or SYNC_FAILED). We must restore their original status because:
        // - Firestore snapshots with MetadataChanges.INCLUDE fire for pending writes
        //   (not yet confirmed by the server), so presence in remoteIds does NOT mean synced.
        // - The PENDING_SYNC → SYNCED transition is handled exclusively by the repository's
        //   explicit updateSyncStatus() call after confirmed cloud write.
        for (entry in unsyncedStatuses) {
            updateSyncStatus(entry.id, entry.syncStatus)
        }

        // Step 4: Remove stale expenses — only those that are NOT in the remote set
        // AND are NOT in a non-SYNCED state (protect unsynced local data)
        val staleIds = localIds.filter { it !in remoteIds && it !in unsyncedIds }
        if (staleIds.isNotEmpty()) {
            deleteExpensesByIds(staleIds)
        }
    }
}
