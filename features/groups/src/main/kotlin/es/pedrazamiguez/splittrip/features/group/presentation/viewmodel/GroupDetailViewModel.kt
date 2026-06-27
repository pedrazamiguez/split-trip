package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * ViewModel for the Group Detail screen.
 *
 * Loads group info once via [GetGroupByIdUseCase] and reactively observes the
 * subunit count via [GetGroupSubunitsFlowUseCase]. Follows the same
 * `_groupId`-gated `flatMapLatest` + `stateIn` pattern as [SubunitManagementViewModel].
 *
 * Group selection is handled in [GroupDetailFeature] via [SharedViewModel] — this
 * ViewModel is intentionally unaware of the active-group concept.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModel(
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper
) : ViewModel() {

    private val _groupId = MutableStateFlow("")

    private val _actions = Channel<GroupDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<GroupDetailUiState> = _groupId
        .filter { it.isNotBlank() }
        .flatMapLatest { groupId ->
            val group = try {
                getGroupByIdUseCase(groupId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load group: $groupId")
                null
            }

            val memberProfiles = if (group != null && group.members.isNotEmpty()) {
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

            if (group == null) {
                return@flatMapLatest flowOf(GroupDetailUiState(isLoading = false, hasError = true))
            }

            val groupUiModel = groupUiMapper.toGroupUiModel(group, memberProfiles)

            combine(
                getGroupSubunitsFlowUseCase(groupId),
                getUserGroupsFlowUseCase()
            ) { subunits, userGroups ->
                GroupDetailUiState(
                    group = groupUiModel,
                    isLoading = false,
                    subunitsCount = subunits.size,
                    isOnlyGroup = userGroups.size == 1
                )
            }
                .catch { e ->
                    Timber.e(e, "Error loading subunits or groups for group $groupId")
                    emit(GroupDetailUiState(group = groupUiModel, isLoading = false))
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

    @Suppress("UNUSED_PARAMETER")
    fun onEvent(event: GroupDetailUiEvent) {
        // No events handled at this time; included for MVI triad completeness.
    }
}
