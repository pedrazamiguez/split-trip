package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.AppDateTimeSelectionSection
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.DueDateSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.RefundDateSection
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.PaymentStatusSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 8: Payment status + conditional due date.
 * When status is SCHEDULED, a date picker appears.
 */
@Composable
fun PaymentStatusStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        PaymentStatusSection(
            availablePaymentStatuses = uiState.availablePaymentStatuses,
            selectedPaymentStatus = uiState.selectedPaymentStatus,
            onPaymentStatusSelected = { onEvent(AddExpenseUiEvent.PaymentStatusSelected(it)) }
        )

        AnimatedVisibility(visible = uiState.showDueDateSection) {
            DueDateSection(
                formattedDueDate = uiState.formattedDueDate,
                isDueDateValid = uiState.isDueDateValid,
                dueDateMillis = uiState.dueDateMillis,
                onDateSelected = { onEvent(AddExpenseUiEvent.DueDateSelected(it)) }
            )
        }

        AnimatedVisibility(visible = uiState.showRefundDateSection) {
            RefundDateSection(
                formattedRefundDate = uiState.formattedRefundDate,
                refundDateMillis = uiState.refundDateMillis,
                onDateSelected = { onEvent(AddExpenseUiEvent.RefundDateSelected(it)) }
            )
        }

        AppDateTimeSelectionSection(
            title = stringResource(R.string.add_expense_date_time_title),
            label = stringResource(R.string.add_expense_date_time_label),
            formattedDateTime = uiState.formattedExpenseDate,
            isDateTimeValid = uiState.isExpenseDateValid,
            dateTimeMillis = uiState.expenseDateMillis,
            onDateTimeSelected = { onEvent(AddExpenseUiEvent.ExpenseDateSelected(it)) }
        )
    }
}
