package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
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
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
        ) {
            if (uiState.isAiCapable && uiState.isAiModeActive) {
                Text(
                    text = stringResource(R.string.expense_autofill_ai_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ReceiptSection(
                receiptUri = uiState.receiptUri,
                mimeType = uiState.receiptAttachment?.mimeType,
                onPickerRequested = { onEvent(AddExpenseUiEvent.RequestPickerSource) },
                onRemoveImage = { onEvent(AddExpenseUiEvent.RemoveReceiptImage) },
                onViewImage = { onEvent(AddExpenseUiEvent.ViewReceiptFullScreen) }
            )

            if (uiState.isAiCapable && uiState.isAiModeActive) {
                SecondaryButton(
                    text = stringResource(R.string.expense_autofill_switch_manual),
                    onClick = { onEvent(AddExpenseUiEvent.SetAiModeActive(false)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
