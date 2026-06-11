package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface ProfileUiAction {
    data class ShowError(val message: UiText) : ProfileUiAction
    data class ShowSuccess(val message: UiText) : ProfileUiAction
}
