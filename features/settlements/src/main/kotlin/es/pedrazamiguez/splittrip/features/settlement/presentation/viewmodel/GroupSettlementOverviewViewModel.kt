package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.mapper.GroupSettlementOverviewUiMapper
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.archive.ArchiveWizardStep
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.GroupSettlementOverviewUiAction
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.GroupSettlementOverviewUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.GroupSettlementOverviewUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
class GroupSettlementOverviewViewModel(
    private val getGroupSettlementsFlowUseCase: GetGroupSettlementsFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val groupSettlementOverviewUiMapper: GroupSettlementOverviewUiMapper,
    private val authenticationService: AuthenticationService,
    private val confirmSettlementUseCase: ConfirmSettlementUseCase,
    private val disputeSettlementUseCase: DisputeSettlementUseCase,
    private val archiveGroupUseCase: ArchiveGroupUseCase,
    private val getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase,
    private val telemetryTracker: TelemetryTracker
) : ViewModel() {

    private val _groupId = MutableStateFlow("")

    private val _localState = MutableStateFlow(
        GroupSettlementOverviewUiState()
    )

    private val _actions = Channel<GroupSettlementOverviewUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<GroupSettlementOverviewUiState> = _groupId
        .filter { it.isNotBlank() }
        .flatMapLatest { groupId ->
            val domainState = combine(
                getGroupSettlementsFlowUseCase(groupId),
                observeGroupUseCase(groupId)
            ) { settlements, group ->
                val currentUserId = authenticationService.requireUserId()
                val memberProfiles = if (group != null && group.members.isNotEmpty()) {
                    try {
                        getMemberProfilesUseCase(group.members)
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to fetch member profiles for group $groupId")
                        emptyMap()
                    }
                } else {
                    emptyMap()
                }
                groupSettlementOverviewUiMapper.toUiState(
                    settlements = settlements,
                    memberProfiles = memberProfiles,
                    currentUserId = currentUserId,
                    groupCreatorId = group?.createdBy ?: "",
                    groupName = group?.name ?: ""
                )
            }

            combine(
                domainState,
                _localState
            ) { baseState, localState ->
                // Clamp currentStep to a valid step in the live activeSteps list.
                // If another user resolves settlements in real-time, activeSteps shrinks
                // (e.g. ACTION_REQUIRED is dropped from the list). Keeping the stale
                // currentStep causes indexOf() to return -1, making both Back and Next
                // handlers no-op silently. We advance to the last step (Confirmation)
                // so the creator can proceed to archive immediately.
                val clampedStep = if (baseState.activeSteps.contains(localState.currentStep)) {
                    localState.currentStep
                } else {
                    baseState.activeSteps.lastOrNull() ?: localState.currentStep
                }
                baseState.copy(
                    activeDisputeSettlementId = localState.activeDisputeSettlementId,
                    disputeReasonInput = localState.disputeReasonInput,
                    isArchiving = localState.isArchiving,
                    currentStep = clampedStep
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = GroupSettlementOverviewUiState()
        )

    fun setGroupId(groupId: String) {
        if (groupId != _groupId.value) {
            _groupId.value = groupId
            viewModelScope.launch {
                try {
                    getSettlementSuggestionsUseCase.persistForGroup(groupId)
                    telemetryTracker.trackEvent("settlement_initiated", mapOf("group_id" to groupId))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to persist settlement suggestions for group $groupId")
                }
            }
        }
    }

    fun onEvent(event: GroupSettlementOverviewUiEvent) {
        when (event) {
            is GroupSettlementOverviewUiEvent.ConfirmSettlement -> handleConfirm(event.settlementId)
            is GroupSettlementOverviewUiEvent.DisputeSettlement -> handleOpenDispute(event.settlementId)
            is GroupSettlementOverviewUiEvent.DisputeReasonChanged -> _localState.update {
                it.copy(disputeReasonInput = event.reason)
            }
            GroupSettlementOverviewUiEvent.DisputeSubmitted -> handleSubmitDispute()
            GroupSettlementOverviewUiEvent.DisputeCancelled -> handleCancelDispute()
            GroupSettlementOverviewUiEvent.CloseTripClicked -> handleArchive()
            GroupSettlementOverviewUiEvent.WizardNextClicked -> handleWizardNext()
            GroupSettlementOverviewUiEvent.WizardBackClicked -> handleWizardBack()
            GroupSettlementOverviewUiEvent.WizardCancelled -> handleWizardCancelled()
            is GroupSettlementOverviewUiEvent.WizardJumpToStep -> handleJumpToStep(event.step)
            GroupSettlementOverviewUiEvent.NavigateToYourBalanceClicked -> {
                viewModelScope.launch {
                    _actions.send(GroupSettlementOverviewUiAction.NavigateToYourBalance)
                }
            }
        }
    }

    private fun handleWizardNext() {
        val state = uiState.value
        val currentIndex = state.activeSteps.indexOf(state.currentStep)
        if (currentIndex in 0 until state.activeSteps.lastIndex) {
            val nextStep = state.activeSteps[currentIndex + 1]
            _localState.update {
                it.copy(
                    currentStep = nextStep
                )
            }
        }
    }

    private fun handleWizardBack() {
        val state = uiState.value
        val currentIndex = state.activeSteps.indexOf(state.currentStep)
        if (currentIndex > 0) {
            val prevStep = state.activeSteps[currentIndex - 1]
            _localState.update {
                it.copy(
                    currentStep = prevStep
                )
            }
        } else if (currentIndex == 0) {
            handleWizardCancelled()
        }
    }

    private fun handleWizardCancelled() {
        viewModelScope.launch {
            _actions.send(GroupSettlementOverviewUiAction.NavigateBack)
        }
    }

    private fun handleJumpToStep(step: ArchiveWizardStep) {
        val state = uiState.value
        if (state.activeSteps.contains(step)) {
            _localState.update {
                it.copy(
                    currentStep = step
                )
            }
        }
    }

    private fun handleConfirm(settlementIdString: String) {
        val groupId = _groupId.value
        val settlementIds = settlementIdString.split(",")

        viewModelScope.launch {
            var hasError = false
            for (id in settlementIds) {
                confirmSettlementUseCase(groupId, id).fold(
                    onSuccess = {
                        telemetryTracker.trackEvent(
                            "settlement_confirmed",
                            mapOf("group_id" to groupId, "settlement_id" to id)
                        )
                    },
                    onFailure = { e ->
                        Timber.w(e, "Failed to confirm settlement $id")
                        hasError = true
                    }
                )
            }

            if (hasError) {
                _actions.send(
                    GroupSettlementOverviewUiAction.ShowError(
                        UiText.StringResource(R.string.settlement_overview_error_confirm)
                    )
                )
            } else {
                _actions.send(
                    GroupSettlementOverviewUiAction.ShowSuccess(
                        UiText.StringResource(R.string.settlement_overview_confirm_success)
                    )
                )
            }
        }
    }

    private fun handleOpenDispute(settlementId: String) {
        _localState.update {
            it.copy(
                activeDisputeSettlementId = settlementId,
                disputeReasonInput = ""
            )
        }
    }

    private fun handleSubmitDispute() {
        val settlementId = _localState.value.activeDisputeSettlementId ?: return
        val reason = _localState.value.disputeReasonInput.trim()
        if (reason.isBlank()) return

        viewModelScope.launch {
            disputeSettlementUseCase(_groupId.value, settlementId, reason).fold(
                onSuccess = {
                    _localState.update {
                        it.copy(
                            activeDisputeSettlementId = null,
                            disputeReasonInput = ""
                        )
                    }
                    _actions.send(
                        GroupSettlementOverviewUiAction.ShowSuccess(
                            UiText.StringResource(R.string.settlement_overview_dispute_success)
                        )
                    )
                },
                onFailure = { e ->
                    Timber.w(e, "Failed to dispute settlement $settlementId")
                    _actions.send(
                        GroupSettlementOverviewUiAction.ShowError(
                            UiText.StringResource(R.string.settlement_overview_error_dispute)
                        )
                    )
                }
            )
        }
    }

    private fun handleCancelDispute() {
        _localState.update {
            it.copy(
                activeDisputeSettlementId = null,
                disputeReasonInput = ""
            )
        }
    }

    private fun handleArchive() {
        _localState.update { it.copy(isArchiving = true) }
        viewModelScope.launch {
            archiveGroupUseCase(_groupId.value).fold(
                onSuccess = {
                    _localState.update { it.copy(isArchiving = false) }
                    _actions.send(GroupSettlementOverviewUiAction.NavigateBack)
                },
                onFailure = { e ->
                    _localState.update { it.copy(isArchiving = false) }
                    Timber.w(e, "Failed to archive group ${_groupId.value} from settlement overview")
                    _actions.send(
                        GroupSettlementOverviewUiAction.ShowError(
                            UiText.StringResource(R.string.settlement_overview_error_archive)
                        )
                    )
                }
            )
        }
    }
}
