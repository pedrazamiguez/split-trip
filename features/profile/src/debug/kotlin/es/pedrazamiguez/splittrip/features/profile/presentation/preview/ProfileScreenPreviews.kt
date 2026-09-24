package es.pedrazamiguez.splittrip.features.profile.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.ProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState

@PreviewComplete
@Composable
private fun ProfileScreenLoadingPreview() {
    PreviewThemeWrapper {
        ProfileScreen(
            uiState = ProfileUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun ProfileScreenWithDataPreview() {
    ProfileUiPreviewHelper(domainUser = PREVIEW_USER) { profileUiModel ->
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = profileUiModel
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ProfileScreenProPreview() {
    ProfileUiPreviewHelper(domainUser = PREVIEW_USER_PRO) { profileUiModel ->
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = profileUiModel
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ProfileScreenNoDisplayNamePreview() {
    ProfileUiPreviewHelper(domainUser = PREVIEW_USER_NO_NAME) { profileUiModel ->
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = profileUiModel
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ProfileScreenGuestPreview() {
    PreviewThemeWrapper {
        ProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                isAnonymous = true
            )
        )
    }
}
