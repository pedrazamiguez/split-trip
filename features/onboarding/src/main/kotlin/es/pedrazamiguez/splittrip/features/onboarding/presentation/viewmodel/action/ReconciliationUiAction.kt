package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface ReconciliationUiAction {
    object NavigationToNext : ReconciliationUiAction
    data class ShowError(val message: UiText) : ReconciliationUiAction
}
