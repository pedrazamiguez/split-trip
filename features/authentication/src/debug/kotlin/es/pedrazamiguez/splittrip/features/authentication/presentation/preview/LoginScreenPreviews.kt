package es.pedrazamiguez.splittrip.features.authentication.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.LoginScreen

@PreviewComplete
@Composable
private fun LoginScreenPreview() {
    PreviewThemeWrapper {
        LoginScreen(
            uiState = AuthenticationUiState()
        )
    }
}

@PreviewComplete
@Composable
private fun LoginScreenExpandedEmailPreview() {
    PreviewThemeWrapper {
        LoginScreen(
            uiState = AuthenticationUiState(
                email = "user@example.com",
                password = "password123",
                isEmailFormExpanded = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun LoginScreenLoadingPreview() {
    PreviewThemeWrapper {
        LoginScreen(
            uiState = AuthenticationUiState(
                email = "user@example.com",
                password = "password123",
                isEmailFormExpanded = true,
                isLoading = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun LoginScreenGoogleLoadingPreview() {
    PreviewThemeWrapper {
        LoginScreen(
            uiState = AuthenticationUiState(
                isGoogleLoading = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun LoginScreenErrorPreview() {
    PreviewThemeWrapper {
        LoginScreen(
            uiState = AuthenticationUiState(
                email = "invalid@example.com",
                password = "wrong",
                isEmailFormExpanded = true,
                error = UiText.DynamicString("Invalid email or password")
            )
        )
    }
}
