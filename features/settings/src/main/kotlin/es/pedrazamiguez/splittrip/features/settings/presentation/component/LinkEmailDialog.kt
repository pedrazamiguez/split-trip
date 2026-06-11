package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState

@Composable
internal fun LinkEmailDialog(
    modifier: Modifier = Modifier,
    uiState: AccountStatusUiState,
    onEvent: (AccountStatusUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(AccountStatusUiEvent.DismissLinkEmailDialog) },
        title = {
            Text(
                stringResource(R.string.account_status_link_email_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                StyledOutlinedTextField(
                    label = stringResource(R.string.account_status_link_email_dialog_password),
                    value = uiState.linkPasswordInput,
                    onValueChange = { onEvent(AccountStatusUiEvent.LinkPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !uiState.isLinking,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                StyledOutlinedTextField(
                    label = stringResource(R.string.account_status_link_email_dialog_confirm_password),
                    value = uiState.linkConfirmPasswordInput,
                    onValueChange = { onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !uiState.isLinking,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.linkPasswordError != null) {
                    FormErrorBanner(error = uiState.linkPasswordError)
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.account_status_link_email_dialog_confirm),
                onClick = { onEvent(AccountStatusUiEvent.SubmitLinkEmailPassword) },
                enabled = !uiState.isLinking,
                isLoading = uiState.isLinking
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.account_status_link_email_dialog_cancel),
                onClick = { onEvent(AccountStatusUiEvent.DismissLinkEmailDialog) },
                enabled = !uiState.isLinking
            )
        },
        modifier = modifier
    )
}
