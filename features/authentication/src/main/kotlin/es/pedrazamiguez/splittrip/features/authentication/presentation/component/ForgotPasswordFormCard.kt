package es.pedrazamiguez.splittrip.features.authentication.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.extension.asString
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiState

@Composable
internal fun ForgotPasswordFormCard(
    modifier: Modifier = Modifier,
    uiState: ForgotPasswordUiState,
    onEvent: (ForgotPasswordUiEvent) -> Unit
) {
    FlatCard(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StyledOutlinedTextField(
                label = stringResource(R.string.forgot_password_email_label),
                value = uiState.email,
                onValueChange = { onEvent(ForgotPasswordUiEvent.EmailChanged(it)) },
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth(),
                supportingText = uiState.emailError?.asString(),
                isError = uiState.emailError != null
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Large))

            GradientButton(
                text = stringResource(R.string.forgot_password_button),
                onClick = { onEvent(ForgotPasswordUiEvent.Submit) },
                enabled = !uiState.isLoading,
                isLoading = uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.generalError != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))
                FormErrorBanner(
                    error = uiState.generalError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
