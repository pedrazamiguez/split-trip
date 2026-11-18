package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface DisputeSettlementUiAction {
    data class ShowSuccess(val message: UiText) : DisputeSettlementUiAction
    data class ShowError(val message: UiText) : DisputeSettlementUiAction
    data object NavigateBack : DisputeSettlementUiAction
}
