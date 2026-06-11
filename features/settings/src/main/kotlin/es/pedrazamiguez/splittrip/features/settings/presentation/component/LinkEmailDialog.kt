package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
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
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
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
    val isLinking = uiState.isLinking
    val hasError = uiState.linkPasswordError != null
    val isMismatch = uiState.linkPasswordError ==
        UiText.StringResource(R.string.account_status_link_email_dialog_error_mismatch)

    AlertDialog(
        onDismissRequest = { onEvent(AccountStatusUiEvent.DismissLinkEmailDialog) },
        title = {
            Text(
                text = stringResource(R.string.account_status_link_email_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)) {
                StyledOutlinedTextField(
                    label = stringResource(R.string.account_status_link_email_dialog_password),
                    value = uiState.linkPasswordInput,
                    onValueChange = { onEvent(AccountStatusUiEvent.LinkPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLinking,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Next,
                    isError = hasError,
                    supportingText = if (hasError && !isMismatch) uiState.linkPasswordError?.asString() else null,
                    modifier = Modifier.fillMaxWidth()
                )
                StyledOutlinedTextField(
                    label = stringResource(R.string.account_status_link_email_dialog_confirm_password),
                    value = uiState.linkConfirmPasswordInput,
                    onValueChange = { onEvent(AccountStatusUiEvent.LinkConfirmPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isLinking,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                    isError = hasError,
                    supportingText = if (hasError && isMismatch) uiState.linkPasswordError?.asString() else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
