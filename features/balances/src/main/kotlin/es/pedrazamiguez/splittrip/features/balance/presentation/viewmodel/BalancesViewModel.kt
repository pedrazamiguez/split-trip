package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesDashboardFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SettlementsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceCashContext
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.BalancesUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.BalancesActivityEventHandler
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesActivitySelectionState
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@Suppress("LongParameterList")
class BalancesViewModel(
    private val getBalancesDashboardFlowUseCase: GetBalancesDashboardFlowUseCase,
    private val getLastSeenBalanceUseCase: GetLastSeenBalanceUseCase,
    private val setLastSeenBalanceUseCase: SetLastSeenBalanceUseCase,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val authenticationService: AuthenticationService,
    private val balancesUiMapper: BalancesUiMapper,
    private val settlementsUiMapper: SettlementsUiMapper,
    private val activityEventHandler: BalancesActivityEventHandler,
    private val appConfigService: AppConfigService,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _lastSeenBalance = MutableStateFlow<String?>(null)
    private val _lastSeenBalanceCents = MutableStateFlow<Long?>(null)
    private var _currentBalanceCents: Long = 0L

    // Actions for one-shot events like error messages
    private val _actions = MutableSharedFlow<BalancesUiAction>()
    val actions: SharedFlow<BalancesUiAction> = _actions.asSharedFlow()

    // Mutable selection state for delete overlays — separate from the Room-derived data flow
    private val _activitySelection = MutableStateFlow(BalancesActivitySelectionState())

    init {
        activityEventHandler.bind(_activitySelection, _actions, viewModelScope)
    }

    val uiState: StateFlow<BalancesUiState> = combine(
        _selectedGroupId
            .filterNotNull()
            .flatMapLatest { groupId ->
                val groupFlow = observeGroupUseCase(groupId)
                val group = getGroupByIdUseCase(groupId)
                val currency = group?.currency ?: appConfigService.defaultCurrencyCode.value
                val groupName = group?.name ?: ""
                val currentUserId = authenticationService.currentUserId()
                val groupMemberIds = group?.members ?: emptyList()

                // Seed the in-memory cache from DataStore once per group switch
                _lastSeenBalance.value = getLastSeenBalanceUseCase(groupId).first()

                combine(
                    getBalancesDashboardFlowUseCase(groupId, currency, groupMemberIds),
                    _lastSeenBalance,
                    groupFlow
                ) { domainModel, lastSeen, reactiveGroup ->
                    val isArchived = reactiveGroup?.status == GroupStatus.ARCHIVED
                    val balance = domainModel.balance
                    val contributions = domainModel.contributions
                    val withdrawals = domainModel.withdrawals
                    val subunits = domainModel.subunits
                    val expenses = domainModel.expenses
                    val memberBalances = domainModel.memberBalances
                    val memberProfiles = domainModel.memberProfiles
                    val settlementSuggestions = domainModel.settlementSuggestions

                    // Build subunit lookup map for mapper use
                    val subunitsMap = subunits.associateBy { it.id }
                    val mappedSettlements = settlementsUiMapper.mapSettlements(
                        settlements = settlementSuggestions,
                        currency = currency,
                        currentUserId = currentUserId ?: "",
                        memberProfiles = memberProfiles
                    )

                    val currentGroupName = reactiveGroup?.name?.takeIf { it.isNotBlank() } ?: groupName
                    val mappedBalance = balancesUiMapper.mapBalance(balance, currentGroupName)
                    val formattedBalance = mappedBalance.formattedBalance
                    val currentCents = balance.virtualBalance
                    val previousCents = _lastSeenBalanceCents.value

                    // Track current cents so handleBalanceAnimationComplete can snapshot it
                    _currentBalanceCents = currentCents

                    BalancesUiState(
                        isLoading = false,
                        groupId = groupId,
                        isGroupArchived = isArchived,
                        pocketBalance = mappedBalance,
                        contributions = balancesUiMapper.mapContributions(
                            contributions = contributions,
                            currentUserId = currentUserId,
                            memberProfiles = memberProfiles,
                            subunits = subunitsMap,
                            groupMemberIds = groupMemberIds,
                            groupCurrency = currency
                        ),
                        cashWithdrawals = balancesUiMapper.mapCashWithdrawals(
                            withdrawals = withdrawals,
                            groupCurrency = currency,
                            currentUserId = currentUserId,
                            memberProfiles = memberProfiles,
                            subunits = subunitsMap,
                            groupMemberIds = groupMemberIds
                        ),
                        memberBalances = balancesUiMapper.mapMemberBalances(
                            balances = memberBalances,
                            currency = currency,
                            currentUserId = currentUserId,
                            memberProfiles = memberProfiles,
                            groupCurrency = currency,
                            cashContext = MemberBalanceCashContext(
                                withdrawals = withdrawals,
                                subunitsMap = subunitsMap,
                                groupMemberIds = groupMemberIds
                            ),
                            groupMemberIds = groupMemberIds
                        ),
                        activityItems = balancesUiMapper.mapActivity(
                            contributions = contributions,
                            withdrawals = withdrawals,
                            groupCurrency = currency,
                            currentUserId = currentUserId,
                            memberProfiles = memberProfiles,
                            subunits = subunitsMap,
                            groupMemberIds = groupMemberIds
                        ),
                        extrasBreakdown = balancesUiMapper.mapExtrasBreakdown(
                            expenses = expenses,
                            withdrawals = withdrawals,
                            groupCurrency = currency,
                            memberProfiles = memberProfiles,
                            subunitsMap = subunitsMap,
                            currentUserId = currentUserId
                        ),
                        shouldAnimateBalance = formattedBalance.isNotBlank() &&
                            formattedBalance != lastSeen,
                        previousBalance = lastSeen ?: "",
                        balanceRollingUp = previousCents == null || currentCents >= previousCents,
                        settlements = mappedSettlements
                    )
                }
                    .catch { e ->
                        Timber.e(e, "Error loading balances for group $groupId")
                        viewModelScope.launch {
                            _actions.emit(
                                BalancesUiAction.ShowLoadError(
                                    UiText.StringResource(R.string.balances_error_loading)
                                )
                            )
                        }
                        emit(
                            BalancesUiState(
                                isLoading = false,
                                groupId = groupId,
                                isGroupArchived = false
                            )
                        )
                    }
                    // Move computeMemberBalances() + mapper calls off the main thread.
                    // This combine chain is CPU-bound (BigDecimal math, list iterations)
                    // and should not block the UI dispatcher.
                    .flowOn(computationDispatcher)
            },
        _activitySelection
    ) { dataState, selection ->
        dataState.copy(
            contributionActionsTarget = selection.contributionActionsTarget,
            withdrawalToDelete = selection.withdrawalToDelete
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = BalancesUiState(isLoading = true)
        )

    fun setSelectedGroup(groupId: String?) {
        if (groupId != _selectedGroupId.value) {
            _selectedGroupId.value = groupId
        }
    }

    fun onEvent(event: BalancesUiEvent) {
        when (event) {
            BalancesUiEvent.BalanceAnimationComplete -> handleBalanceAnimationComplete()

            is BalancesUiEvent.ContributionActionsRequested ->
                activityEventHandler.handleContributionActionsRequested(event.contribution)

            BalancesUiEvent.ContributionActionsDismissed ->
                activityEventHandler.handleContributionActionsDismissed()

            is BalancesUiEvent.DeleteContributionConfirmed -> {
                val groupId = _selectedGroupId.value ?: return
                activityEventHandler.handleDeleteContributionConfirmed(groupId, event.contributionId)
            }

            is BalancesUiEvent.DeleteWithdrawalRequested ->
                activityEventHandler.handleDeleteWithdrawalRequested(event.withdrawal)

            BalancesUiEvent.DeleteWithdrawalDismissed ->
                activityEventHandler.handleDeleteWithdrawalDismissed()

            is BalancesUiEvent.DeleteWithdrawalConfirmed -> {
                val groupId = _selectedGroupId.value ?: return
                activityEventHandler.handleDeleteWithdrawalConfirmed(groupId, event.withdrawalId)
            }
        }
    }

    private fun handleBalanceAnimationComplete() {
        val groupId = _selectedGroupId.value ?: return
        val formattedBalance = uiState.value.pocketBalance.formattedBalance
        if (formattedBalance.isNotBlank()) {
            // Update in-memory immediately → combine re-emits with shouldAnimateBalance = false
            _lastSeenBalance.value = formattedBalance
            _lastSeenBalanceCents.value = _currentBalanceCents
            viewModelScope.launch {
                setLastSeenBalanceUseCase(groupId, formattedBalance)
            }
        }
    }
}
