package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface CreateEditGroupUiAction {
    data object NavigateBack : CreateEditGroupUiAction
    data class ShowSuccess(val message: UiText) : CreateEditGroupUiAction
    data class ShowError(val message: UiText) : CreateEditGroupUiAction
    data object RequestExitConfirmation : CreateEditGroupUiAction
    data class NavigateToRoute(val route: String) : CreateEditGroupUiAction
    data object NavigateToSubscriptions : CreateEditGroupUiAction
}
