package es.pedrazamiguez.splittrip.features.contribution.presentation.component.step

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.rememberAutoFocusRequester
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LargeBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

/**
 * Step 1: Amount input.
 * The amount field auto-focuses so the keyboard opens immediately.
 */
@Composable
fun ContributionAmountStep(
    uiState: AddContributionUiState,
    onEvent: (AddContributionUiEvent) -> Unit,
    onSubmitKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = rememberAutoFocusRequester()
    val focusManager = LocalFocusManager.current

    WizardStepLayout(modifier = modifier) {
        StyledOutlinedTextField(
            value = uiState.amountInput,
            onValueChange = { onEvent(AddContributionUiEvent.UpdateAmount(it)) },
            label = stringResource(R.string.contribution_add_money_amount_hint),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Decimal,
            isError = uiState.amountError,
            suffix = {
                if (uiState.groupCurrencySymbol.isNotBlank()) {
                    LargeBodyText(
                        text = uiState.groupCurrencySymbol,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            supportingText = if (uiState.amountError) {
                stringResource(R.string.contribution_add_money_error_amount)
            } else {
                null
            },
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (uiState.isCurrentStepValid) {
                        onSubmitKeyboard()
                    }
                }
            ),
            focusRequester = focusRequester,
            moveCursorToEndOnFocus = true
        )
    }
}
