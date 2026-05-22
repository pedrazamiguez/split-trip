package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.ReceiptSection
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 9: Receipt image attachment.
 */
@Composable
fun ReceiptStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        ReceiptSection(
            receiptUri = uiState.receiptUri,
            mimeType = uiState.receiptAttachment?.mimeType,
            onPickerRequested = { onEvent(AddExpenseUiEvent.RequestPickerSource) },
            onRemoveImage = { onEvent(AddExpenseUiEvent.RemoveReceiptImage) },
            onViewImage = { onEvent(AddExpenseUiEvent.ViewReceiptFullScreen) }
        )
    }
}
