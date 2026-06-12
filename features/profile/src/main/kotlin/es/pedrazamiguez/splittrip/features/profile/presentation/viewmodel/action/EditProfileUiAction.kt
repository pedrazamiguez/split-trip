package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface EditProfileUiAction {
    data object NavigateBack : EditProfileUiAction
    data class ShowNotification(val message: UiText) : EditProfileUiAction
}
