package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface GroupDetailUiAction {
    data class ShowError(val message: UiText) : GroupDetailUiAction
    data class DeleteSuccess(val message: UiText) : GroupDetailUiAction
    data class LeaveSuccess(val message: UiText) : GroupDetailUiAction
}
