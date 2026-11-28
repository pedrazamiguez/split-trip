package es.pedrazamiguez.splittrip.domain.datasource.cloud

import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow

interface CloudExpenseDataSource {
    suspend fun addExpense(groupId: String, expense: Expense)
    suspend fun deleteExpense(groupId: String, expenseId: String)

    /**
     * One-shot paginated full-collection fetch used for initial bootstrap or expired-checkpoint fallback.
     */
    suspend fun fetchExpensesFullSnapshot(groupId: String): DeltaSyncBatch<Expense>

    /**
     * One-shot incremental delta fetch for changes after [sinceWatermark].
     */
    suspend fun fetchExpensesDelta(groupId: String, sinceWatermark: SyncWatermark): DeltaSyncBatch<Expense>

    /**
     * Reactive delta stream for real-time observers starting after [sinceWatermark] (or bounded initial listener when null).
     */
    fun observeExpensesDeltaFlow(groupId: String, sinceWatermark: SyncWatermark?): Flow<DeltaSyncBatch<Expense>>

    /**
     * Verifies that an expense document exists on the Firestore server (not just local cache).
     * Forces a server round-trip — throws if the device is offline.
     *
     * Used by repositories to confirm that a locally-created expense has been
     * successfully persisted to the server, enabling the PENDING_SYNC → SYNCED transition.
     *
     * @param groupId The ID of the group containing the expense.
     * @param expenseId The ID of the expense to verify.
     * @return true if the expense exists on the server.
     * @throws Exception if the server is unreachable (e.g., airplane mode).
     */
    suspend fun verifyExpenseOnServer(groupId: String, expenseId: String): Boolean

    /**
     * Saves a cash-funded expense using an optimistic-locking Firestore transaction.
     *
     * The transaction atomically:
     * 1. Reads the current `remainingAmount` for each consumed withdrawal.
     * 2. Verifies each value against [expectedRemainingAmounts] (the amounts observed by the
     *    client before FIFO consumption).
     * 3. Writes the expense document.
     * 4. Updates each consumed withdrawal's `remainingAmount` to `expected − consumed`.
     *
     * If any withdrawal's server-side `remainingAmount` differs from the expected value,
     * the transaction is aborted and [es.pedrazamiguez.splittrip.domain.exception.CashConflictException]
     * is thrown, indicating a concurrent write by another group member.
     *
     * @param groupId The group the expense belongs to.
     * @param expense The expense to persist. Must have [Expense.cashTranches] populated so
     *   the transaction knows how much to deduct from each withdrawal.
     * @param expectedRemainingAmounts Map of withdrawal ID → `remainingAmount` observed by
     *   the client before FIFO consumption began.
     * @throws es.pedrazamiguez.splittrip.domain.exception.CashConflictException if a concurrent
     *   write has modified any consumed withdrawal.
     * @throws Exception if the server is unreachable (offline, network error, etc.).
     */
    suspend fun addExpenseWithCashPreconditions(
        groupId: String,
        expense: Expense,
        expectedRemainingAmounts: Map<String, Long>
    )
}
