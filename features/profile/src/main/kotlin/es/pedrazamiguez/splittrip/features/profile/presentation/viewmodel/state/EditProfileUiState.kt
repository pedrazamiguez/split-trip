package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

data class EditProfileUiState(
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String? = null,
    val localAvatarPath: String? = null,
    val localAvatarMimeType: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val showCropOverlay: Boolean = false,
    val cropSourceUri: String? = null,
    val displayNameError: UiText? = null,
    val bioError: UiText? = null
)
