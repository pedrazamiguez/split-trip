package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ExpensesViewModel(
    private val useCases: ExpensesUseCases,
    private val expenseUiMapper: ExpenseUiMapper,
    private val authenticationService: AuthenticationService,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val expenseFilterService: ExpenseFilterService,
    private val telemetryTracker: TelemetryTracker
) : ViewModel() {

    private val _scrollState = MutableStateFlow(Pair(0, 0))
    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _filterCriteria = MutableStateFlow(ExpenseFilterCriteria())

    // Actions for one-shot events like success/error messages
    private val _actions = MutableSharedFlow<ExpensesUiAction>()
    val actions: SharedFlow<ExpensesUiAction> = _actions.asSharedFlow()

    val uiState: StateFlow<ExpensesUiState> = _selectedGroupId
        .filterNotNull()
        .flatMapLatest { groupId ->
            val groupFlow = observeGroupUseCase(groupId)
            val currentUserId = authenticationService.currentUserId()

            val debouncedCriteriaFlow = _filterCriteria
                .debounce { criteria ->
                    if (criteria.searchQuery.isBlank()) 0L else SEARCH_DEBOUNCE_MS
                }

            // Merge: emit once immediately (Unit), plus on every explicit refresh
            merge(
                flowOf(Unit),
                _refreshTrigger
            ).flatMapLatest {
                combine(
                    useCases.getGroupExpensesFlowUseCase(groupId),
                    useCases.getGroupContributionsFlowUseCase(groupId),
                    useCases.getGroupSubunitsFlowUseCase(groupId),
                    groupFlow,
                    debouncedCriteriaFlow
                ) { expenses, contributions, subunits, group, debouncedCriteria ->
                    val totalExpensesCount = expenses.size
                    val filteredExpenses = expenseFilterService.filter(expenses, debouncedCriteria)
                    val isFiltered = debouncedCriteria.isActive
                    val visibleExpensesCount = filteredExpenses.size

                    val groupCurrency = group?.currency
                        ?: expenses.firstOrNull()?.groupCurrency
                        ?: "EUR"

                    val today = LocalDate.now()
                    val activeExpenses = filteredExpenses.filter { it.paymentStatus != PaymentStatus.CANCELLED }
                    val (futureScheduledExpenses, settledExpenses) = activeExpenses.partition {
                        it.paymentStatus == PaymentStatus.SCHEDULED &&
                            it.dueDate?.toLocalDate()?.isAfter(today) == true
                    }
                    val totalSpentCents = settledExpenses.sumOf { it.groupAmount }
                    val totalScheduledCents = futureScheduledExpenses.sumOf { it.groupAmount }

                    val formattedTotalSpent = expenseUiMapper.formatTotalSpent(totalSpentCents, groupCurrency)
                    val formattedTotalScheduled = if (totalScheduledCents > 0L) {
                        expenseUiMapper.formatScheduledAmount(totalScheduledCents, groupCurrency)
                    } else {
                        null
                    }

                    val isArchived = group?.status == GroupStatus.ARCHIVED
                    val groupMemberIds = group?.members ?: emptyList()

                    // Collect ALL unique user IDs: group members + expense creators + payers
                    val allUserIds = buildSet {
                        addAll(groupMemberIds)
                        filteredExpenses.forEach {
                            add(it.createdBy)
                            it.payerId?.let { payerId -> add(payerId) }
                        }
                    }.toList()
                    val memberProfiles = useCases.getMemberProfilesUseCase(allUserIds)

                    // Build lookup maps for scope-aware badge resolution
                    val pairedContributions = contributions
                        .filter { it.linkedExpenseId != null }
                        .associateBy { it.linkedExpenseId!! }
                    val subunitsById = subunits.associateBy { it.id }

                    val mappedGroups = expenseUiMapper.mapGroupedByDate(
                        expenses = filteredExpenses,
                        memberProfiles = memberProfiles,
                        currentUserId = currentUserId,
                        pairedContributions = pairedContributions,
                        subunits = subunitsById,
                        groupMemberIds = groupMemberIds
                    )
                    ExpenseCalculationResult(
                        mappedGroups = mappedGroups,
                        isArchived = isArchived,
                        totalExpensesCount = totalExpensesCount,
                        formattedTotalSpent = formattedTotalSpent,
                        formattedTotalScheduled = formattedTotalScheduled,
                        visibleExpensesCount = visibleExpensesCount,
                        isFiltered = isFiltered
                    )
                }
                    .transformLatest { result ->
                        if (result.mappedGroups.any { it.expenses.isNotEmpty() } || result.totalExpensesCount > 0) {
                            emit(
                                UiStateUpdate.Success(
                                    data = result.mappedGroups,
                                    isGroupArchived = result.isArchived,
                                    totalExpensesCount = result.totalExpensesCount,
                                    formattedTotalSpent = result.formattedTotalSpent,
                                    formattedTotalScheduled = result.formattedTotalScheduled,
                                    visibleExpensesCount = result.visibleExpensesCount,
                                    isFiltered = result.isFiltered
                                )
                            )
                        } else {
                            // Grace period to avoid empty state flicker
                            emit(
                                UiStateUpdate.LoadingEmpty(
                                    isGroupArchived = result.isArchived,
                                    totalExpensesCount = result.totalExpensesCount,
                                    formattedTotalSpent = result.formattedTotalSpent,
                                    formattedTotalScheduled = result.formattedTotalScheduled,
                                    visibleExpensesCount = result.visibleExpensesCount,
                                    isFiltered = result.isFiltered
                                )
                            )
                            delay(EMPTY_STATE_GRACE_PERIOD_MS)
                            emit(
                                UiStateUpdate.Success(
                                    data = result.mappedGroups,
                                    isGroupArchived = result.isArchived,
                                    totalExpensesCount = result.totalExpensesCount,
                                    formattedTotalSpent = result.formattedTotalSpent,
                                    formattedTotalScheduled = result.formattedTotalScheduled,
                                    visibleExpensesCount = result.visibleExpensesCount,
                                    isFiltered = result.isFiltered
                                )
                            )
                        }
                    }
                    .catch { e ->
                        Timber.e(e, "Error loading expenses")
                        viewModelScope.launch {
                            _actions.emit(
                                ExpensesUiAction.ShowLoadError(
                                    UiText.StringResource(R.string.expenses_error_loading)
                                )
                            )
                        }
                        emit(UiStateUpdate.Error(isGroupArchived = false))
                    }
                    .map { update ->
                        when (update) {
                            is UiStateUpdate.LoadingEmpty -> ExpensesUiState(
                                isLoading = true,
                                groupId = groupId,
                                isGroupArchived = update.isGroupArchived,
                                totalExpensesCount = update.totalExpensesCount,
                                formattedTotalSpent = update.formattedTotalSpent,
                                formattedTotalScheduled = update.formattedTotalScheduled,
                                visibleExpensesCount = update.visibleExpensesCount,
                                isFiltered = update.isFiltered
                            )

                            is UiStateUpdate.Success -> ExpensesUiState(
                                expenseGroups = update.data,
                                isLoading = false,
                                groupId = groupId,
                                isGroupArchived = update.isGroupArchived,
                                totalExpensesCount = update.totalExpensesCount,
                                formattedTotalSpent = update.formattedTotalSpent,
                                formattedTotalScheduled = update.formattedTotalScheduled,
                                visibleExpensesCount = update.visibleExpensesCount,
                                isFiltered = update.isFiltered
                            )

                            is UiStateUpdate.Error -> ExpensesUiState(
                                isLoading = false,
                                groupId = groupId,
                                isGroupArchived = update.isGroupArchived
                            )
                        }
                    }
            }
        }
        .combineWithFilterAndScroll(_filterCriteria, _scrollState)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = ExpensesUiState(isLoading = true)
        )

    fun onEvent(event: ExpensesUiEvent) {
        when (event) {
            ExpensesUiEvent.LoadExpenses -> {
                viewModelScope.launch { _refreshTrigger.emit(Unit) }
            }

            is ExpensesUiEvent.ScrollPositionChanged -> {
                _scrollState.update { event.index to event.offset }
            }

            is ExpensesUiEvent.DeleteExpense -> handleDeleteExpense(event.expenseId)
            is ExpensesUiEvent.CancelExpense -> handleCancelExpense(event.expenseId)
            ExpensesUiEvent.ExpenseAdded -> {
                viewModelScope.launch { _actions.emit(ExpensesUiAction.ScrollToTop) }
            }

            is ExpensesUiEvent.SearchQueryChanged -> {
                _filterCriteria.update { it.copy(searchQuery = event.query) }
            }

            is ExpensesUiEvent.FilterCriteriaChanged -> {
                _filterCriteria.value = event.criteria
            }

            ExpensesUiEvent.ClearFilters -> {
                _filterCriteria.update { it.clearNonSearchFilters() }
            }
        }
    }

    fun setSelectedGroup(groupId: String?) {
        if (groupId != _selectedGroupId.value) {
            _filterCriteria.value = ExpenseFilterCriteria()
            _selectedGroupId.value = groupId
        }
    }

    private fun handleDeleteExpense(expenseId: String) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            try {
                useCases.deleteExpenseUseCase(groupId, expenseId)
                telemetryTracker.trackEvent(
                    "expense_deleted",
                    mapOf("expense_id" to expenseId, "group_id" to groupId)
                )
                _actions.emit(
                    ExpensesUiAction.ShowDeleteSuccess(
                        UiText.StringResource(R.string.expense_deleted_successfully)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete expense: $expenseId")
                _actions.emit(
                    ExpensesUiAction.ShowDeleteError(
                        UiText.StringResource(R.string.error_deleting_expense)
                    )
                )
            }
        }
    }

    private fun handleCancelExpense(expenseId: String) {
        val groupId = _selectedGroupId.value ?: return
        viewModelScope.launch {
            try {
                val domainExpense = useCases.getExpenseByIdFlowUseCase(expenseId).first()
                if (domainExpense == null) {
                    Timber.w("Expense not found for cancellation: $expenseId")
                    return@launch
                }
                val updatedExpense = domainExpense.copy(
                    paymentStatus = PaymentStatus.CANCELLED
                )
                useCases.updateExpenseUseCase(
                    groupId = groupId,
                    expense = updatedExpense,
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                ).onSuccess {
                    _actions.emit(
                        ExpensesUiAction.ShowCancelSuccess(
                            UiText.StringResource(R.string.expense_cancelled_successfully)
                        )
                    )
                }.onFailure { e ->
                    Timber.e(e, "Failed to cancel reservation expense: $expenseId")
                    _actions.emit(
                        ExpensesUiAction.ShowCancelError(
                            UiText.StringResource(R.string.error_cancelling_expense)
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling reservation expense: $expenseId")
                _actions.emit(
                    ExpensesUiAction.ShowCancelError(
                        UiText.StringResource(R.string.error_cancelling_expense)
                    )
                )
            }
        }
    }

    private data class ExpenseCalculationResult(
        val mappedGroups: ImmutableList<ExpenseDateGroupUiModel>,
        val isArchived: Boolean,
        val totalExpensesCount: Int,
        val formattedTotalSpent: String,
        val formattedTotalScheduled: String?,
        val visibleExpensesCount: Int,
        val isFiltered: Boolean
    )

    private sealed interface UiStateUpdate {
        val isGroupArchived: Boolean
        data class LoadingEmpty(
            override val isGroupArchived: Boolean,
            val totalExpensesCount: Int = 0,
            val formattedTotalSpent: String = "",
            val formattedTotalScheduled: String? = null,
            val visibleExpensesCount: Int = 0,
            val isFiltered: Boolean = false
        ) : UiStateUpdate

        data class Success(
            val data: ImmutableList<ExpenseDateGroupUiModel>,
            override val isGroupArchived: Boolean,
            val totalExpensesCount: Int = 0,
            val formattedTotalSpent: String = "",
            val formattedTotalScheduled: String? = null,
            val visibleExpensesCount: Int = 0,
            val isFiltered: Boolean = false
        ) : UiStateUpdate

        data class Error(override val isGroupArchived: Boolean) : UiStateUpdate
    }

    private fun Flow<ExpensesUiState>.combineWithFilterAndScroll(
        filterCriteriaFlow: StateFlow<ExpenseFilterCriteria>,
        scrollFlow: StateFlow<Pair<Int, Int>>
    ): Flow<ExpensesUiState> =
        combine(this, filterCriteriaFlow, scrollFlow) { state, criteria, scroll ->
            state.copy(
                searchQuery = criteria.searchQuery,
                filterCriteria = criteria,
                scrollPosition = scroll.first,
                scrollOffset = scroll.second
            )
        }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L

        // Grace period before showing the empty state.
        // On cold start, Room emits an empty list instantly while the cloud sync
        // runs in the background. transformLatest will cancel this delay the moment
        // Room emits non-empty data (after the sync upserts), so groups with data
        // are never delayed. Only genuinely empty groups wait the full duration.
        private const val EMPTY_STATE_GRACE_PERIOD_MS = 400L
    }
}
