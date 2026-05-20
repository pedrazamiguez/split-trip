package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(groupId: String, expense: Expense)

    /**
     * Saves a cash-funded expense with optimistic-locking conflict detection.
     *
     * Room-first: the expense is saved to local storage immediately (PENDING_SYNC).
     * A synchronous Firestore transaction then verifies that no concurrent write
     * has consumed the same cash withdrawals in the meantime:
     *
     * - **Online + no conflict:** expense is marked SYNCED in Room after the transaction commits.
     *   Returns `true` — the caller may safely update withdrawal remaining amounts.
     * - **Online + conflict:** the Room write is rolled back and
     *   [es.pedrazamiguez.splittrip.domain.exception.CashConflictException] is re-thrown.
     * - **Offline / network error:** expense stays in Room as SYNC_FAILED (standard offline fallback,
     *   no exception propagated to the caller). Returns `false` — the caller must **not** update
     *   withdrawal remaining amounts, since the Firestore transaction did not run and doing so
     *   would deduct withdrawals in the cloud without a corresponding expense document.
     *
     * @param groupId The group the expense belongs to.
     * @param expense The cash-funded expense to persist. Must have [Expense.cashTranches] populated.
     * @param expectedRemainingAmounts Map of withdrawal ID → `remainingAmount` observed before FIFO
     *   consumption. Used as preconditions in the Firestore transaction.
     * @return `true` if the Firestore transaction committed successfully, `false` if the write fell
     *   back to offline-only Room storage (no cloud commit).
     * @throws es.pedrazamiguez.splittrip.domain.exception.CashConflictException when a concurrent
     *   write has modified any consumed withdrawal (online only).
     */
    suspend fun addCashExpense(
        groupId: String,
        expense: Expense,
        expectedRemainingAmounts: Map<String, Long>
    ): Boolean

    suspend fun getExpenseById(expenseId: String): Expense?

    fun getExpenseByIdFlow(expenseId: String): Flow<Expense?>

    fun getGroupExpensesFlow(groupId: String): Flow<List<Expense>>

    suspend fun deleteExpense(groupId: String, expenseId: String)

    /**
     * Writes the Firebase Storage download URL for an expense's receipt to local Room
     * so that subsequent Firestore sync writes include the remote URL in the document.
     *
     * Called by the repository internally after a successful [CloudStorageDataSource.uploadReceipt].
     * Exposed on the interface so future use cases can also trigger remote URL persistence
     * (e.g. after a cross-device download).
     */
    suspend fun updateReceiptRemoteUrl(expenseId: String, remoteUrl: String)
}
