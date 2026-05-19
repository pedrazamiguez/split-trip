package es.pedrazamiguez.splittrip.domain.datasource.local

import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface LocalExpenseDataSource {

    fun getExpensesByGroupIdFlow(groupId: String): Flow<List<Expense>>

    suspend fun getExpenseById(expenseId: String): Expense?

    suspend fun saveExpenses(expenses: List<Expense>)

    suspend fun saveExpense(expense: Expense)

    suspend fun deleteExpense(expenseId: String)

    suspend fun deleteExpensesByGroupId(groupId: String)

    /**
     * Atomically replaces all expenses for a group with the provided list.
     * Used during real-time sync to reconcile local state with the cloud snapshot.
     * This handles both additions and deletions made by other users/devices.
     */
    suspend fun replaceExpensesForGroup(groupId: String, expenses: List<Expense>)

    suspend fun getExpenseIdsByGroup(groupId: String): List<String>

    /**
     * Updates the sync status of a single expense.
     * Used by repositories to track cloud sync progress (PENDING_SYNC → SYNCED / SYNC_FAILED).
     */
    suspend fun updateSyncStatus(expenseId: String, syncStatus: SyncStatus)

    /**
     * Returns IDs of expenses in a group that are waiting for server confirmation.
     * Used by the repository after reconciliation to attempt server verification
     * and transition PENDING_SYNC items to SYNCED.
     */
    suspend fun getPendingSyncExpenseIds(groupId: String): List<String>

    /**
     * Persists the Firebase Storage download URL for an expense's receipt.
     * Called by the repository after a successful background upload so that the URL
     * survives app restarts and is included in subsequent Firestore sync writes.
     */
    suspend fun updateReceiptRemoteUrl(expenseId: String, remoteUrl: String)

    suspend fun clearAllExpenses()
}
