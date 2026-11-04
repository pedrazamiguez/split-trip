package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.HeldBreakdownContext
import es.pedrazamiguez.splittrip.features.balance.presentation.model.HeldExpenseEntry
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricBreakdownItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricScopeBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ScopeKey
import es.pedrazamiguez.splittrip.features.balance.presentation.model.scopeComparator
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal fun mapScheduledBreakdown(
    expenses: List<Expense>,
    context: HeldBreakdownContext
): ImmutableList<MetricScopeBreakdownUiModel> {
    val today = LocalDate.now()
    val futureScheduled = expenses.filter { expense ->
        expense.paymentStatus == PaymentStatus.SCHEDULED &&
            expense.dueDate?.toLocalDate()?.isAfter(today) == true
    }
    return buildHeldBreakdown(
        expenses = futureScheduled,
        context = context,
        resolveDateText = { it.dueDate?.formatShortDate(context.locale) }
    )
}

internal fun mapRefundableBreakdown(
    expenses: List<Expense>,
    context: HeldBreakdownContext
): ImmutableList<MetricScopeBreakdownUiModel> {
    val today = LocalDate.now()
    val refundable = expenses.filter { expense ->
        expense.paymentStatus == PaymentStatus.REFUNDABLE &&
            expense.dueDate?.toLocalDate()?.isBefore(today) != true
    }
    return buildHeldBreakdown(
        expenses = refundable,
        context = context,
        resolveDateText = { (it.operationDate ?: it.createdAt)?.formatShortDate(context.locale) }
    )
}

private fun buildHeldBreakdown(
    expenses: List<Expense>,
    context: HeldBreakdownContext,
    resolveDateText: (Expense) -> String?
): ImmutableList<MetricScopeBreakdownUiModel> {
    if (expenses.isEmpty()) return persistentListOf()

    val entries = expenses.map { resolveExpenseEntry(it, context, resolveDateText) }
    val grouped = entries.groupBy { it.scopeKey }
    val comparator = scopeComparator(context.currentUserId)

    return grouped.entries
        .sortedWith { e1, e2 -> comparator.compare(e1.key, e2.key) }
        .map { (scopeKey, scopeEntries) ->
            val subtotal = scopeEntries.sumOf { it.effectiveAmount }
            val items = scopeEntries.map { entry ->
                MetricBreakdownItemUiModel(
                    title = entry.title,
                    subtitle = entry.dateText,
                    formattedAmount = formatCurrencyAmount(entry.effectiveAmount, context.groupCurrency, context.locale)
                )
            }.toImmutableList()

            MetricScopeBreakdownUiModel(
                scopeLabel = scopeKey.label,
                scopeType = scopeKey.type,
                formattedSubtotal = formatCurrencyAmount(subtotal, context.groupCurrency, context.locale),
                items = items
            )
        }
        .toImmutableList()
}

private fun resolveExpenseEntry(
    expense: Expense,
    context: HeldBreakdownContext,
    resolveDateText: (Expense) -> String?
): HeldExpenseEntry {
    val youLabel = context.resourceProvider.getString(R.string.balances_member_you)
    val fallbackExpenseTitle = context.resourceProvider.getString(R.string.balances_extras_expense_fallback)
    val groupScopeLabel = context.resourceProvider.getString(R.string.balances_contribution_scope_group)
    val unknownSubunitLabel = context.resourceProvider.getString(R.string.balances_cash_breakdown_unknown_subunit)

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
            activeSplits.all { it.subunitId != null && it.subunitId == activeSplits.first().subunitId } -> {
            val subunitId = activeSplits.first().subunitId!!
            val label = context.subunitsMap[subunitId]?.name ?: unknownSubunitLabel
            ScopeKey(label = label, type = PayerType.SUBUNIT, id = subunitId)
        }
        else -> {
            ScopeKey(label = groupScopeLabel, type = PayerType.GROUP, id = null)
        }
    }

    val effectiveAmount = context.addOnCalculationService.calculateEffectiveGroupAmount(
        expense.groupAmount,
        expense.addOns
    )

    return HeldExpenseEntry(
        scopeKey = scopeKey,
        title = expense.title.ifBlank { fallbackExpenseTitle },
        dateText = resolveDateText(expense),
        effectiveAmount = effectiveAmount
    )
}
