package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface SpendingInsightsUiAction {
    data object NavigateBack : SpendingInsightsUiAction
    data class ShowLoadError(val message: UiText) : SpendingInsightsUiAction
}
