package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.CropRect

sealed interface EditProfileUiEvent {
    data class OnDisplayNameChanged(val name: String) : EditProfileUiEvent
    data class OnBioChanged(val bio: String) : EditProfileUiEvent
    data class OnAvatarPicked(val uri: String, val mimeType: String) : EditProfileUiEvent
    data class OnCropConfirmed(val cropRect: CropRect) : EditProfileUiEvent
    data object OnAvatarRemoved : EditProfileUiEvent
    data object OnCropCancelled : EditProfileUiEvent
    data object OnSaveClicked : EditProfileUiEvent
}
