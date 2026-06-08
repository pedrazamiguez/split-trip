package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.attributeContributions
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.attributeExpensesByPaymentMethod
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.attributeRemainingByScope
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.attributeWithdrawals
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.balanceDistributeByShares
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.balanceDistributeEvenly
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.buildCashInHandByCurrency
import es.pedrazamiguez.splittrip.domain.usecase.balance.support.buildCurrencyAmountList
import java.math.BigDecimal

/**
 * Use case that derives per-member financial balances from
 * pre-loaded domain data. Does NOT subscribe to repository flows — the caller
 * (typically ViewModel) is responsible for collecting the data streams and
 * passing them in, avoiding duplicate Firestore snapshot listeners.
 *
 * Attribution rules:
 * - **Contributions:** Individual → full amount to userId.
 *   Subunit → distributed among subunit members by [Subunit.memberShares].
 * - **Withdrawals:** GROUP → equal split among all group members.
 *   SUBUNIT → distributed by memberShares. USER → full amount to withdrawnBy.
 * - **Expenses:** Already per-user via [es.pedrazamiguez.splittrip.domain.model.ExpenseSplit].
 *   Split into CASH vs non-CASH by [Expense.paymentMethod].
 *   **Add-ons (fees, tips, surcharges) increase the effective group amount.**
 *
 * Financial model per member:
 * - pocketBalance = contributed − withdrawn − nonCashSpent
 * - cashInHand = Σ (withdrawal.remainingAmount × deductedBaseAmount / amountWithdrawn) per member
 *   (scope-aware: GROUP → equal split; USER → 100% to withdrawnBy; SUBUNIT → by memberShares)
 * - totalSpent = cashSpent + nonCashSpent
 *
 * All helper logic lives in top-level internal functions:
 * [attributeContributions], [attributeWithdrawals], [attributeRemainingByScope],
 * [attributeExpensesByPaymentMethod], [buildCashInHandByCurrency], [buildCurrencyAmountList].
 * Distribution helpers: [balanceDistributeByShares], [balanceDistributeEvenly].
 */
class GetMemberBalancesFlowUseCaseImpl(
    private val addOnCalculationService: AddOnCalculationService
) : GetMemberBalancesFlowUseCase {

    override fun computeMemberBalances(
        contributions: List<Contribution>,
        withdrawals: List<CashWithdrawal>,
        expenses: List<Expense>,
        subunits: List<Subunit>,
        groupMemberIds: List<String>,
        groupCurrency: String
    ): List<MemberBalance> {
        val subunitMap = subunits.associateBy { it.id }

        val contributedMap = attributeContributions(contributions, subunitMap, groupMemberIds)
        val withdrawalResult = attributeWithdrawals(withdrawals, subunitMap, groupMemberIds, addOnCalculationService)
        val remainingResult = attributeRemainingByScope(withdrawals, subunitMap, groupMemberIds)
        val expenseResult = attributeExpensesByPaymentMethod(expenses, addOnCalculationService)

        val allUserIds = buildSet {
            addAll(groupMemberIds)
            addAll(contributedMap.keys)
            addAll(withdrawalResult.groupCurrencyMap.keys)
            addAll(expenseResult.cashSpentMap.keys)
            addAll(expenseResult.nonCashSpentMap.keys)
        }

        return allUserIds.map { userId ->
            val contributed = contributedMap[userId] ?: 0L
            val withdrawn = withdrawalResult.groupCurrencyMap[userId] ?: 0L
            val cashSpent = expenseResult.cashSpentMap[userId] ?: 0L
            val nonCashSpent = expenseResult.nonCashSpentMap[userId] ?: 0L

            MemberBalance(
                userId = userId,
                contributed = contributed,
                withdrawn = withdrawn,
                cashSpent = cashSpent,
                nonCashSpent = nonCashSpent,
                totalSpent = cashSpent + nonCashSpent,
                pocketBalance = contributed - withdrawn - nonCashSpent,
                cashInHand = remainingResult.groupCurrencyMap[userId] ?: 0L,
                cashInHandByCurrency = buildCashInHandByCurrency(
                    remainingByCurrency = remainingResult.byCurrency[userId] ?: emptyMap(),
                    groupCurrency = groupCurrency
                ),
                cashSpentByCurrency = buildCurrencyAmountList(
                    byCurrencyMap = expenseResult.cashSpentByCurrency[userId] ?: emptyMap(),
                    equivByCurrency = expenseResult.cashEquivByCurrency[userId] ?: emptyMap(),
                    groupCurrency = groupCurrency
                ),
                nonCashSpentByCurrency = buildCurrencyAmountList(
                    byCurrencyMap = expenseResult.nonCashSpentByCurrency[userId] ?: emptyMap(),
                    equivByCurrency = expenseResult.nonCashEquivByCurrency[userId] ?: emptyMap(),
                    groupCurrency = groupCurrency
                )
            )
        }
    }

    companion object {
        /** Delegates to [balanceDistributeByShares] — accessible via `GetMemberBalancesFlowUseCase.distributeByShares(...)`. */
        internal fun distributeByShares(
            totalAmount: Long,
            memberShares: Map<String, BigDecimal>
        ): Map<String, Long> = balanceDistributeByShares(totalAmount, memberShares)

        /** Delegates to [balanceDistributeEvenly] — accessible via `GetMemberBalancesFlowUseCase.distributeEvenly(...)`. */
        internal fun distributeEvenly(
            totalAmount: Long,
            memberIds: List<String>
        ): Map<String, Long> = balanceDistributeEvenly(totalAmount, memberIds)
    }
}
