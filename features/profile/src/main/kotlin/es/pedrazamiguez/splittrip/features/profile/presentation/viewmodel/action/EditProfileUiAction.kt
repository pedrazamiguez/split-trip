package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle

sealed interface EditProfileUiAction {
    data object NavigateBack : EditProfileUiAction
    data class ShowNotification(
        val message: UiText,
        val style: TopPillStyle = TopPillStyle.NEUTRAL
    ) : EditProfileUiAction
}
