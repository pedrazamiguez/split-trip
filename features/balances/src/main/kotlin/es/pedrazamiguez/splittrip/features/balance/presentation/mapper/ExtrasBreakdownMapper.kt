package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.extensions.toEpochMillisUtc
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtraItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtrasBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.RawExtraItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal fun mapExtrasBreakdown(
    expenses: List<Expense>,
    withdrawals: List<CashWithdrawal>,
    context: ExtrasBreakdownContext
): ImmutableList<ExtrasBreakdownUiModel> {
    val youLabel = context.resourceProvider.getString(R.string.balances_member_you)
    val rawExtras = mutableListOf<RawExtraItem>()

    rawExtras.addAll(
        gatherExpenseExtras(expenses, context, youLabel)
    )

    rawExtras.addAll(
        gatherWithdrawalExtras(withdrawals, context, youLabel)
    )

    val sortedRawExtras = rawExtras.sortedByDescending { it.createdAt?.toEpochMillisUtc() ?: 0L }
    return buildTypeBreakdown(sortedRawExtras, context)
}

private fun gatherExpenseExtras(
    expenses: List<Expense>,
    context: ExtrasBreakdownContext,
    youLabel: String
): List<RawExtraItem> {
    val rawExtras = mutableListOf<RawExtraItem>()
    for (expense in expenses) {
        val activeSplits = expense.splits.filter { !it.isExcluded }
        val scopeLabel = when {
            activeSplits.size == 1 -> {
                val userId = activeSplits.first().userId
                context.userUiMapper.mapToDisplayName(
                    context.memberProfiles[userId],
                    userId,
                    context.currentUserId,
                    youLabel
                )
            }
            activeSplits.isNotEmpty() &&
                activeSplits.all {
                    it.subunitId != null && it.subunitId == activeSplits.first().subunitId
                } -> {
                val subunitId = activeSplits.first().subunitId!!
                context.subunitsMap[subunitId]?.name ?: context.resourceProvider.getString(
                    R.string.balances_cash_breakdown_unknown_subunit
                )
            }
            else -> {
                context.resourceProvider.getString(R.string.balances_contribution_scope_group)
            }
        }
        val title = expense.title.ifBlank {
            context.resourceProvider.getString(R.string.balances_extras_expense_fallback)
        }

        for (addOn in expense.addOns) {
            if (addOn.type != AddOnType.DISCOUNT) {
                rawExtras.add(
                    RawExtraItem(
                        parentTitle = title,
                        createdAt = expense.createdAt,
                        addOn = addOn,
                        scopeLabel = scopeLabel
                    )
                )
            }
        }
    }
    return rawExtras
}

private fun gatherWithdrawalExtras(
    withdrawals: List<CashWithdrawal>,
    context: ExtrasBreakdownContext,
    youLabel: String
): List<RawExtraItem> {
    val rawExtras = mutableListOf<RawExtraItem>()
    for (withdrawal in withdrawals) {
        val scopeLabel = when (withdrawal.withdrawalScope) {
            PayerType.USER -> {
                context.userUiMapper.mapToDisplayName(
                    context.memberProfiles[withdrawal.withdrawnBy],
                    withdrawal.withdrawnBy,
                    context.currentUserId,
                    youLabel
                )
            }
            PayerType.SUBUNIT -> {
                withdrawal.subunitId?.let { context.subunitsMap[it]?.name }
                    ?: context.resourceProvider.getString(
                        R.string.balances_cash_breakdown_unknown_subunit
                    )
            }
            PayerType.GROUP -> {
                context.resourceProvider.getString(R.string.balances_contribution_scope_group)
            }
        }

        val dateText = withdrawal.createdAt?.formatShortDate(context.locale) ?: ""
        val title = if (withdrawal.title.isNullOrBlank()) {
            context.resourceProvider.getString(R.string.balances_extras_atm_fallback, dateText)
        } else {
            withdrawal.title!!
        }

        for (addOn in withdrawal.addOns) {
            if (addOn.type != AddOnType.DISCOUNT) {
                rawExtras.add(
                    RawExtraItem(
                        parentTitle = title,
                        createdAt = withdrawal.createdAt,
                        addOn = addOn,
                        scopeLabel = scopeLabel
                    )
                )
            }
        }
    }
    return rawExtras
}

private fun buildTypeBreakdown(
    sortedRawExtras: List<RawExtraItem>,
    context: ExtrasBreakdownContext
): ImmutableList<ExtrasBreakdownUiModel> {
    val result = mutableListOf<ExtrasBreakdownUiModel>()
    val groups = listOf(AddOnType.FEE, AddOnType.SURCHARGE, AddOnType.TIP)

    for (type in groups) {
        val itemsOfType = sortedRawExtras.filter { it.addOn.type == type }
        if (itemsOfType.isNotEmpty()) {
            val groupedByScope = itemsOfType.groupBy { it.scopeLabel }
            val uiItems = groupedByScope.entries
                .sortedBy { it.key }
                .map { (scopeLabel, items) ->
                    val sumCents = items.sumOf { it.addOn.groupAmountCents }
                    ExtraItemUiModel(
                        parentTitle = scopeLabel,
                        dateText = "",
                        description = null,
                        formattedAmount = formatCurrencyAmount(
                            sumCents,
                            context.groupCurrency,
                            context.locale
                        ),
                        scopeLabel = ""
                    )
                }.toImmutableList()

            val subtotalCents = itemsOfType.sumOf { it.addOn.groupAmountCents }
            val formattedSubtotal = formatCurrencyAmount(
                subtotalCents,
                context.groupCurrency,
                context.locale
            )

            val typeLabel = when (type) {
                AddOnType.FEE -> context.resourceProvider.getString(
                    R.string.balances_extras_add_on_fee_plural
                )
                AddOnType.SURCHARGE -> context.resourceProvider.getString(
                    R.string.balances_extras_add_on_surcharge_plural
                )
                AddOnType.TIP -> context.resourceProvider.getString(
                    R.string.balances_extras_add_on_tip_plural
                )
                else -> ""
            }

            result.add(
                ExtrasBreakdownUiModel(
                    typeLabel = typeLabel,
                    items = uiItems,
                    formattedSubtotal = formattedSubtotal
                )
            )
        }
    }
    return result.toImmutableList()
}
