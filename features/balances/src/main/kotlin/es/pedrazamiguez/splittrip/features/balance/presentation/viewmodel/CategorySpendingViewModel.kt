package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.CategorySpendingUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.CategorySpendingUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.CategorySpendingUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.CategorySpendingUiState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class CategorySpendingViewModel(
    private val getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase,
    private val getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase,
    private val observeGroupUseCase: ObserveGroupUseCase,
    private val appConfigService: AppConfigService,
    private val categorySpendingUiMapper: CategorySpendingUiMapper,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<String?>(null)

    private val _actions = MutableSharedFlow<CategorySpendingUiAction>()
    val actions: SharedFlow<CategorySpendingUiAction> = _actions.asSharedFlow()

    val uiState: StateFlow<CategorySpendingUiState> = _selectedGroupId
        .filterNotNull()
        .flatMapLatest { groupId ->
            combine(
                observeGroupUseCase(groupId),
                getGroupExpensesFlowUseCase(groupId),
                getCashWithdrawalsFlowUseCase(groupId)
            ) { group, expenses, withdrawals ->
                val currency = group?.currency ?: appConfigService.defaultCurrencyCode.value
                val mappedItems = categorySpendingUiMapper.mapExpenses(
                    expenses = expenses,
                    groupCurrency = currency,
                    withdrawals = withdrawals
                )

                val totalCents = categorySpendingUiMapper.calculateTotalCents(
                    expenses = expenses,
                    withdrawals = withdrawals
                )
                val formattedTotal = categorySpendingUiMapper.formatTotalAmount(totalCents, currency)

                CategorySpendingUiState(
                    isLoading = false,
                    items = mappedItems,
                    totalFormattedAmount = formattedTotal
                )
            }.catch { e ->
                Timber.e(e, "Error loading category spending for group $groupId")
                emit(CategorySpendingUiState(isLoading = false))
            }.flowOn(computationDispatcher)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = CategorySpendingUiState(isLoading = true)
        )

    fun setSelectedGroup(groupId: String?) {
        if (groupId != _selectedGroupId.value) {
            _selectedGroupId.value = groupId
        }
    }

    fun onEvent(event: CategorySpendingUiEvent) {
        when (event) {
            CategorySpendingUiEvent.OnNavigateBack -> {
                viewModelScope.launch {
                    _actions.emit(CategorySpendingUiAction.NavigateBack)
                }
            }
        }
    }
}
