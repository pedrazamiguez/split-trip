package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.WithdrawalPoolOption
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository

/**
 * Determines which withdrawal pools have available funds for a given cash expense configuration.
 *
 * For GROUP-scoped expenses, probes the GROUP pool, the current user's personal (USER-scoped)
 * pool when a [payerId] (userId) is provided, and any SUBUNIT pools for [subunitIds] the user
 * belongs to. This allows the pool-selection widget to surface personal and subunit cash as
 * supplements when the GROUP pool alone is insufficient.
 *
 * For USER and SUBUNIT scopes, probes the personal/subunit pool AND the GROUP pool independently
 * using [CashWithdrawalRepository.getAvailableWithdrawalsByExactScope] (no fallback).
 *
 * The UI shows a pool-selection widget only when this use case returns more than one option.
 * When only one pool has funds, the caller should auto-select it silently so the submit path
 * is always uniform (preferred pool is always set before calling [AddExpenseUseCase]).
 *
 * @param cashWithdrawalRepository Repository for querying scoped withdrawal availability.
 */
class GetAvailableWithdrawalPoolsUseCase(
    private val cashWithdrawalRepository: CashWithdrawalRepository
) {
    /**
     * Returns the list of pools that have at least one withdrawal with `remainingAmount > 0`
     * for the given [groupId] / [currency] combination.
     *
     * @param groupId       The group the expense belongs to.
     * @param currency      The source currency of the expense (ISO 4217, e.g. "THB").
     * @param payerType     The expense's payer scope (GROUP / USER / SUBUNIT).
     * @param payerId       For USER scope: the userId. For SUBUNIT scope: the subunitId.
     *                      For GROUP scope: the current userId, used to probe the user's
     *                      personal (USER-scoped) pool as a supplement to the GROUP pool.
     * @param subunitIds    For GROUP scope: IDs of subunits the current user belongs to.
     *                      Each subunit's pool is probed independently and surfaced as a
     *                      selectable option when funds are available. Ignored for USER/SUBUNIT scope.
     * @return A list of [WithdrawalPoolOption] values with available cash, in priority order.
     *         For GROUP: GROUP pool first (primary), USER pool second (supplement),
     *         then SUBUNIT pools in the order provided by [subunitIds].
     *         For USER/SUBUNIT: personal/subunit pool first, GROUP pool second.
     *         Empty when no pool has funds.
     */
    suspend operator fun invoke(
        groupId: String,
        currency: String,
        payerType: PayerType,
        payerId: String? = null,
        subunitIds: List<String> = emptyList()
    ): List<WithdrawalPoolOption> = if (payerType == PayerType.GROUP) {
        buildGroupPools(groupId, currency, payerId, subunitIds)
    } else {
        buildPersonalPools(groupId, currency, payerType, payerId)
    }

    /**
     * Probes GROUP, USER, and SUBUNIT pools for a GROUP-scoped expense.
     * Returns available pools in priority order: GROUP first, then USER supplement,
     * then each SUBUNIT that has funded cash.
     */
    private suspend fun buildGroupPools(
        groupId: String,
        currency: String,
        payerId: String?,
        subunitIds: List<String>
    ): List<WithdrawalPoolOption> {
        val groupPool = cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
            groupId = groupId,
            currency = currency,
            scope = PayerType.GROUP
        )
        val userPool = if (!payerId.isNullOrBlank()) {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = currency,
                scope = PayerType.USER,
                scopeOwnerId = payerId
            )
        } else {
            emptyList()
        }
        val subunitPools = subunitIds.mapNotNull { subunitId ->
            val pool = cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = currency,
                scope = PayerType.SUBUNIT,
                scopeOwnerId = subunitId
            )
            if (pool.isNotEmpty()) WithdrawalPoolOption(PayerType.SUBUNIT, subunitId) else null
        }
        return buildList {
            if (groupPool.isNotEmpty()) add(WithdrawalPoolOption(PayerType.GROUP))
            if (userPool.isNotEmpty()) add(WithdrawalPoolOption(PayerType.USER, payerId))
            addAll(subunitPools)
        }
    }

    /**
     * Probes personal/subunit and GROUP pools independently for USER/SUBUNIT-scoped expenses.
     * Returns personal pool first (primary), then GROUP pool (fallback supplement).
     */
    private suspend fun buildPersonalPools(
        groupId: String,
        currency: String,
        payerType: PayerType,
        payerId: String?
    ): List<WithdrawalPoolOption> {
        val personalPool = if (!payerId.isNullOrBlank()) {
            cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
                groupId = groupId,
                currency = currency,
                scope = payerType,
                scopeOwnerId = payerId
            )
        } else {
            emptyList()
        }
        val groupPool = cashWithdrawalRepository.getAvailableWithdrawalsByExactScope(
            groupId = groupId,
            currency = currency,
            scope = PayerType.GROUP
        )

        return buildList {
            if (personalPool.isNotEmpty()) add(WithdrawalPoolOption(payerType, payerId))
            if (groupPool.isNotEmpty()) add(WithdrawalPoolOption(PayerType.GROUP))
        }
    }
}
