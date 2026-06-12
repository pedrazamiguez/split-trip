package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState

@Composable
internal fun RegisterFormFields(
    modifier: Modifier = Modifier,
    uiState: RegisterUiState,
    anyLoading: Boolean,
    onEvent: (RegisterUiEvent) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        modifier = modifier.fillMaxWidth()
    ) {
        StyledOutlinedTextField(
            label = stringResource(R.string.register_display_name_label),
            value = uiState.displayName,
            onValueChange = { onEvent(RegisterUiEvent.DisplayNameChanged(it)) },
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        StyledOutlinedTextField(
            label = stringResource(R.string.login_email_label),
            value = uiState.email,
            onValueChange = { onEvent(RegisterUiEvent.EmailChanged(it)) },
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        StyledOutlinedTextField(
            label = stringResource(R.string.login_password_label),
            value = uiState.password,
            onValueChange = { onEvent(RegisterUiEvent.PasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        StyledOutlinedTextField(
            label = stringResource(R.string.register_confirm_password_label),
            value = uiState.confirmPassword,
            onValueChange = { onEvent(RegisterUiEvent.ConfirmPasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
