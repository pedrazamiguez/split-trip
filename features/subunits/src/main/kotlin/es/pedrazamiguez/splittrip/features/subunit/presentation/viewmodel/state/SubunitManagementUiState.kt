package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.subunit.presentation.model.SubunitUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class SubunitManagementUiState(
    val isLoading: Boolean = true,
    val groupId: String = "",
    val groupName: String = "",
    val subunits: ImmutableList<SubunitUiModel> = persistentListOf(),
    val isGroupArchived: Boolean = false
)
