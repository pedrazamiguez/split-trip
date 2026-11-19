package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state

data class DisputeSettlementUiState(
    val reason: String = "",
    val isSubmitting: Boolean = false,
    val isOffline: Boolean = false
) {
    val isSubmitEnabled: Boolean
        get() = reason.isNotBlank() && !isSubmitting && !isOffline
}
