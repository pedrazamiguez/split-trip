package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.DueDateSection
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
    }
}
