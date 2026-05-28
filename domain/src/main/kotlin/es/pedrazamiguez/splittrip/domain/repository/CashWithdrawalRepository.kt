package es.pedrazamiguez.splittrip.domain.repository

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import kotlinx.coroutines.flow.Flow

interface CashWithdrawalRepository {

    suspend fun addWithdrawal(groupId: String, withdrawal: CashWithdrawal)

    fun getGroupWithdrawalsFlow(groupId: String): Flow<List<CashWithdrawal>>

    /**
     * Fetches available (non-exhausted) withdrawals for FIFO consumption, scoped to the
     * expense's payer type.
     *
     * FIFO pool priority:
     * - **GROUP:** returns only GROUP-scoped withdrawals.
     * - **USER:** returns USER-scoped withdrawals for [payerId] first, then GROUP-scoped
     *   withdrawals appended as a fallback. The combined list preserves oldest-first ordering
     *   within each pool so the FIFO algorithm naturally drains the personal pool before
     *   consuming from the group pool.
     * - **SUBUNIT:** returns SUBUNIT-scoped withdrawals for [payerId] first, then GROUP-scoped
     *   withdrawals appended as a fallback.
     *
     * @param groupId The group the expense belongs to.
     * @param currency The source currency of the expense (e.g. "THB").
     * @param payerType Scope of the expense payer (GROUP / USER / SUBUNIT).
     * @param payerId The userId for USER scope, or the subunitId for SUBUNIT scope.
     *   Ignored for GROUP scope.
     */
    suspend fun getAvailableWithdrawals(
        groupId: String,
        currency: String,
        payerType: PayerType,
        payerId: String? = null
    ): List<CashWithdrawal>

    /**
     * Updates the remaining amount on a withdrawal after FIFO consumption or refund.
     */
    suspend fun updateRemainingAmount(withdrawalId: String, newRemaining: Long)

    /**
     * Atomically updates the remaining amounts on multiple withdrawals in a single local DB
     * transaction, then syncs all changes to the cloud in one background job.
     * Preferred over calling [updateRemainingAmount] in a loop for multi-tranche cash expenses.
     *
     * @param groupId The group the withdrawals belong to (needed for cloud sync).
     * @param withdrawals The updated [CashWithdrawal] objects with their new [CashWithdrawal.remainingAmount] already applied.
     */
    suspend fun updateRemainingAmounts(groupId: String, withdrawals: List<CashWithdrawal>)

    /**
     * Fetches available (non-exhausted) withdrawals for a **single specific scope**,
     * with no GROUP fallback appended.
     *
     * Unlike [getAvailableWithdrawals], this method queries exactly the requested scope:
     * - **GROUP:** returns GROUP-scoped withdrawals only.
     * - **USER:** returns USER-scoped withdrawals for [scopeOwnerId] only (no GROUP append).
     * - **SUBUNIT:** returns SUBUNIT-scoped withdrawals for [scopeOwnerId] only (no GROUP append).
     *
     * Used by [es.pedrazamiguez.splittrip.domain.usecase.expense.GetAvailableWithdrawalPoolsUseCase]
     * to independently probe each pool's availability, and by the FIFO logic when a user has
     * explicitly chosen a pool via the pool-selection UI.
     *
     * @param groupId    The group the expense belongs to.
     * @param currency   The source currency of the expense (e.g. "THB").
     * @param scope      The exact scope to query (GROUP / USER / SUBUNIT).
     * @param scopeOwnerId The userId for USER scope, or the subunitId for SUBUNIT scope.
     *   Ignored (may be null) for GROUP scope.
     */
    suspend fun getAvailableWithdrawalsByExactScope(
        groupId: String,
        currency: String,
        scope: PayerType,
        scopeOwnerId: String? = null
    ): List<CashWithdrawal>

    /**
     * Refunds a previously consumed tranche back to its withdrawal.
     * Adds amountToRefund to the withdrawal's current remainingAmount.
     */
    suspend fun refundTranche(withdrawalId: String, amountToRefund: Long)

    suspend fun deleteWithdrawal(groupId: String, withdrawalId: String)

    /**
     * Fetches a single cash withdrawal by its ID.
     */
    suspend fun getWithdrawalById(withdrawalId: String): CashWithdrawal?
}
