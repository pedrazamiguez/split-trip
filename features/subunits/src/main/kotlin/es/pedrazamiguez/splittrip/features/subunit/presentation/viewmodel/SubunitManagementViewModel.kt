package es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.subunit.R
import es.pedrazamiguez.splittrip.features.subunit.presentation.mapper.SubunitUiMapper
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.SubunitManagementUiAction
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.SubunitManagementUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.SubunitManagementUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Subunit Management screen (list view).
 *
 * Observes subunits from the local database (Room) via hot flow.
 * Handles delete operations and navigates to create/edit screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubunitManagementViewModel(
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    private val deleteSubunitUseCase: DeleteSubunitUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val subunitUiMapper: SubunitUiMapper,
    private val observeGroupUseCase: ObserveGroupUseCase
) : ViewModel() {

    private val _groupId = MutableStateFlow("")

    private val _actions = MutableSharedFlow<SubunitManagementUiAction>()
    val actions: SharedFlow<SubunitManagementUiAction> = _actions.asSharedFlow()

    val uiState: StateFlow<SubunitManagementUiState> = _groupId
        .filter { it.isNotBlank() }
        .flatMapLatest { groupId ->
            val groupFlow = observeGroupUseCase(groupId)
            val group = getGroupByIdUseCase(groupId)
            val memberIds = group?.members ?: emptyList()
            val memberProfiles = getMemberProfilesUseCase(memberIds)
            val groupName = group?.name ?: ""

            combine(
                getGroupSubunitsFlowUseCase(groupId),
                groupFlow
            ) { subunits, reactiveGroup ->
                SubunitManagementUiState(
                    isLoading = false,
                    groupId = groupId,
                    groupName = groupName,
                    subunits = subunitUiMapper.toSubunitUiModelList(subunits, memberProfiles),
                    isGroupArchived = reactiveGroup?.status == GroupStatus.ARCHIVED
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = SubunitManagementUiState()
        )

    fun setGroupId(groupId: String) {
        if (groupId != _groupId.value) {
            _groupId.value = groupId
        }
    }

    fun onEvent(event: SubunitManagementUiEvent) {
        when (event) {
            SubunitManagementUiEvent.CreateSubunit -> navigateToCreate()
            is SubunitManagementUiEvent.EditSubunit -> navigateToEdit(event.subunitId)
            is SubunitManagementUiEvent.ConfirmDeleteSubunit -> handleDeleteSubunit(event.subunitId)
        }
    }

    private fun navigateToCreate() {
        viewModelScope.launch {
            _actions.emit(SubunitManagementUiAction.NavigateToCreateSubunit(_groupId.value))
        }
    }

    private fun navigateToEdit(subunitId: String) {
        viewModelScope.launch {
            _actions.emit(SubunitManagementUiAction.NavigateToEditSubunit(_groupId.value, subunitId))
        }
    }

    private fun handleDeleteSubunit(subunitId: String) {
        val groupId = _groupId.value
        viewModelScope.launch {
            try {
                deleteSubunitUseCase(groupId, subunitId)
                _actions.emit(
                    SubunitManagementUiAction.ShowSuccess(
                        UiText.StringResource(R.string.subunit_deleted_success)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete subunit: $subunitId")
                _actions.emit(
                    SubunitManagementUiAction.ShowError(
                        UiText.StringResource(R.string.subunit_error_delete_failed)
                    )
                )
            }
        }
    }
}
