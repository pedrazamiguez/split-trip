package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event

sealed interface GroupsUiEvent {
    data object LoadGroups : GroupsUiEvent
    data class ScrollPositionChanged(val index: Int, val offset: Int) : GroupsUiEvent
    data class DeleteGroup(val groupId: String) : GroupsUiEvent
    data class ArchiveGroup(val groupId: String) : GroupsUiEvent
}
