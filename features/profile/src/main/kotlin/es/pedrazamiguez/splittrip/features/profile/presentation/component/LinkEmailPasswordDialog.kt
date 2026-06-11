package es.pedrazamiguez.splittrip.features.profile.presentation.component

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
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState

@Composable
internal fun LinkEmailPasswordDialog(
    modifier: Modifier = Modifier,
    uiState: ProfileUiState,
    onEvent: (ProfileUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(ProfileUiEvent.DismissLinkEmailDialog) },
        title = {
            Text(stringResource(R.string.profile_link_email_dialog_title), style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.profile_link_email_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StyledOutlinedTextField(
                    label = stringResource(R.string.profile_link_email_dialog_password),
                    value = uiState.linkPasswordInput,
                    onValueChange = { onEvent(ProfileUiEvent.LinkPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !uiState.isLinking,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )
                StyledOutlinedTextField(
                    label = stringResource(R.string.profile_link_email_dialog_confirm_password),
                    value = uiState.linkConfirmPasswordInput,
                    onValueChange = { onEvent(ProfileUiEvent.LinkConfirmPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !uiState.isLinking,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.linkPasswordError != null) {
                    FormErrorBanner(error = uiState.linkPasswordError)
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.profile_link_email_dialog_submit),
                onClick = { onEvent(ProfileUiEvent.SubmitLinkEmailPassword) },
                enabled = !uiState.isLinking,
                isLoading = uiState.isLinking
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.profile_link_email_dialog_cancel),
                onClick = { onEvent(ProfileUiEvent.DismissLinkEmailDialog) },
                enabled = !uiState.isLinking
            )
        },
        modifier = modifier
    )
}
