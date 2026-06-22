package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event

sealed interface ReconciliationUiEvent {
    object MigrateData : ReconciliationUiEvent
}
