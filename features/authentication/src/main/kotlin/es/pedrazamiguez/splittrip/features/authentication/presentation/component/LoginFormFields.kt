package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState

@Composable
internal fun LoginFormFields(
    modifier: Modifier = Modifier,
    uiState: AuthenticationUiState,
    anyLoading: Boolean,
    onEvent: (AuthenticationUiEvent) -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default),
        modifier = modifier.fillMaxWidth()
    ) {
        StyledOutlinedTextField(
            label = stringResource(R.string.login_email_label),
            value = uiState.email,
            onValueChange = { onEvent(AuthenticationUiEvent.EmailChanged(it)) },
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            modifier = Modifier.fillMaxWidth()
        )
        StyledOutlinedTextField(
            label = stringResource(R.string.login_password_label),
            value = uiState.password,
            onValueChange = { onEvent(AuthenticationUiEvent.PasswordChanged(it)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = !anyLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = stringResource(R.string.login_forgot_password),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = !anyLoading,
                        role = Role.Button,
                        onClick = onForgotPasswordClick
                    )
                    .padding(
                        horizontal = MaterialTheme.spacing.Small,
                        vertical = MaterialTheme.spacing.ExtraSmall
                    )
            )
        }
    }
}
