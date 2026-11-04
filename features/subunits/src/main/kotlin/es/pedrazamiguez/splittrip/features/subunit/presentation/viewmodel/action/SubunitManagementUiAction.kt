package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface SubunitManagementUiAction {
    data class ShowSuccess(val message: UiText) : SubunitManagementUiAction
    data class ShowError(val message: UiText) : SubunitManagementUiAction
    data class NavigateToCreateSubunit(val groupId: String) : SubunitManagementUiAction
    data class NavigateToEditSubunit(val groupId: String, val subunitId: String) : SubunitManagementUiAction
    data object NavigateToSubscriptions : SubunitManagementUiAction
}
