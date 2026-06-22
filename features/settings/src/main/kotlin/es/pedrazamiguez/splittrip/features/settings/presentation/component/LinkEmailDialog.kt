package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Composable
internal fun LinkEmailDialog(
    modifier: Modifier = Modifier,
    uiState: AccountStatusUiState,
    onEvent: (AccountStatusUiEvent) -> Unit
) {
    val isLinking = uiState.isLinking

    AlertDialog(
        onDismissRequest = { onEvent(AccountStatusUiEvent.DismissLinkEmailDialog) },
        title = {
            Text(
                text = stringResource(R.string.account_status_link_email_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LinkEmailDialogFields(
                uiState = uiState,
                onEvent = onEvent
            )
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.account_status_link_email_dialog_confirm),
                onClick = { onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword) },
                enabled = !isLinking,
                isLoading = isLinking
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.account_status_link_email_dialog_cancel),
                onClick = { onEvent(AccountStatusUiEvent.DismissLinkEmailDialog) },
                enabled = !isLinking
            )
        },
        modifier = modifier
    )
}
