package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupDetailUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Group Detail screen.
 *
 * Reactively observes group info via [ObserveGroupUseCase] and subunit count via
 * [GetGroupSubunitsFlowUseCase]. Follows the same `_groupId`-gated `flatMapLatest` +
 * `stateIn` pattern.
 *
 * Group selection is handled in [GroupDetailFeature] via [SharedViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModel(
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper,
    private val authenticationService: AuthenticationService,
    private val archiveGroupUseCase: ArchiveGroupUseCase
) : ViewModel() {

    private val _groupId = MutableStateFlow("")

    private val _localUiState = MutableStateFlow(LocalUiState())

    private val _actions = Channel<GroupDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<GroupDetailUiState> = _groupId
        .filter { it.isNotBlank() }
        .flatMapLatest { groupId ->
            observeGroupUseCase(groupId)
                .distinctUntilChanged()
                .flatMapLatest { group ->
                    if (group == null) {
                        return@flatMapLatest flowOf(GroupDetailUiState(isLoading = false, hasError = true))
                    }

                    val memberProfiles = if (group.members.isNotEmpty()) {
                        try {
                            getMemberProfilesUseCase(group.members)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch member profiles for group $groupId")
                            emptyMap()
                        }
                    } else {
                        emptyMap()
                    }

                    val groupUiModel = groupUiMapper.toGroupUiModel(group, memberProfiles)

                    combine(
                        getGroupSubunitsFlowUseCase(groupId).distinctUntilChanged(),
                        getUserGroupsFlowUseCase().distinctUntilChanged(),
                        _localUiState
                    ) { subunits, userGroups, localState ->
                        val currentUserId = authenticationService.requireUserId()
                        GroupDetailUiState(
                            group = groupUiModel,
                            isLoading = false,
                            subunitsCount = subunits.size,
                            isOnlyGroup = userGroups.size == 1,
                            showArchiveConfirmation = localState.showArchiveConfirmation,
                            isUserAdmin = group.createdBy == currentUserId,
                            isArchiving = localState.isArchiving
                        )
                    }
                        .catch { e ->
                            Timber.e(e, "Error loading subunits or groups for group $groupId")
                            emit(GroupDetailUiState(group = groupUiModel, isLoading = false))
                        }
                }
        }
        .catch { e ->
            Timber.e(e, "Fatal error in GroupDetailViewModel flow")
            _actions.send(
                GroupDetailUiAction.ShowError(
                    UiText.StringResource(R.string.group_detail_error_loading)
                )
            )
            emit(GroupDetailUiState(isLoading = false, hasError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = GroupDetailUiState()
        )

    fun setGroupId(groupId: String) {
        if (groupId != _groupId.value) {
            _groupId.value = groupId
        }
    }

    fun onEvent(event: GroupDetailUiEvent) {
        when (event) {
            GroupDetailUiEvent.ArchiveClicked -> {
                _localUiState.update { it.copy(showArchiveConfirmation = true) }
            }
            GroupDetailUiEvent.ArchiveCancelled -> {
                _localUiState.update { it.copy(showArchiveConfirmation = false) }
            }
            GroupDetailUiEvent.ArchiveConfirmed -> {
                _localUiState.update { it.copy(showArchiveConfirmation = false, isArchiving = true) }
                viewModelScope.launch {
                    archiveGroupUseCase(_groupId.value).fold(
                        onSuccess = {
                            _localUiState.update { it.copy(isArchiving = false) }
                        },
                        onFailure = { _ ->
                            _localUiState.update { it.copy(isArchiving = false) }
                            _actions.send(
                                GroupDetailUiAction.ShowError(
                                    UiText.StringResource(DesignSystemR.string.group_error_archiving_failed)
                                )
                            )
                        }
                    )
                }
            }
        }
    }

    private data class LocalUiState(
        val showArchiveConfirmation: Boolean = false,
        val isArchiving: Boolean = false
    )
}
