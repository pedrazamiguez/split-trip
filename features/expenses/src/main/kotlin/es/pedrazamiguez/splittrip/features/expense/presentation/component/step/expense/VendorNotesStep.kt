package es.pedrazamiguez.splittrip.features.expense.presentation.component.step.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 7: Vendor name and optional notes.
 */
@Composable
fun VendorNotesStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    WizardStepLayout(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            SectionHeadingText(text = stringResource(R.string.add_expense_vendor_helper))
            StyledOutlinedTextField(
                value = uiState.vendor,
                onValueChange = { onEvent(AddExpenseUiEvent.VendorChanged(it)) },
                label = stringResource(R.string.add_expense_vendor_label),
                modifier = Modifier.fillMaxWidth(),
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
            SectionHeadingText(text = stringResource(R.string.add_expense_notes_helper))
            StyledOutlinedTextField(
                value = uiState.notes,
                onValueChange = { onEvent(AddExpenseUiEvent.NotesChanged(it)) },
                label = stringResource(R.string.add_expense_notes_label),
                modifier = Modifier.fillMaxWidth(),
                capitalization = KeyboardCapitalization.Sentences,
                singleLine = false,
                maxLines = 5,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onImeNext()
                    }
                )
            )
        }
    }
}
