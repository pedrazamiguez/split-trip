package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtrasBreakdownContext
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricScopeBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.RawExtraItem
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ScopeKey
import es.pedrazamiguez.splittrip.features.balance.presentation.model.scopeComparator
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun mapExtrasBreakdown(
    expenses: List<Expense>,
    withdrawals: List<CashWithdrawal>,
    context: ExtrasBreakdownContext
): ImmutableList<MetricScopeBreakdownUiModel> {
    val youLabel = context.resourceProvider.getString(R.string.balances_member_you)
    val rawExtras = mutableListOf<RawExtraItem>()

    rawExtras.addAll(
        gatherExpenseExtras(expenses, context, youLabel)
    )

    rawExtras.addAll(
        gatherWithdrawalExtras(withdrawals, context, youLabel)
    )

    if (rawExtras.isEmpty()) return persistentListOf()

    return buildScopeBreakdown(rawExtras, context)
}

private fun gatherExpenseExtras(
    expenses: List<Expense>,
    context: ExtrasBreakdownContext,
    youLabel: String
): List<RawExtraItem> {
    val today = LocalDate.now()
    val activeExpenses = expenses.filter { expense ->
        val isFutureScheduled = expense.paymentStatus == PaymentStatus.SCHEDULED &&
            expense.dueDate?.toLocalDate()?.isAfter(today) == true
        expense.paymentStatus != PaymentStatus.CANCELLED && !isFutureScheduled
    }

    val rawExtras = mutableListOf<RawExtraItem>()
    val unknownSubunitLabel = context.resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)
    val groupScopeLabel = context.resourceProvider.getString(R.string.balances_contribution_scope_group)
    val fallbackExpenseTitle = context.resourceProvider.getString(R.string.balances_extras_expense_fallback)

    for (expense in activeExpenses) {
        val activeSplits = expense.splits.filter { !it.isExcluded }
        val scopeKey = when {
            activeSplits.size == 1 -> {
                val userId = activeSplits.first().userId
                val label = context.userUiMapper.mapToDisplayName(
                    context.memberProfiles[userId],
                    userId,
                    context.currentUserId,
                    youLabel
                )
                ScopeKey(label = label, type = PayerType.USER, id = userId)
            }
            activeSplits.isNotEmpty() &&
                activeSplits.all {
                    it.subunitId != null && it.subunitId == activeSplits.first().subunitId
                } -> {
                val subunitId = activeSplits.first().subunitId!!
                val name = context.subunitsMap[subunitId]?.name ?: unknownSubunitLabel
                ScopeKey(label = name, type = PayerType.SUBUNIT, id = subunitId)
            }
            else -> {
                ScopeKey(label = groupScopeLabel, type = PayerType.GROUP, id = null)
            }
        }
        val title = expense.title.ifBlank { fallbackExpenseTitle }

        for (addOn in expense.addOns) {
            if (addOn.type != AddOnType.DISCOUNT && addOn.groupAmountCents > 0L) {
                rawExtras.add(
                    RawExtraItem(
                        parentTitle = title,
                        createdAt = expense.createdAt,
                        addOn = addOn,
                        scopeKey = scopeKey
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
    val unknownSubunitLabel = context.resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)
    val groupScopeLabel = context.resourceProvider.getString(R.string.balances_contribution_scope_group)

    for (withdrawal in withdrawals) {
        val scopeKey = when (withdrawal.withdrawalScope) {
            PayerType.USER -> {
                val label = context.userUiMapper.mapToDisplayName(
                    context.memberProfiles[withdrawal.withdrawnBy],
                    withdrawal.withdrawnBy,
                    context.currentUserId,
                    youLabel
                )
                ScopeKey(label = label, type = PayerType.USER, id = withdrawal.withdrawnBy)
            }
            PayerType.SUBUNIT -> {
                val subunitId = withdrawal.subunitId
                val name = subunitId?.let { context.subunitsMap[it]?.name } ?: unknownSubunitLabel
                ScopeKey(label = name, type = PayerType.SUBUNIT, id = subunitId)
            }
            PayerType.GROUP -> {
                ScopeKey(label = groupScopeLabel, type = PayerType.GROUP, id = null)
            }
        }

        val dateText = withdrawal.createdAt?.formatShortDate(context.locale) ?: ""
        val title = if (withdrawal.title.isNullOrBlank()) {
            context.resourceProvider.getString(R.string.balances_extras_atm_fallback, dateText)
        } else {
            withdrawal.title!!
        }

        for (addOn in withdrawal.addOns) {
            if (addOn.type != AddOnType.DISCOUNT && addOn.groupAmountCents > 0L) {
                rawExtras.add(
                    RawExtraItem(
                        parentTitle = title,
                        createdAt = withdrawal.createdAt,
                        addOn = addOn,
                        scopeKey = scopeKey
                    )
                )
            }
        }
    }
    return rawExtras
}

private fun buildScopeBreakdown(
    rawExtras: List<RawExtraItem>,
    context: ExtrasBreakdownContext
): ImmutableList<MetricScopeBreakdownUiModel> {
    val groupedByScope = rawExtras.groupBy { it.scopeKey }
    val comparator = scopeComparator(context.currentUserId)

    return groupedByScope.entries
        .sortedWith { e1, e2 -> comparator.compare(e1.key, e2.key) }
        .mapNotNull { (scopeKey, items) ->
            val typeItems = mutableListOf<MetricBreakdownItemUiModel>()
            val types = listOf(AddOnType.FEE, AddOnType.SURCHARGE, AddOnType.TIP)

            for (type in types) {
                val sumCents = items.filter { it.addOn.type == type }.sumOf { it.addOn.groupAmountCents }
                if (sumCents > 0L) {
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
                    typeItems.add(
                        MetricBreakdownItemUiModel(
                            title = typeLabel,
                            subtitle = null,
                            formattedAmount = formatCurrencyAmount(
                                sumCents,
                                context.groupCurrency,
                                context.locale
                            )
                        )
                    )
                }
            }

            if (typeItems.isEmpty()) {
                null
            } else {
                val subtotalCents = items.sumOf { it.addOn.groupAmountCents }
                MetricScopeBreakdownUiModel(
                    scopeLabel = scopeKey.label,
                    scopeType = scopeKey.type,
                    formattedSubtotal = formatCurrencyAmount(
                        subtotalCents,
                        context.groupCurrency,
                        context.locale
                    ),
                    items = typeItems.toImmutableList()
                )
            }
        }
        .toImmutableList()
}
