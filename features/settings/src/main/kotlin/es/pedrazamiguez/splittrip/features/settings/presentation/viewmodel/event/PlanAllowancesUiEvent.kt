package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event

sealed interface PlanAllowancesUiEvent {
    data object RequestUpgrade : PlanAllowancesUiEvent
}
