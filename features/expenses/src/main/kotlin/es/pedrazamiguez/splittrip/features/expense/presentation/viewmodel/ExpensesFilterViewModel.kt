package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpensesFilterUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesFilterUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesFilterUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesFilterUiState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesFilterViewModel(
    private val getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase,
    private val expenseFilterService: ExpenseFilterService,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val authenticationService: AuthenticationService,
    private val expensesFilterUiMapper: ExpensesFilterUiMapper,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _draftCriteria = MutableStateFlow(ExpenseFilterCriteria())
    private var isInitialized = false

    private val _actions = MutableSharedFlow<ExpensesFilterUiAction>()
    val actions: SharedFlow<ExpensesFilterUiAction> = _actions.asSharedFlow()

    val uiState: StateFlow<ExpensesFilterUiState> = _selectedGroupId
        .filterNotNull()
        .flatMapLatest { groupId ->
            combine(
                observeGroupUseCase(groupId),
                getGroupExpensesFlowUseCase(groupId),
                _draftCriteria
            ) { group, expenses, draft ->
                val totalExpensesCount = expenses.size
                val matchingExpenses = expenseFilterService.filter(expenses, draft)
                val currentUserId = authenticationService.currentUserId()

                val groupMemberIds = group?.members ?: emptyList()
                val allUserIds = buildSet {
                    addAll(groupMemberIds)
                    expenses.forEach { expense ->
                        add(expense.createdBy)
                        expense.payerId?.let { add(it) }
                        expense.splits.forEach { split ->
                            add(split.userId)
                        }
                    }
                }.toList()

                val memberProfiles = getMemberProfilesUseCase(allUserIds)
                val availableMembers = expensesFilterUiMapper.mapAvailableMembers(
                    allUserIds = allUserIds,
                    memberProfiles = memberProfiles,
                    currentUserId = currentUserId
                )

                val groupCurrency = group?.currency ?: "EUR"
                val groupCurrencies = if (group != null && group.extraCurrencies.isNotEmpty()) {
                    (listOf(group.currency) + group.extraCurrencies).distinct().toImmutableList()
                } else {
                    persistentListOf()
                }

                val (oldestDate, newestDate) = expensesFilterUiMapper.extractDateBounds(expenses)
                val formattedStartDate = expensesFilterUiMapper.formatFilterDate(draft.startDate)
                val formattedEndDate = expensesFilterUiMapper.formatFilterDate(draft.endDate)
                val activePreset = expensesFilterUiMapper.findMatchingPreset(draft.startDate, draft.endDate)

                ExpensesFilterUiState(
                    draftCriteria = draft,
                    availableMembers = availableMembers,
                    matchingExpensesCount = matchingExpenses.size,
                    totalExpensesCount = totalExpensesCount,
                    isLoading = false,
                    groupId = groupId,
                    oldestExpenseDate = oldestDate,
                    newestExpenseDate = newestDate,
                    formattedStartDate = formattedStartDate,
                    formattedEndDate = formattedEndDate,
                    activePreset = activePreset,
                    groupCurrency = groupCurrency,
                    groupCurrencies = groupCurrencies
                )
            }
        }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = ExpensesFilterUiState(isLoading = true)
        )

    fun onEvent(event: ExpensesFilterUiEvent) {
        when (event) {
            is ExpensesFilterUiEvent.Initialize -> {
                if (!isInitialized) {
                    _draftCriteria.value = event.initialCriteria
                    isInitialized = true
                }
            }

            is ExpensesFilterUiEvent.UpdateDraft -> {
                _draftCriteria.value = event.criteria
            }

            is ExpensesFilterUiEvent.DatePresetSelected -> {
                val currentActive = expensesFilterUiMapper.findMatchingPreset(
                    _draftCriteria.value.startDate,
                    _draftCriteria.value.endDate
                )
                if (currentActive == event.preset) {
                    _draftCriteria.value = _draftCriteria.value.copy(startDate = null, endDate = null)
                } else {
                    val (start, end) = expensesFilterUiMapper.calculatePresetRange(event.preset)
                    _draftCriteria.value = _draftCriteria.value.copy(startDate = start, endDate = end)
                }
            }

            ExpensesFilterUiEvent.ResetDraft -> {
                val cleared = _draftCriteria.value.clearNonSearchFilters()
                _draftCriteria.value = cleared
                viewModelScope.launch {
                    _actions.emit(ExpensesFilterUiAction.FiltersReset(cleared))
                }
            }

            ExpensesFilterUiEvent.ApplyFilters -> {
                viewModelScope.launch {
                    _actions.emit(
                        ExpensesFilterUiAction.ApplyAndNavigateBack(_draftCriteria.value)
                    )
                }
            }
        }
    }

    fun setSelectedGroup(groupId: String?) {
        if (groupId != _selectedGroupId.value) {
            _selectedGroupId.value = groupId
        }
    }
}
