package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SubcategorySpendingInsight
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.AddOnBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.AddOnTypeBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SubcategoryInsightUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun mapAddOnsBreakdown(
    insights: GroupSpendingInsights,
    formattingHelper: FormattingHelper,
    resourceProvider: ResourceProvider
): AddOnBreakdownUiModel? {
    val summary = insights.addOnsSummary
    val hasExtras = summary.totalExtrasCents > 0L
    val hasDiscounts = summary.totalDiscountsCents > 0L
    if (!hasExtras && !hasDiscounts) return null

    val formattedTotalExtras = formattingHelper.formatCentsWithCurrency(
        cents = summary.totalExtrasCents,
        currencyCode = insights.currency
    )
    val formattedShare = if (summary.expenseExtrasCents > 0L && insights.totalSpentCents > 0L) {
        resourceProvider.getString(
            R.string.balances_insights_add_ons_share,
            summary.extrasBreakdownPercentage.toDisplayString()
        )
    } else {
        null
    }
    val formattedWithdrawalNote = if (summary.withdrawalFeesCents > 0L) {
        resourceProvider.getString(
            R.string.balances_insights_add_ons_withdrawal_note,
            formattingHelper.formatCentsWithCurrency(summary.withdrawalFeesCents, insights.currency)
        )
    } else {
        null
    }
    val formattedDiscountsNote = if (summary.totalDiscountsCents > 0L) {
        resourceProvider.getString(
            R.string.balances_insights_add_ons_discounts_note,
            formattingHelper.formatCentsWithCurrency(summary.totalDiscountsCents, insights.currency)
        )
    } else {
        null
    }
    val typeItems = summary.typeBreakdown.mapNotNull { item ->
        val labelRes = resolveAddOnTypeLabelRes(item.type) ?: return@mapNotNull null
        AddOnTypeBreakdownUiModel(
            type = item.type,
            label = resourceProvider.getString(labelRes),
            icon = item.type.toIconVector(),
            formattedAmount = formattingHelper.formatCentsWithCurrency(item.totalAmountCents, insights.currency)
        )
    }.toImmutableList()

    return AddOnBreakdownUiModel(
        formattedTotalExtras = formattedTotalExtras,
        formattedPercentageOfSpending = formattedShare,
        formattedWithdrawalFeesNote = formattedWithdrawalNote,
        formattedDiscountsSavedNote = formattedDiscountsNote,
        typeBreakdown = typeItems
    )
}

internal fun mapSubcategoryItems(
    subcategories: List<SubcategorySpendingInsight>,
    currency: String,
    formattingHelper: FormattingHelper,
    resourceProvider: ResourceProvider
): ImmutableList<SubcategoryInsightUiModel> = subcategories.map { subcategory ->
    SubcategoryInsightUiModel(
        subcategory = subcategory.subcategory,
        subcategoryName = resourceProvider.getString(subcategory.subcategory.toStringRes()),
        subcategoryIcon = subcategory.subcategory.toIconVector(),
        formattedAmount = formattingHelper.formatCentsWithCurrency(subcategory.totalAmountCents, currency),
        formattedPercentage = subcategory.breakdownPercentage.toDisplayString(),
        expenseCountLabel = resourceProvider.getQuantityString(
            R.plurals.balances_insights_expense_count,
            subcategory.expenseCount,
            subcategory.expenseCount
        )
    )
}.toImmutableList()

internal fun BreakdownPercentage.toDisplayString(): String = when (this) {
    BreakdownPercentage.Zero -> "0%"
    BreakdownPercentage.SubOnePercent -> "<1%"
    is BreakdownPercentage.Whole -> "$percent%"
}

private fun resolveAddOnTypeLabelRes(type: AddOnType): Int? = when (type) {
    AddOnType.FEE -> R.string.balances_extras_add_on_fee_plural
    AddOnType.SURCHARGE -> R.string.balances_extras_add_on_surcharge_plural
    AddOnType.TIP -> R.string.balances_extras_add_on_tip_plural
    AddOnType.DISCOUNT -> null
}
