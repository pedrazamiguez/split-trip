package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper
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
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class BalancesViewModel(
    private val useCases: BalancesUseCases,
    private val authenticationService: AuthenticationService,
    private val balancesUiMapper: BalancesUiMapper,
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
                val groupFlow = useCases.observeGroupUseCase(groupId)
                val group = useCases.getGroupByIdUseCase(groupId)
                val currency = group?.currency ?: appConfigService.defaultCurrencyCode.value
                val groupName = group?.name ?: ""
                val currentUserId = authenticationService.currentUserId()
                val groupMemberIds = group?.members ?: emptyList()

                // Seed the in-memory cache from DataStore once per group switch
                _lastSeenBalance.value = useCases.getLastSeenBalanceUseCase(groupId).first()

                // Nested combine: inner combines 5 data flows into DataSnapshot (debounced
                // to absorb rapid Firestore reconciliation bursts), outer pairs with
                // lastSeenBalance for balance animation logic.
                combine(
                    combine(
                        useCases.getGroupPocketBalanceFlowUseCase(groupId, currency),
                        useCases.getGroupContributionsFlowUseCase(groupId),
                        useCases.getCashWithdrawalsFlowUseCase(groupId),
                        useCases.getGroupSubunitsFlowUseCase(groupId),
                        useCases.getGroupExpensesFlowUseCase(groupId)
                    ) { balance, contributions, withdrawals, subunits, expenses ->
                        DataSnapshot(balance, contributions, withdrawals, subunits, expenses)
                    }
                        // Debounce absorbs rapid multi-table writes (e.g. Firestore reconciliation
                        // that updates expenses + splits + withdrawals in quick succession) so that
                        // the CPU-bound computeMemberBalances() runs once per logical batch rather
                        // than once per individual table write.
                        .debounce { appConfigService.balanceComputationDebounceMs.value },
                    _lastSeenBalance,
                    groupFlow
                ) { snapshot, lastSeen, reactiveGroup ->
                    val isArchived = reactiveGroup?.status == GroupStatus.ARCHIVED
                    val balance = snapshot.balance
                    val contributions = snapshot.contributions
                    val withdrawals = snapshot.withdrawals
                    val subunits = snapshot.subunits
                    val expenses = snapshot.expenses

                    // Compute member balances from already-loaded data (pure computation)
                    val memberBalances = useCases.getMemberBalancesFlowUseCase.computeMemberBalances(
                        contributions = contributions,
                        withdrawals = withdrawals,
                        expenses = expenses,
                        subunits = subunits,
                        groupMemberIds = groupMemberIds,
                        groupCurrency = currency
                    )

                    // Build subunit lookup map for mapper use
                    val subunitsMap = subunits.associateBy { it.id }

                    // Collect ALL unique user IDs from the data being displayed,
                    // not just group.members — contributions/withdrawals may reference
                    // users not yet in the group members list (e.g. manually-added data).
                    val allUserIds = buildSet {
                        addAll(groupMemberIds)
                        contributions.forEach { add(it.userId) }
                        withdrawals.forEach { add(it.withdrawnBy) }
                        memberBalances.forEach { add(it.userId) }
                    }.toList()
                    val memberProfiles = useCases.getMemberProfilesUseCase(allUserIds)

                    val mappedBalance = balancesUiMapper.mapBalance(balance, groupName)
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
                            contributions,
                            currentUserId,
                            memberProfiles,
                            subunitsMap
                        ),
                        cashWithdrawals = balancesUiMapper.mapCashWithdrawals(
                            withdrawals,
                            currency,
                            currentUserId,
                            memberProfiles,
                            subunitsMap
                        ),
                        memberBalances = balancesUiMapper.mapMemberBalances(
                            memberBalances,
                            currency,
                            currentUserId,
                            memberProfiles,
                            groupCurrency = currency,
                            cashContext = MemberBalanceCashContext(
                                withdrawals = withdrawals,
                                subunitsMap = subunitsMap,
                                groupMemberIds = groupMemberIds
                            )
                        ),
                        activityItems = balancesUiMapper.mapActivity(
                            contributions,
                            withdrawals,
                            currency,
                            currentUserId,
                            memberProfiles,
                            subunitsMap
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
                        balanceRollingUp = previousCents == null || currentCents >= previousCents
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
            contributionToDelete = selection.contributionToDelete,
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

            is BalancesUiEvent.DeleteContributionRequested ->
                activityEventHandler.handleDeleteContributionRequested(event.contribution)

            BalancesUiEvent.DeleteContributionDismissed ->
                activityEventHandler.handleDeleteContributionDismissed()

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
            // Persist to DataStore for next app launch
            viewModelScope.launch {
                useCases.setLastSeenBalanceUseCase(groupId, formattedBalance)
            }
        }
    }

    private data class DataSnapshot(
        val balance: GroupPocketBalance,
        val contributions: List<Contribution>,
        val withdrawals: List<CashWithdrawal>,
        val subunits: List<Subunit>,
        val expenses: List<Expense>
    )
}
