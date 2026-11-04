package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatForDisplay
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceCashContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** Computes the user's attributed native share (in withdrawal currency cents) for display. */
internal fun computeUserNativeShare(
    withdrawal: CashWithdrawal,
    userId: String,
    groupMemberIds: List<String>,
    subunitsMap: Map<String, Subunit>
): Long {
    val remaining = withdrawal.remainingAmount
    return when (withdrawal.withdrawalScope) {
        PayerType.USER -> if (withdrawal.withdrawnBy == userId) remaining else 0L
        PayerType.GROUP -> {
            if (groupMemberIds.isEmpty()) 0L else remaining / groupMemberIds.size
        }
        PayerType.SUBUNIT -> {
            val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
            val share = subunit?.memberShares?.get(userId) ?: return 0L
            BigDecimal(remaining)
                .multiply(share)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }
}

/** Computes the total scaled fee cents for a member over all withdrawals. */
internal fun computeMemberTotalFees(
    userId: String,
    withdrawals: List<CashWithdrawal>,
    groupMemberIds: List<String>,
    subunitsMap: Map<String, Subunit>
): Long {
    return withdrawals.sumOf { withdrawal ->
        if (withdrawal.amountWithdrawn == 0L || withdrawal.remainingAmount <= 0L) return@sumOf 0L
        val nativeShare = computeUserNativeShare(withdrawal, userId, groupMemberIds, subunitsMap)
        if (nativeShare > 0L) {
            val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
            withdrawal.addOns
                .filter { it.type != AddOnType.DISCOUNT }
                .sumOf { addOn ->
                    computeAddOnShare(
                        addOn = addOn,
                        scope = withdrawal.withdrawalScope,
                        withdrawnBy = withdrawal.withdrawnBy,
                        userId = userId,
                        groupMemberIds = groupMemberIds,
                        subunit = subunit
                    )
                }
        } else {
            0L
        }
    }
}

/** Helper to compute a single add-on's share for a member. */
internal fun computeAddOnShare(
    addOn: AddOn,
    scope: PayerType,
    withdrawnBy: String,
    userId: String,
    groupMemberIds: List<String>,
    subunit: Subunit?
): Long {
    return when (scope) {
        PayerType.USER -> if (withdrawnBy == userId) addOn.groupAmountCents else 0L
        PayerType.GROUP -> if (groupMemberIds.isEmpty()) 0L else addOn.groupAmountCents / groupMemberIds.size
        PayerType.SUBUNIT -> {
            val share = subunit?.memberShares?.get(userId) ?: BigDecimal.ZERO
            BigDecimal(addOn.groupAmountCents)
                .multiply(share)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
    }
}

internal fun mapCashBreakdown(
    userId: String,
    withdrawals: List<CashWithdrawal>,
    subunitsMap: Map<String, Subunit>,
    groupMemberIds: List<String>,
    groupCurrency: String,
    locale: Locale,
    resourceProvider: ResourceProvider
): ImmutableList<CashBreakdownUiModel> {
    val scopeOrder = mapOf(PayerType.GROUP to 0, PayerType.SUBUNIT to 1, PayerType.USER to 2)
    val cashContext = MemberBalanceCashContext(
        withdrawals = withdrawals,
        subunitsMap = subunitsMap,
        groupMemberIds = groupMemberIds
    )
    return withdrawals
        .sortedWith(
            compareBy<CashWithdrawal> { scopeOrder[it.withdrawalScope] ?: 3 }
                .thenByDescending { it.createdAt }
        )
        .mapNotNull { withdrawal ->
            if (withdrawal.amountWithdrawn == 0L || withdrawal.remainingAmount <= 0L) return@mapNotNull null
            val nativeShare = computeUserNativeShare(withdrawal, userId, groupMemberIds, subunitsMap)
            if (nativeShare <= 0L) return@mapNotNull null
            buildCashBreakdownEntry(
                withdrawal = withdrawal,
                nativeShare = nativeShare,
                userId = userId,
                groupCurrency = groupCurrency,
                locale = locale,
                resourceProvider = resourceProvider,
                cashContext = cashContext
            )
        }
        .toImmutableList()
}

private fun buildCashBreakdownEntry(
    withdrawal: CashWithdrawal,
    nativeShare: Long,
    userId: String,
    groupCurrency: String,
    locale: Locale,
    resourceProvider: ResourceProvider,
    cashContext: MemberBalanceCashContext
): CashBreakdownUiModel {
    val groupEquivalent = BigDecimal(nativeShare)
        .multiply(BigDecimal(withdrawal.deductedBaseAmount))
        .divide(BigDecimal(withdrawal.amountWithdrawn), 0, RoundingMode.HALF_UP)
        .toLong()
    val isForeign = withdrawal.currency != groupCurrency
    val dateText = withdrawal.createdAt?.formatShortDate(locale) ?: ""
    val label = if (withdrawal.title.isNullOrBlank()) {
        resourceProvider.getString(R.string.balances_cash_breakdown_atm_fallback, dateText)
    } else {
        withdrawal.title ?: ""
    }
    return CashBreakdownUiModel(
        withdrawalLabel = label,
        dateText = dateText,
        formattedRate = if (isForeign) {
            resourceProvider.getString(
                R.string.balances_cash_breakdown_rate,
                withdrawal.exchangeRate.formatForDisplay(locale, maxDecimalPlaces = 6),
                withdrawal.currency,
                groupCurrency
            )
        } else {
            ""
        },
        formattedNativeRemaining = formatCurrencyAmount(nativeShare, withdrawal.currency, locale),
        formattedEquivalent = if (isForeign) {
            formatCurrencyAmount(groupEquivalent, groupCurrency, locale)
        } else {
            ""
        },
        scopeLabel = when (withdrawal.withdrawalScope) {
            PayerType.GROUP -> resourceProvider.getString(R.string.balances_cash_breakdown_group_scope)
            PayerType.USER -> resourceProvider.getString(R.string.balances_cash_breakdown_personal_scope)
            PayerType.SUBUNIT -> withdrawal.subunitId?.let { cashContext.subunitsMap[it]?.name }
                ?: resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)
        },
        isEstimatedShare = withdrawal.withdrawalScope == PayerType.GROUP,
        formattedAddOns = formatWithdrawalAddOns(
            withdrawal = withdrawal,
            userId = userId,
            groupMemberIds = cashContext.groupMemberIds,
            subunitsMap = cashContext.subunitsMap,
            groupCurrency = groupCurrency,
            locale = locale
        )
    )
}

private fun formatWithdrawalAddOns(
    withdrawal: CashWithdrawal,
    userId: String,
    groupMemberIds: List<String>,
    subunitsMap: Map<String, Subunit>,
    groupCurrency: String,
    locale: Locale
): String {
    val nonDiscountAddOns = withdrawal.addOns.filter { it.type != AddOnType.DISCOUNT }
    if (nonDiscountAddOns.isEmpty()) return ""
    val subunit = withdrawal.subunitId?.let { subunitsMap[it] }
    val totalUserAddOnCents = nonDiscountAddOns.sumOf { addOn ->
        computeAddOnShare(
            addOn = addOn,
            scope = withdrawal.withdrawalScope,
            withdrawnBy = withdrawal.withdrawnBy,
            userId = userId,
            groupMemberIds = groupMemberIds,
            subunit = subunit
        )
    }
    if (totalUserAddOnCents <= 0L) return ""
    return formatCurrencyAmount(totalUserAddOnCents, groupCurrency, locale)
}
