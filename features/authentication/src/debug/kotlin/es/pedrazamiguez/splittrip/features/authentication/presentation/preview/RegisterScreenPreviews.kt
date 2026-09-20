package es.pedrazamiguez.splittrip.features.authentication.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.RegisterScreen

@PreviewComplete
@Composable
fun RegisterScreenDefaultPreview() {
    PreviewThemeWrapper {
        RegisterScreen(
            uiState = RegisterUiState(
                email = "newuser@example.com",
                displayName = "New User",
                password = "password123",
                confirmPassword = "password123"
            )
        )
    }
}

@PreviewComplete
@Composable
fun RegisterScreenLoadingPreview() {
    PreviewThemeWrapper {
        RegisterScreen(
            uiState = RegisterUiState(
                email = "newuser@example.com",
                displayName = "New User",
                password = "password123",
                confirmPassword = "password123",
                isLoading = true
            )
        )
    }
}
