package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GroupsUiState(
    val groups: ImmutableList<GroupUiModel> = persistentListOf(),
    val isLoading: Boolean = true,
    val scrollPosition: Int = 0,
    val scrollOffset: Int = 0,
    val isAnonymous: Boolean = false
)
