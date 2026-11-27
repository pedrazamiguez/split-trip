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
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.rememberAutoFocusRequester
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.AiScanPromptCard
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.LocalAiPackPromptBanner
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Step 1: Expense title.
 * Auto-focused so the keyboard opens immediately.
 */
@Composable
fun TitleStep(
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val titleFocusRequester = rememberAutoFocusRequester()

    WizardStepLayout(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
        ) {
            if (uiState.isAiCapable && !uiState.isAiModeActive && !uiState.isEditMode) {
                AiScanPromptCard(
                    isProFeature = uiState.isAiProFeature,
                    onSwitchToAi = {
                        onEvent(AddExpenseUiEvent.SetAiModeActive(true))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    remainingScansLabel = uiState.aiRemainingScansLabel,
                    isQuotaExhausted = uiState.isAiGated
                )
            } else if (!uiState.isAiCapable && !uiState.isEditMode) {
                uiState.localAiPackBanner?.let { banner ->
                    LocalAiPackPromptBanner(
                        banner = banner,
                        onManageClick = { onEvent(AddExpenseUiEvent.OpenLocalAiPackSettings) },
                        onDismissClick = { onEvent(AddExpenseUiEvent.DismissLocalAiPackBanner) }
                    )
                }
            }

            StyledOutlinedTextField(
                value = uiState.expenseTitle,
                onValueChange = { onEvent(AddExpenseUiEvent.TitleChanged(it)) },
                label = stringResource(R.string.add_expense_what_for),
                modifier = Modifier.fillMaxWidth(),
                isError = !uiState.isTitleValid,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onImeNext()
                    }
                ),
                focusRequester = titleFocusRequester,
                moveCursorToEndOnFocus = true
            )
        }
    }
}
