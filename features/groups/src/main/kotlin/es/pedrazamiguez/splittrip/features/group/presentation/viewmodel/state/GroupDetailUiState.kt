package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

data class GroupDetailUiState(
    val group: GroupUiModel? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val subunitsCount: Int = 0,
    val isOnlyGroup: Boolean = false,
    val showArchiveConfirmation: Boolean = false,
    val isUserAdmin: Boolean = false,
    val isArchiving: Boolean = false
)
