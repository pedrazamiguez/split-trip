package es.pedrazamiguez.splittrip.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.data.local.entity.SyncStatusEntry
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
@Dao
interface ExpenseDao {

    @Query(
        "SELECT * FROM expenses WHERE groupId = :groupId ORDER BY COALESCE(operationDateMillis, createdAtMillis) DESC"
    )
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

    @Query("UPDATE expenses SET receiptLocalUri = :localUri WHERE id = :expenseId")
    suspend fun updateReceiptLocalUri(expenseId: String, localUri: String)

    @Query(
        """
        SELECT * FROM expenses
        WHERE groupId = :groupId
          AND receiptLocalUri IS NOT NULL AND receiptLocalUri != ''
          AND (receiptRemoteUrl IS NULL OR receiptRemoteUrl = '')
        """
    )
    suspend fun getExpensesWithPendingReceipts(groupId: String): List<ExpenseEntity>

    /**
     * Replaces a group's expenses with incoming remote state, using a diff-based
     * reconciliation strategy:
     * - Preserves local-only receipt metadata and non-SYNCED statuses.
     * - Only upserts entities that are new or whose properties have actually changed.
     * - Only deletes entities that were removed from the remote set and were SYNCED.
     *
     * When reconciling an unchanged snapshot, zero database writes occur, preventing
     * unnecessary Room flow invalidations.
     */
    @Transaction
    suspend fun replaceExpensesForGroup(groupId: String, expenses: List<ExpenseEntity>) {
        val existingExpenses = getExpensesByGroupId(groupId).associateBy { it.id }
        val mergedExpenses = expenses.map { remote ->
            val local = existingExpenses[remote.id]
            if (local != null) {
                remote.copy(
                    receiptLocalUri = local.receiptLocalUri?.takeIf { it.isNotBlank() } ?: remote.receiptLocalUri,
                    receiptRemoteUrl = remote.receiptRemoteUrl?.takeIf { it.isNotBlank() } ?: local.receiptRemoteUrl,
                    receiptMimeType = remote.receiptMimeType?.takeIf { it.isNotBlank() } ?: local.receiptMimeType,
                    receiptCapturedAtMillis = if (remote.receiptCapturedAtMillis != 0L) {
                        remote.receiptCapturedAtMillis
                    } else {
                        local.receiptCapturedAtMillis
                    },
                    syncStatus = local.syncStatus.takeIf { it != "SYNCED" } ?: remote.syncStatus
                )
            } else {
                remote
            }
        }

        val remoteIds = mergedExpenses.map { it.id }.toSet()
        val localIds = existingExpenses.keys

        val toUpsert = mergedExpenses.filter { merged ->
            val existing = existingExpenses[merged.id]
            existing == null || existing != merged
        }
        if (toUpsert.isNotEmpty()) {
            insertExpenses(toUpsert)
        }

        val staleIds = localIds.filter { id ->
            id !in remoteIds && existingExpenses[id]?.syncStatus == "SYNCED"
        }
        if (staleIds.isNotEmpty()) {
            deleteExpensesByIds(staleIds)
        }
    }
}
