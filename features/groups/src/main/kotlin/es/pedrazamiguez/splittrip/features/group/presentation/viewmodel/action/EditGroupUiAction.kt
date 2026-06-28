package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface EditGroupUiAction {
    data class ShowNotification(val message: UiText) : EditGroupUiAction
    object NavigateBack : EditGroupUiAction
}
