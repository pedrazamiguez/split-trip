package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupCurrencyRateUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.model.leave.LeaveWizardUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class GroupDetailUiState(
    val group: GroupUiModel? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val subunitsCount: Int = 0,
    val isOnlyGroup: Boolean = false,
    val isUserAdmin: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val isLeaving: Boolean = false,
    val leaveWizardState: LeaveWizardUiState = LeaveWizardUiState(),
    val currencyRates: ImmutableList<GroupCurrencyRateUiModel> = persistentListOf()
)
