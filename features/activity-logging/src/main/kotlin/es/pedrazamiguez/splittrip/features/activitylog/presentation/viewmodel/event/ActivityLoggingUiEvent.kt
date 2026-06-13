package es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.event

sealed interface ActivityLoggingUiEvent {
    object Refresh : ActivityLoggingUiEvent
}
