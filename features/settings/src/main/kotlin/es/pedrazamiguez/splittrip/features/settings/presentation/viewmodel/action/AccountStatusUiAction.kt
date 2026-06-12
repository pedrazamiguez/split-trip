package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface AccountStatusUiAction {
    data class ShowError(val message: UiText) : AccountStatusUiAction
    data class ShowSuccess(val message: UiText) : AccountStatusUiAction
    data object NavigateBack : AccountStatusUiAction
}
