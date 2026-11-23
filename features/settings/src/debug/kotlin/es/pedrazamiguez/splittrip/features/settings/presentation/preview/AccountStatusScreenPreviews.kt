package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.AccountStatusScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun AccountStatusScreenPreview() {
    PreviewThemeWrapper {
        AccountStatusScreen(
            uiState = AccountStatusUiState(
                isLoading = false,
                email = "user@example.com",
                isAnonymous = false,
                joinDateText = "August 2026",
                linkedProviders = persistentListOf(
                    AuthProviderType.EMAIL_PASSWORD,
                    AuthProviderType.GOOGLE
                )
            ),
            onLinkGoogleClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun AccountStatusScreenPartialLinkedPreview() {
    PreviewThemeWrapper {
        AccountStatusScreen(
            uiState = AccountStatusUiState(
                isLoading = false,
                email = "user@example.com",
                isAnonymous = false,
                joinDateText = "August 2026",
                linkedProviders = persistentListOf(
                    AuthProviderType.GOOGLE
                )
            ),
            onLinkGoogleClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun AccountStatusScreenGuestPreview() {
    PreviewThemeWrapper {
        AccountStatusScreen(
            uiState = AccountStatusUiState(
                isLoading = false,
                isAnonymous = true,
                linkedProviders = persistentListOf()
            ),
            onLinkGoogleClick = {},
            onEvent = {}
        )
    }
}
