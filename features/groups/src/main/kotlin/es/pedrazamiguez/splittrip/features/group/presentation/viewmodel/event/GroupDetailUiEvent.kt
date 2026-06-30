package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

sealed interface GroupDetailUiEvent {
    data object ArchiveClicked : GroupDetailUiEvent
    data object ArchiveConfirmed : GroupDetailUiEvent
    data object ArchiveCancelled : GroupDetailUiEvent
    data object DeleteClicked : GroupDetailUiEvent
    data object DeleteConfirmed : GroupDetailUiEvent
    data object DeleteCancelled : GroupDetailUiEvent
    data object LeaveClicked : GroupDetailUiEvent
    data object LeaveConfirmed : GroupDetailUiEvent
    data object LeaveCancelled : GroupDetailUiEvent
}
