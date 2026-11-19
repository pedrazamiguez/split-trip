package es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.contribution.R
import es.pedrazamiguez.splittrip.features.contribution.presentation.mapper.ContributionDetailUiMapper
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.ContributionDetailUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.ContributionDetailUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.ContributionDetailUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

private data class ContributionContext(
    val groupId: String,
    val contributionId: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class ContributionDetailViewModel(
    private val getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val getGroupSubunitsUseCase: GetGroupSubunitsUseCase,
    private val deleteContributionUseCase: DeleteContributionUseCase,
    private val authenticationService: AuthenticationService,
    private val contributionDetailUiMapper: ContributionDetailUiMapper
) : ViewModel() {

    private val _context = MutableStateFlow<ContributionContext?>(null)

    private val _actions = Channel<ContributionDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<ContributionDetailUiState> = _context
        .filterNotNull()
        .filter { it.groupId.isNotBlank() && it.contributionId.isNotBlank() }
        .flatMapLatest { (groupId, contributionId) ->
            var cachedUserIds = emptySet<String>()
            var cachedProfiles = emptyMap<String, User>()
            var cachedSubunits = emptyMap<String, Subunit>()
            var cachedGroupSubunitId = ""

            getGroupContributionsFlowUseCase(groupId)
                .flatMapLatest { contributions ->
                    val contribution = contributions.find { it.id == contributionId }
                    if (contribution == null) {
                        return@flatMapLatest flowOf(
                            ContributionDetailUiState(isLoading = false, hasError = true)
                        )
                    }

                    val groupFlow = observeGroupUseCase(groupId)

                    val allUserIds = buildSet {
                        if (contribution.userId.isNotBlank()) add(contribution.userId)
                        if (contribution.createdBy.isNotBlank()) add(contribution.createdBy)
                    }

                    if (allUserIds != cachedUserIds) {
                        cachedUserIds = allUserIds
                        cachedProfiles = try {
                            getMemberProfilesUseCase(allUserIds.toList())
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch member profiles for contribution $contributionId")
                            emptyMap()
                        }
                    }

                    if (cachedGroupSubunitId != groupId) {
                        cachedGroupSubunitId = groupId
                        cachedSubunits = try {
                            getGroupSubunitsUseCase(groupId).associateBy { it.id }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to fetch subunits for contribution $contributionId")
                            emptyMap()
                        }
                    }

                    val currentUserId = authenticationService.currentUserId()

                    groupFlow.map { group ->
                        val groupMemberIds = group?.members ?: emptyList()
                        val groupCurrency = group?.currency ?: contribution.currency
                        val isGroupArchived = group?.status == GroupStatus.ARCHIVED

                        val uiModel = contributionDetailUiMapper.map(
                            contribution = contribution,
                            groupCurrency = groupCurrency,
                            memberProfiles = cachedProfiles,
                            subunitsMap = cachedSubunits,
                            groupMemberIds = groupMemberIds,
                            currentUserId = currentUserId
                        )

                        ContributionDetailUiState(
                            contribution = uiModel,
                            isLoading = false,
                            hasError = false,
                            isGroupArchived = isGroupArchived
                        )
                    }
                }
        }
        .catch { e ->
            Timber.e(e, "Fatal error in ContributionDetailViewModel flow")
            _actions.send(
                ContributionDetailUiAction.ShowError(
                    UiText.StringResource(R.string.contribution_detail_error_loading)
                )
            )
            emit(ContributionDetailUiState(isLoading = false, hasError = true))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = ContributionDetailUiState()
        )

    fun setContext(groupId: String, contributionId: String) {
        if (_context.value?.groupId != groupId || _context.value?.contributionId != contributionId) {
            _context.value = ContributionContext(groupId, contributionId)
        }
    }

    fun onEvent(event: ContributionDetailUiEvent) {
        when (event) {
            ContributionDetailUiEvent.DeleteConfirmed -> handleDelete()
            is ContributionDetailUiEvent.LinkedExpenseClicked -> handleLinkedExpenseClick(event.expenseId)
        }
    }

    private fun handleLinkedExpenseClick(expenseId: String) {
        if (expenseId.isBlank()) return
        viewModelScope.launch {
            _actions.send(ContributionDetailUiAction.NavigateToExpenseDetail(expenseId))
        }
    }

    private fun handleDelete() {
        val contribution = uiState.value.contribution ?: return
        viewModelScope.launch {
            try {
                deleteContributionUseCase(contribution.groupId, contribution.id)
                _actions.send(
                    ContributionDetailUiAction.DeleteSuccess(
                        UiText.StringResource(R.string.contribution_detail_deleted_successfully)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete contribution: ${contribution.id}")
                _actions.send(
                    ContributionDetailUiAction.ShowError(
                        UiText.StringResource(R.string.contribution_detail_delete_error)
                    )
                )
            }
        }
    }
}
