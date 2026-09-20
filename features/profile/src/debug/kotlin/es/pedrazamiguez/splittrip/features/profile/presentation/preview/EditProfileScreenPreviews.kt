package es.pedrazamiguez.splittrip.features.profile.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.EditProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.EditProfileUiState

@PreviewComplete
@Composable
fun EditProfileScreenDefaultPreview() {
    PreviewThemeWrapper {
        EditProfileScreen(
            uiState = EditProfileUiState(
                displayName = "Antonio García",
                bio = "Travel enthusiast and developer.",
                avatarUrl = null,
                isLoading = false
            ),
            onEvent = {},
            onAvatarClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun EditProfileScreenLoadingPreview() {
    PreviewThemeWrapper {
        EditProfileScreen(
            uiState = EditProfileUiState(
                isLoading = true
            ),
            onEvent = {},
            onAvatarClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun EditProfileScreenSavingPreview() {
    PreviewThemeWrapper {
        EditProfileScreen(
            uiState = EditProfileUiState(
                displayName = "Antonio García",
                bio = "Travel enthusiast and developer.",
                avatarUrl = null,
                isSaving = true
            ),
            onEvent = {},
            onAvatarClick = {}
        )
    }
}

@PreviewComplete
@Composable
fun EditProfileScreenErrorsPreview() {
    PreviewThemeWrapper {
        EditProfileScreen(
            uiState = EditProfileUiState(
                displayName = "",
                bio = "A".repeat(300),
                displayNameError = UiText.DynamicString("Display name cannot be empty"),
                bioError = UiText.DynamicString("Bio cannot exceed 200 characters")
            ),
            onEvent = {},
            onAvatarClick = {}
        )
    }
}
