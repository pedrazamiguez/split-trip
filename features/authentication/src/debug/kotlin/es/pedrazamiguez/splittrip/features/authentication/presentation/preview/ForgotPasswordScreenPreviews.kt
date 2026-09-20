package es.pedrazamiguez.splittrip.features.authentication.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.ForgotPasswordScreen

@PreviewComplete
@Composable
fun ForgotPasswordScreenDefaultPreview() {
    PreviewThemeWrapper {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(
                email = "user@example.com"
            )
        )
    }
}

@PreviewComplete
@Composable
fun ForgotPasswordScreenLoadingPreview() {
    PreviewThemeWrapper {
        ForgotPasswordScreen(
            uiState = ForgotPasswordUiState(
                email = "user@example.com",
                isLoading = true
            )
        )
    }
}
