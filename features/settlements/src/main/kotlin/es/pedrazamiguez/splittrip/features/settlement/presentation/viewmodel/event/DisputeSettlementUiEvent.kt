package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event

sealed interface DisputeSettlementUiEvent {
    data class ReasonChanged(val reason: String) : DisputeSettlementUiEvent
    data object SubmitClicked : DisputeSettlementUiEvent
    data object NavigateBackClicked : DisputeSettlementUiEvent
}
