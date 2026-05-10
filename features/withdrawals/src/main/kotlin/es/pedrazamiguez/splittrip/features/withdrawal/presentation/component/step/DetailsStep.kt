package es.pedrazamiguez.splittrip.features.withdrawal.presentation.component.step

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.SectionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.wizard.WizardStepLayout
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState

/**
 * Step DETAILS: Title, notes, and ATM fee opt-in toggle.
 *
 * Title and notes are optional free-text fields. The ATM fee toggle is the gateway
 * that makes the ATM_FEE (and optionally FEE_EXCHANGE_RATE) steps appear after this
 * one. Scope is handled by the preceding [ScopeStep].
 */
@Composable
fun DetailsStep(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    onImeNext: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    WizardStepLayout(modifier = modifier) {
        AtmFeeToggleCard(uiState = uiState, onEvent = onEvent)
        TitleNotesCard(uiState = uiState, onEvent = onEvent, onImeNext = onImeNext)
    }
}

@Composable
private fun TitleNotesCard(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit,
    onImeNext: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)) {
        Text(
            text = stringResource(R.string.withdrawal_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        StyledOutlinedTextField(
            value = uiState.title,
            onValueChange = { onEvent(AddCashWithdrawalUiEvent.TitleChanged(it)) },
            label = stringResource(R.string.withdrawal_details_title_hint),
            modifier = Modifier.fillMaxWidth(),
            capitalization = KeyboardCapitalization.Sentences,
            singleLine = true,
            imeAction = ImeAction.Next
        )
        StyledOutlinedTextField(
            value = uiState.notes,
            onValueChange = { onEvent(AddCashWithdrawalUiEvent.NotesChanged(it)) },
            label = stringResource(R.string.withdrawal_details_notes_hint),
            modifier = Modifier.fillMaxWidth(),
            capitalization = KeyboardCapitalization.Sentences,
            singleLine = false,
            maxLines = 3,
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

@Composable
private fun AtmFeeToggleCard(
    uiState: AddCashWithdrawalUiState,
    onEvent: (AddCashWithdrawalUiEvent) -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.withdrawal_fee_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.withdrawal_fee_toggle_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = uiState.hasFee,
                onCheckedChange = { onEvent(AddCashWithdrawalUiEvent.FeeToggled(it)) }
            )
        }
    }
}
