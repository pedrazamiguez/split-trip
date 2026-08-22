package es.pedrazamiguez.splittrip.domain.usecase.balance.support

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Distributes withdrawal deducted amounts to individual members based on scope,
 * tracking both group-currency totals and per-currency native amounts.
 *
 * - GROUP → equal split among all group members.
 * - SUBUNIT → distribute by memberShares.
 * - USER → full amount to withdrawnBy.
 *
 * **Add-ons (ATM fees) increase the effective deducted amount** via
 * [AddOnCalculationService.calculateEffectiveDeductedAmount].
 *
 * @return [WithdrawalResult] containing group-currency map and per-member per-currency breakdown.
 */
internal fun attributeWithdrawals(
    withdrawals: List<CashWithdrawal>,
    subunitMap: Map<String, Subunit>,
    groupMemberIds: List<String>,
    addOnCalculationService: AddOnCalculationService
): WithdrawalResult {
    val groupCurrencyResult = mutableMapOf<String, Long>()
    // userId → currency → WithdrawalCurrencyAttribution
    val byCurrency = mutableMapOf<String, MutableMap<String, WithdrawalCurrencyAttribution>>()

    for (withdrawal in withdrawals) {
        val effectiveDeducted = addOnCalculationService.calculateEffectiveDeductedAmount(
            withdrawal.deductedBaseAmount,
            withdrawal.addOns
        )
        val distributions = distributeByScope(
            effectiveDeducted,
            withdrawal.withdrawalScope,
            withdrawal.withdrawnBy,
            subunitMap,
            withdrawal.subunitId,
            groupMemberIds
        )
        val nativeDistributions = distributeByScope(
            withdrawal.amountWithdrawn,
            withdrawal.withdrawalScope,
            withdrawal.withdrawnBy,
            subunitMap,
            withdrawal.subunitId,
            groupMemberIds
        )
        val rawDistributions = distributeByScope(
            withdrawal.deductedBaseAmount,
            withdrawal.withdrawalScope,
            withdrawal.withdrawnBy,
            subunitMap,
            withdrawal.subunitId,
            groupMemberIds
        )
        accumulateTotals(groupCurrencyResult, distributions)
        // Use rawDistributions (excluding ATM fee) so per-currency equivalents reflect physical cash value.
        accumulateCurrencyAttribution(byCurrency, nativeDistributions, rawDistributions, withdrawal.currency)
    }

    return WithdrawalResult(groupCurrencyMap = groupCurrencyResult, byCurrency = byCurrency)
}

/**
 * Computes the group-currency equivalent of the remaining (unspent) cash in each
 * withdrawal, attributed to members by scope.
 *
 * Replaces the old `rawWithdrawn − cashSpent` approximation. With scope-aware FIFO
 * updating `remainingAmount` on every CASH expense, `remainingAmount` is always accurate.
 *
 * Conversion: `groupCurrencyRemaining = remainingAmount × deductedBaseAmount ÷ amountWithdrawn`
 *
 * @return [RemainingResult] containing per-member scalar `cashInHand` and per-currency breakdown.
 */
internal fun attributeRemainingByScope(
    withdrawals: List<CashWithdrawal>,
    subunitMap: Map<String, Subunit>,
    groupMemberIds: List<String>
): RemainingResult {
    val groupCurrencyResult = mutableMapOf<String, Long>()
    val byCurrency = mutableMapOf<String, MutableMap<String, RemainingCurrencyAttribution>>()

    for (withdrawal in withdrawals) {
        if (withdrawal.amountWithdrawn == 0L) continue

        val groupCurrencyRemaining = BigDecimal(withdrawal.remainingAmount)
            .multiply(BigDecimal(withdrawal.deductedBaseAmount))
            .divide(BigDecimal(withdrawal.amountWithdrawn), 0, RoundingMode.HALF_UP)
            .toLong()

        val groupDistributions = distributeByScope(
            groupCurrencyRemaining,
            withdrawal.withdrawalScope,
            withdrawal.withdrawnBy,
            subunitMap,
            withdrawal.subunitId,
            groupMemberIds
        )
        accumulateTotals(groupCurrencyResult, groupDistributions)

        val nativeDistributions = distributeByScope(
            withdrawal.remainingAmount,
            withdrawal.withdrawalScope,
            withdrawal.withdrawnBy,
            subunitMap,
            withdrawal.subunitId,
            groupMemberIds
        )

        for ((userId, nativeRemaining) in nativeDistributions) {
            val groupEquiv = groupDistributions[userId] ?: 0L
            val userMap = byCurrency.getOrPut(userId) { mutableMapOf() }
            val existing = userMap[withdrawal.currency]
            userMap[withdrawal.currency] = if (existing != null) {
                RemainingCurrencyAttribution(
                    nativeRemaining = existing.nativeRemaining + nativeRemaining,
                    groupEquivalent = existing.groupEquivalent + groupEquiv
                )
            } else {
                RemainingCurrencyAttribution(nativeRemaining = nativeRemaining, groupEquivalent = groupEquiv)
            }
        }
    }

    return RemainingResult(groupCurrencyMap = groupCurrencyResult, byCurrency = byCurrency)
}

/**
 * Sums expense split amounts per user, separated by payment method (CASH vs non-CASH),
 * tracking both group-currency totals and per-source-currency native amounts.
 *
 * **Add-ons are included via [AddOnCalculationService.calculateEffectiveGroupAmount].**
 */
internal fun attributeExpensesByPaymentMethod(
    expenses: List<Expense>,
    addOnCalculationService: AddOnCalculationService
): ExpenseResult {
    val accumulator = ExpenseAttributionAccumulator(addOnCalculationService)
    for (expense in expenses) {
        accumulator.accumulate(expense)
    }
    return accumulator.toResult()
}

private class ExpenseAttributionAccumulator(
    private val addOnCalculationService: AddOnCalculationService
) {
    val cashResult = mutableMapOf<String, Long>()
    val nonCashResult = mutableMapOf<String, Long>()
    val refundableResult = mutableMapOf<String, Long>()
    val cashByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val nonCashByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val refundableByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val cashEquivByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val nonCashEquivByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val refundableEquivByCurrency = mutableMapOf<String, MutableMap<String, Long>>()

    private data class TargetDestination(
        val spentMap: MutableMap<String, Long>,
        val byCurrencyMap: MutableMap<String, MutableMap<String, Long>>,
        val equivByCurrencyMap: MutableMap<String, MutableMap<String, Long>>
    )

    private fun resolveDestinations(
        paymentStatus: PaymentStatus,
        paymentMethod: PaymentMethod
    ): List<TargetDestination> {
        val isRefundable = paymentStatus == PaymentStatus.REFUNDABLE
        val isCash = paymentMethod == PaymentMethod.CASH
        val destinations = mutableListOf<TargetDestination>()
        if (isRefundable) {
            destinations.add(TargetDestination(refundableResult, refundableByCurrency, refundableEquivByCurrency))
        }
        if (isCash) {
            destinations.add(TargetDestination(cashResult, cashByCurrency, cashEquivByCurrency))
        } else {
            destinations.add(TargetDestination(nonCashResult, nonCashByCurrency, nonCashEquivByCurrency))
        }
        return destinations
    }

    fun accumulate(expense: Expense) {
        if (expense.paymentStatus == PaymentStatus.CANCELLED) {
            return
        }
        if (expense.subExpenses.isEmpty()) {
            accumulateSingle(expense)
        } else {
            accumulateComposite(expense)
        }
    }

    private fun accumulateSingle(expense: Expense) {
        val destinations = resolveDestinations(expense.paymentStatus, expense.paymentMethod)
        val effectiveGroupAmount = addOnCalculationService.calculateEffectiveGroupAmount(
            expense.groupAmount,
            expense.addOns
        )
        for (split in expense.splits) {
            if (!split.isExcluded) {
                val spentInGroupCurrency = convertSplitToGroupCurrency(
                    split.amountCents,
                    expense.sourceAmount,
                    effectiveGroupAmount
                )
                for (dest in destinations) {
                    dest.spentMap[split.userId] = (dest.spentMap[split.userId] ?: 0L) + spentInGroupCurrency

                    val userCurrencyMap = dest.byCurrencyMap.getOrPut(split.userId) { mutableMapOf() }
                    userCurrencyMap[expense.sourceCurrency] =
                        (userCurrencyMap[expense.sourceCurrency] ?: 0L) + split.amountCents

                    val userEquivMap = dest.equivByCurrencyMap.getOrPut(split.userId) { mutableMapOf() }
                    userEquivMap[expense.sourceCurrency] =
                        (userEquivMap[expense.sourceCurrency] ?: 0L) + spentInGroupCurrency
                }
            }
        }
    }

    private fun accumulateComposite(expense: Expense) {
        for (subExpense in expense.subExpenses) {
            if (subExpense.paymentStatus == PaymentStatus.CANCELLED) continue
            val destinations = resolveDestinations(subExpense.paymentStatus, subExpense.paymentMethod)
            val effectiveGroupAmount = addOnCalculationService.calculateEffectiveGroupAmount(
                subExpense.groupAmountCents,
                subExpense.addOns
            )
            for (split in expense.splits) {
                if (split.isExcluded) continue
                accumulateSubExpenseSplit(
                    destinations = destinations,
                    split = split,
                    expenseSourceAmount = expense.sourceAmount,
                    subExpense = subExpense,
                    effectiveGroupAmount = effectiveGroupAmount
                )
            }
        }
    }

    private fun accumulateSubExpenseSplit(
        destinations: List<TargetDestination>,
        split: ExpenseSplit,
        expenseSourceAmount: Long,
        subExpense: SubExpense,
        effectiveGroupAmount: Long
    ) {
        val spentInGroupCurrency = convertSplitToGroupCurrency(
            split.amountCents,
            expenseSourceAmount,
            effectiveGroupAmount
        )
        val splitNativeForSub = convertSplitToGroupCurrency(
            split.amountCents,
            expenseSourceAmount,
            subExpense.amountCents
        )
        for (dest in destinations) {
            dest.spentMap[split.userId] = (dest.spentMap[split.userId] ?: 0L) + spentInGroupCurrency

            val userCurrencyMap = dest.byCurrencyMap.getOrPut(split.userId) { mutableMapOf() }
            userCurrencyMap[subExpense.currency] =
                (userCurrencyMap[subExpense.currency] ?: 0L) + splitNativeForSub

            val userEquivMap = dest.equivByCurrencyMap.getOrPut(split.userId) { mutableMapOf() }
            userEquivMap[subExpense.currency] =
                (userEquivMap[subExpense.currency] ?: 0L) + spentInGroupCurrency
        }
    }

    fun toResult() = ExpenseResult(
        cashSpentMap = cashResult,
        nonCashSpentMap = nonCashResult,
        refundableSpentMap = refundableResult,
        cashSpentByCurrency = cashByCurrency,
        nonCashSpentByCurrency = nonCashByCurrency,
        refundableSpentByCurrency = refundableByCurrency,
        cashEquivByCurrency = cashEquivByCurrency,
        nonCashEquivByCurrency = nonCashEquivByCurrency,
        refundableEquivByCurrency = refundableEquivByCurrency
    )
}

/**
 * Builds per-currency [CurrencyAmount] list for cash in hand using the
 * sum-of-remaining approach. Filters out currencies with zero remaining native amount.
 */
internal fun buildCashInHandByCurrency(
    remainingByCurrency: Map<String, RemainingCurrencyAttribution>,
    groupCurrency: String
): List<CurrencyAmount> = remainingByCurrency.mapNotNull { (currency, attribution) ->
    if (attribution.nativeRemaining <= 0) return@mapNotNull null
    val equivalent = if (currency == groupCurrency) attribution.nativeRemaining else attribution.groupEquivalent
    CurrencyAmount(currency = currency, amountCents = attribution.nativeRemaining, equivalentCents = equivalent)
}.sortedBy { it.currency }

/**
 * Builds per-currency [CurrencyAmount] list for total cash withdrawn attributed to a member.
 */
internal fun buildWithdrawnByCurrency(
    byCurrencyMap: Map<String, WithdrawalCurrencyAttribution>,
    groupCurrency: String
): List<CurrencyAmount> {
    if (byCurrencyMap.isEmpty()) return emptyList()
    return byCurrencyMap.map { (currency, attribution) ->
        val equivalent = if (currency == groupCurrency) attribution.nativeAmount else attribution.groupEquivalent
        CurrencyAmount(currency = currency, amountCents = attribution.nativeAmount, equivalentCents = equivalent)
    }.sortedBy { it.currency }
}

/**
 * Builds per-currency [CurrencyAmount] list for expense breakdowns using exact per-user equivalents.
 */
internal fun buildCurrencyAmountList(
    byCurrencyMap: Map<String, Long>,
    equivByCurrency: Map<String, Long>,
    groupCurrency: String
): List<CurrencyAmount> {
    if (byCurrencyMap.isEmpty()) return emptyList()
    return byCurrencyMap.map { (currency, nativeAmountCents) ->
        val equivalent = if (currency == groupCurrency) nativeAmountCents else (equivByCurrency[currency] ?: 0L)
        CurrencyAmount(currency = currency, amountCents = nativeAmountCents, equivalentCents = equivalent)
    }.sortedBy { it.currency }
}

/**
 * Converts a split amount from source currency to group currency.
 * `splitGroupAmount = splitAmountCents × groupAmount ÷ sourceAmount` (HALF_UP).
 */
internal fun convertSplitToGroupCurrency(
    splitAmountCents: Long,
    sourceAmount: Long,
    groupAmount: Long
): Long {
    if (sourceAmount == 0L) return 0L
    if (sourceAmount == groupAmount) return splitAmountCents
    return BigDecimal(splitAmountCents)
        .multiply(BigDecimal(groupAmount))
        .divide(BigDecimal(sourceAmount), 0, RoundingMode.HALF_UP)
        .toLong()
}

/** Adds each userId→amount entry from [distributions] into the running [totals] map. */
internal fun accumulateTotals(
    totals: MutableMap<String, Long>,
    distributions: Map<String, Long>
) {
    for ((userId, amount) in distributions) {
        totals[userId] = (totals[userId] ?: 0L) + amount
    }
}

/**
 * Merges per-currency native and group-equivalent amounts from a single withdrawal
 * into the running [byCurrency] accumulator.
 */
internal fun accumulateCurrencyAttribution(
    byCurrency: MutableMap<String, MutableMap<String, WithdrawalCurrencyAttribution>>,
    nativeDistributions: Map<String, Long>,
    rawDistributions: Map<String, Long>,
    currency: String
) {
    for ((userId, nativeAmount) in nativeDistributions) {
        val groupEquivalent = rawDistributions[userId] ?: 0L
        val userMap = byCurrency.getOrPut(userId) { mutableMapOf() }
        val existing = userMap[currency]
        userMap[currency] = if (existing != null) {
            WithdrawalCurrencyAttribution(
                nativeAmount = existing.nativeAmount + nativeAmount,
                groupEquivalent = existing.groupEquivalent + groupEquivalent
            )
        } else {
            WithdrawalCurrencyAttribution(nativeAmount = nativeAmount, groupEquivalent = groupEquivalent)
        }
    }
}

/**
 * Distributes an [amount] among members according to the [scope] (GROUP/SUBUNIT/USER).
 *
 * Consolidates the common when-dispatch pattern used by attribution functions
 * to avoid repeated branching logic.
 */
internal fun distributeByScope(
    amount: Long,
    scope: PayerType,
    fallbackUserId: String,
    subunitMap: Map<String, Subunit>,
    subunitId: String?,
    groupMemberIds: List<String>
): Map<String, Long> = when (scope) {
    PayerType.GROUP -> balanceDistributeEvenly(amount, groupMemberIds)
    PayerType.SUBUNIT -> {
        val subunit = subunitId?.let { subunitMap[it] }
        if (subunit == null || subunit.memberShares.isEmpty()) {
            mapOf(fallbackUserId to amount)
        } else {
            balanceDistributeByShares(amount, subunit.memberShares)
        }
    }
    PayerType.USER -> mapOf(fallbackUserId to amount)
}
