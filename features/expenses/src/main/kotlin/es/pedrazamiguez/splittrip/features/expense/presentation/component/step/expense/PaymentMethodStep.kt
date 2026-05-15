package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.payment.PaymentMethodSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 2: Payment method selection.
 * Always shown — determines whether the exchange rate is locked (CASH) or editable.
 * Auto-advances to the next step after a selection is made.
 */
@Composable
fun PaymentMethodStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onAutoAdvance: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        PaymentMethodSection(
            paymentMethods = uiState.paymentMethods,
            selectedPaymentMethod = uiState.selectedPaymentMethod,
            onPaymentMethodSelected = { methodId ->
                onEvent(AddExpenseUiEvent.PaymentMethodSelected(methodId))
                onAutoAdvance()
            }
        )
    }
}
