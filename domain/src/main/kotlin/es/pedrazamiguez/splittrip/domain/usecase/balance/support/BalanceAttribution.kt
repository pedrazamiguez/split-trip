package es.pedrazamiguez.splittrip.domain.usecase.balance.support

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.CurrencyAmount
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Distributes contribution amounts to individual members.
 *
 * - GROUP contributions → equal split among all group members.
 * - SUBUNIT contributions → distribute by member shares with remainder allocation.
 * - USER contributions (individual) → full amount to userId.
 */
internal fun attributeContributions(
    contributions: List<Contribution>,
    subunitMap: Map<String, Subunit>,
    groupMemberIds: List<String>
): Map<String, Long> {
    val result = mutableMapOf<String, Long>()
    for (contribution in contributions) {
        val distributions = distributeByScope(
            contribution.amount,
            contribution.contributionScope,
            contribution.userId,
            subunitMap,
            contribution.subunitId,
            groupMemberIds
        )
        for ((userId, amount) in distributions) {
            result[userId] = (result[userId] ?: 0L) + amount
        }
    }
    return result
}

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
@Suppress("CognitiveComplexMethod") // Branching is inherent to cash/non-cash + per-currency accumulation
internal fun attributeExpensesByPaymentMethod(
    expenses: List<Expense>,
    addOnCalculationService: AddOnCalculationService
): ExpenseResult {
    val cashResult = mutableMapOf<String, Long>()
    val nonCashResult = mutableMapOf<String, Long>()
    val cashByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val nonCashByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val cashEquivByCurrency = mutableMapOf<String, MutableMap<String, Long>>()
    val nonCashEquivByCurrency = mutableMapOf<String, MutableMap<String, Long>>()

    for (expense in expenses) {
        if (expense.paymentStatus == PaymentStatus.CANCELLED) continue
        val isCash = expense.paymentMethod == PaymentMethod.CASH
        val targetMap = if (isCash) cashResult else nonCashResult
        val targetByCurrency = if (isCash) cashByCurrency else nonCashByCurrency
        val targetEquivByCurrency = if (isCash) cashEquivByCurrency else nonCashEquivByCurrency

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
                targetMap[split.userId] = (targetMap[split.userId] ?: 0L) + spentInGroupCurrency

                val userCurrencyMap = targetByCurrency.getOrPut(split.userId) { mutableMapOf() }
                userCurrencyMap[expense.sourceCurrency] =
                    (userCurrencyMap[expense.sourceCurrency] ?: 0L) + split.amountCents

                val userEquivMap = targetEquivByCurrency.getOrPut(split.userId) { mutableMapOf() }
                userEquivMap[expense.sourceCurrency] =
                    (userEquivMap[expense.sourceCurrency] ?: 0L) + spentInGroupCurrency
            }
        }
    }

    return ExpenseResult(
        cashSpentMap = cashResult,
        nonCashSpentMap = nonCashResult,
        cashSpentByCurrency = cashByCurrency,
        nonCashSpentByCurrency = nonCashByCurrency,
        cashEquivByCurrency = cashEquivByCurrency,
        nonCashEquivByCurrency = nonCashEquivByCurrency
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
