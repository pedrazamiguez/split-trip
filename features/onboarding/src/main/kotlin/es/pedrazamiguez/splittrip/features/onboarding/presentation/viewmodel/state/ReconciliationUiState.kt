package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state

sealed interface ReconciliationUiState {
    object WaitingForYou : ReconciliationUiState
    object Migrating : ReconciliationUiState
    object Success : ReconciliationUiState
}
