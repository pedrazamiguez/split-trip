package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.expense.R
import java.time.LocalDate

/**
 * Centralises all scheduled-payment badge logic so that both [ExpenseUiMapper] (list) and
 * [ExpenseDetailUiMapper] (detail) produce identical badge text from a single source of truth.
 *
 * Returns a [Pair] of (badgeText, isPastDue):
 * - badgeText is null when the expense is not SCHEDULED or has no due date.
 * - isPastDue signals that the due date has passed or is today (used for urgent/error styling).
 */
class ScheduledBadgeUiMapper(
    private val formattingHelper: FormattingHelper,
    private val resourceProvider: ResourceProvider
) {

    fun buildBadge(expense: Expense): Pair<String?, Boolean> {
        val dueDate = expense.dueDate
        if (expense.paymentStatus != PaymentStatus.SCHEDULED || dueDate == null) return null to false

        val today = LocalDate.now()
        val dueDateLocal = dueDate.toLocalDate()

        return when {
            dueDateLocal.isBefore(today) ->
                resourceProvider.getString(R.string.expense_scheduled_paid) to true

            dueDateLocal.isEqual(today) ->
                resourceProvider.getString(R.string.expense_scheduled_due_today) to true

            dueDateLocal.isEqual(today.plusDays(1)) ->
                resourceProvider.getString(R.string.expense_scheduled_due_tomorrow) to false

            else ->
                resourceProvider.getString(
                    R.string.expense_scheduled_due_on,
                    formattingHelper.formatShortDate(dueDate)
                ) to false
        }
    }
}
