package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.UnresolvedSettlementsException
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupDetailUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupDetailUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.GroupDetailViewModelLocalState
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.GroupLeaveWizardEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupDetailUiState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
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
 * [GetGroupSubunitsFlowUseCase]. Delegating leave wizard events to [GroupLeaveWizardEventHandler].
 */
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
class GroupDetailViewModel(
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper,
    private val authenticationService: AuthenticationService,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val getGroupSettlementsFlowUseCase: GetGroupSettlementsFlowUseCase,
    private val leaveWizardEventHandler: GroupLeaveWizardEventHandler,
    private val getExchangeRateUseCase: GetExchangeRateUseCase
) : ViewModel() {

    private val _groupId = MutableStateFlow("")

    private val _localUiState = MutableStateFlow(GroupDetailViewModelLocalState())

    private val _actions = Channel<GroupDetailUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        leaveWizardEventHandler.bind(
            scope = viewModelScope,
            onLeaveSuccess = { message -> _actions.send(GroupDetailUiAction.LeaveSuccess(message)) },
            onError = { message -> _actions.send(GroupDetailUiAction.ShowError(message)) }
        )
    }

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

                    val currencyRates = if (group.extraCurrencies.isNotEmpty()) {
                        coroutineScope {
                            group.extraCurrencies.map { extraCurrency ->
                                async {
                                    val rate = try {
                                        getExchangeRateUseCase(
                                            baseCurrencyCode = group.currency,
                                            targetCurrencyCode = extraCurrency
                                        )?.rate
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Timber.w(
                                            e,
                                            "Failed to resolve exchange rate for ${group.currency} -> $extraCurrency"
                                        )
                                        null
                                    }
                                    extraCurrency to rate
                                }
                            }.awaitAll().toMap()
                        }.let { ratesMap ->
                            groupUiMapper.mapCurrencyExchangeRates(group.currency, ratesMap)
                        }
                    } else {
                        persistentListOf()
                    }

                    combine(
                        getGroupSubunitsFlowUseCase(groupId).distinctUntilChanged(),
                        getUserGroupsFlowUseCase().distinctUntilChanged(),
                        getGroupSettlementsFlowUseCase(groupId).distinctUntilChanged(),
                        _localUiState,
                        leaveWizardEventHandler.wizardState
                    ) { subunits, userGroups, _, localState, wizardState ->
                        val currentUserId = authenticationService.requireUserId()

                        GroupDetailUiState(
                            group = groupUiModel,
                            isLoading = false,
                            subunitsCount = subunits.size,
                            isOnlyGroup = userGroups.size == 1,
                            isUserAdmin = group.createdBy == currentUserId,
                            showDeleteConfirmation = localState.showDeleteConfirmation,
                            isDeleting = localState.isDeleting,
                            isLeaving = wizardState.isLeaving,
                            leaveWizardState = wizardState,
                            currencyRates = currencyRates
                        )
                    }
                        .catch { e ->
                            Timber.e(e, "Error loading subunits or groups for group $groupId")
                            emit(
                                GroupDetailUiState(
                                    group = groupUiModel,
                                    isLoading = false,
                                    currencyRates = currencyRates
                                )
                            )
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
                viewModelScope.launch {
                    _actions.send(GroupDetailUiAction.NavigateToSettlementOverview(_groupId.value))
                }
            }
            GroupDetailUiEvent.DeleteClicked -> _localUiState.update { it.copy(showDeleteConfirmation = true) }
            GroupDetailUiEvent.DeleteCancelled -> _localUiState.update { it.copy(showDeleteConfirmation = false) }
            GroupDetailUiEvent.DeleteConfirmed -> handleDelete()
            GroupDetailUiEvent.LeaveClicked -> leaveWizardEventHandler.handleLeaveClicked(_groupId.value)
            GroupDetailUiEvent.LeaveCancelled,
            GroupDetailUiEvent.WizardCancelled -> leaveWizardEventHandler.handleWizardCancelled()
            GroupDetailUiEvent.LeaveConfirmed -> leaveWizardEventHandler.handleLeave(_groupId.value)
            GroupDetailUiEvent.WizardNextClicked -> leaveWizardEventHandler.handleWizardNext(_groupId.value)
            GroupDetailUiEvent.WizardBackClicked -> leaveWizardEventHandler.handleWizardBack()
            GroupDetailUiEvent.NavigateToYourBalanceClicked -> {
                leaveWizardEventHandler.handleWizardCancelled()
                viewModelScope.launch {
                    _actions.send(GroupDetailUiAction.NavigateToYourBalance)
                }
            }
            is GroupDetailUiEvent.WizardJumpToStepClicked ->
                leaveWizardEventHandler.handleJumpToStep(event.step)
        }
    }

    private fun handleDelete() {
        _localUiState.update { it.copy(showDeleteConfirmation = false, isDeleting = true) }
        viewModelScope.launch {
            try {
                deleteGroupUseCase(_groupId.value)
                _localUiState.update { it.copy(isDeleting = false) }
                _actions.send(
                    GroupDetailUiAction.DeleteSuccess(UiText.StringResource(R.string.group_deleted_successfully))
                )
            } catch (e: UnresolvedSettlementsException) {
                Timber.w(e, "Cannot delete group with unresolved settlements: ${_groupId.value}")
                _localUiState.update { it.copy(isDeleting = false) }
                _actions.send(
                    GroupDetailUiAction.ShowError(
                        UiText.StringResource(R.string.error_group_delete_unresolved_settlements)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete group: ${_groupId.value}")
                _localUiState.update { it.copy(isDeleting = false) }
                _actions.send(GroupDetailUiAction.ShowError(UiText.StringResource(R.string.error_deleting_group)))
            }
        }
    }
}
