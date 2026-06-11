package es.pedrazamiguez.splittrip.features.authentication.presentation.component

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
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

@Composable
internal fun CollisionMergeDialog(
    modifier: Modifier = Modifier,
    uiState: AuthenticationUiState,
    onEvent: (AuthenticationUiEvent) -> Unit
) {
    AlertDialog(
        onDismissRequest = { onEvent(AuthenticationUiEvent.DismissCollisionDialog) },
        title = {
            Text(
                text = stringResource(R.string.login_collision_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.login_collision_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StyledOutlinedTextField(
                    label = stringResource(R.string.login_collision_dialog_password_label),
                    value = uiState.collisionPassword,
                    onValueChange = { onEvent(AuthenticationUiEvent.CollisionPasswordChanged(it)) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    enabled = !uiState.isMerging,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
                if (uiState.mergeError != null) {
                    FormErrorBanner(error = uiState.mergeError)
                }
            }
        },
        confirmButton = {
            GradientButton(
                text = stringResource(R.string.login_collision_dialog_submit),
                onClick = { onEvent(AuthenticationUiEvent.SubmitCollisionMerge) },
                enabled = !uiState.isMerging,
                isLoading = uiState.isMerging
            )
        },
        dismissButton = {
            SecondaryButton(
                text = stringResource(R.string.login_collision_dialog_cancel),
                onClick = { onEvent(AuthenticationUiEvent.DismissCollisionDialog) },
                enabled = !uiState.isMerging
            )
        },
        modifier = modifier
    )
}
