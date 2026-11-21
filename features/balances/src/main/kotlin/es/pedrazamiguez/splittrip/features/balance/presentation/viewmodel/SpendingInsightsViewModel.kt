package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SpendingInsightsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.SpendingInsightsUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingInsightsViewModel(
    private val getGroupSpendingInsightsFlowUseCase: GetGroupSpendingInsightsFlowUseCase,
    private val spendingInsightsUiMapper: SpendingInsightsUiMapper,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private data class SpendingInsightsSelectionState(
        val preset: SpendingPeriodPreset = SpendingPeriodPreset.ALL_TIME,
        val customStartDate: LocalDate? = null,
        val customEndDate: LocalDate? = null,
        val entityBreakdownMode: EntityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
        val selectedDayBarId: String? = null
    )

    private val _selectedGroupId = MutableStateFlow<String?>(null)
    private val _selectionState = MutableStateFlow(SpendingInsightsSelectionState())
    private val _actions = MutableSharedFlow<SpendingInsightsUiAction>()
    val actions: SharedFlow<SpendingInsightsUiAction> = _actions.asSharedFlow()

    private var latestOldestExpenseDate: LocalDate? = null
    private var latestNewestExpenseDate: LocalDate? = null

    private val periodFlow: Flow<SpendingInsightsPeriod> = _selectionState
        .map(::resolvePeriod)
        .distinctUntilChanged()

    val uiState: StateFlow<SpendingInsightsUiState> = _selectedGroupId
        .filterNotNull()
        .flatMapLatest { groupId ->
            combine(
                getGroupSpendingInsightsFlowUseCase(
                    groupId = groupId,
                    periodFlow = periodFlow,
                    todayProvider = todayProvider
                ),
                _selectionState
            ) { insights, selection ->
                latestOldestExpenseDate = insights.oldestExpenseDate
                latestNewestExpenseDate = insights.newestExpenseDate

                spendingInsightsUiMapper.mapToUiState(
                    insights = insights,
                    selectedPreset = selection.preset,
                    customStartDate = selection.customStartDate,
                    customEndDate = selection.customEndDate,
                    entityBreakdownMode = selection.entityBreakdownMode,
                    selectedDayBarId = selection.selectedDayBarId,
                    referenceYear = todayProvider().year
                )
            }
                .catch { throwable ->
                    Timber.e(throwable, "Error loading spending insights for group $groupId")
                    _actions.emit(
                        SpendingInsightsUiAction.ShowLoadError(
                            UiText.StringResource(R.string.balances_insights_error_loading)
                        )
                    )
                    emit(
                        SpendingInsightsUiState(
                            isLoading = false,
                            groupId = groupId
                        )
                    )
                }
                .flowOn(computationDispatcher)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = SpendingInsightsUiState(isLoading = true)
        )

    fun setSelectedGroup(groupId: String?) {
        if (groupId != _selectedGroupId.value) {
            if (groupId != null) {
                _selectionState.value = SpendingInsightsSelectionState()
                latestOldestExpenseDate = null
                latestNewestExpenseDate = null
            }
            _selectedGroupId.value = groupId
        }
    }

    fun onEvent(event: SpendingInsightsUiEvent) {
        when (event) {
            is SpendingInsightsUiEvent.SelectPeriodPreset -> handleSelectPeriodPreset(event.preset)
            is SpendingInsightsUiEvent.SelectCustomStartDate -> handleSelectCustomStartDate(event.date)
            is SpendingInsightsUiEvent.SelectCustomEndDate -> handleSelectCustomEndDate(event.date)
            is SpendingInsightsUiEvent.SelectDayBar -> {
                _selectionState.update { it.copy(selectedDayBarId = event.barId) }
            }
            is SpendingInsightsUiEvent.SelectEntityBreakdownMode -> {
                _selectionState.update { it.copy(entityBreakdownMode = event.mode) }
            }
            SpendingInsightsUiEvent.NavigateBack -> {
                viewModelScope.launch {
                    _actions.emit(SpendingInsightsUiAction.NavigateBack)
                }
            }
        }
    }

    private fun handleSelectPeriodPreset(preset: SpendingPeriodPreset) {
        _selectionState.update { current ->
            if (preset == SpendingPeriodPreset.CUSTOM) {
                val defaultEnd = latestNewestExpenseDate ?: todayProvider()
                val defaultStart = latestOldestExpenseDate ?: defaultEnd
                current.copy(
                    preset = preset,
                    customStartDate = current.customStartDate ?: defaultStart,
                    customEndDate = current.customEndDate ?: defaultEnd,
                    selectedDayBarId = null
                )
            } else {
                current.copy(
                    preset = preset,
                    selectedDayBarId = null
                )
            }
        }
    }

    private fun handleSelectCustomStartDate(date: LocalDate) {
        _selectionState.update { current ->
            val clampedEnd = maxOf(current.customEndDate ?: date, date)
            current.copy(
                preset = SpendingPeriodPreset.CUSTOM,
                customStartDate = date,
                customEndDate = clampedEnd,
                selectedDayBarId = null
            )
        }
    }

    private fun handleSelectCustomEndDate(date: LocalDate) {
        _selectionState.update { current ->
            val clampedStart = minOf(current.customStartDate ?: date, date)
            current.copy(
                preset = SpendingPeriodPreset.CUSTOM,
                customStartDate = clampedStart,
                customEndDate = date,
                selectedDayBarId = null
            )
        }
    }

    private fun resolvePeriod(selection: SpendingInsightsSelectionState): SpendingInsightsPeriod =
        when (selection.preset) {
            SpendingPeriodPreset.ALL_TIME -> SpendingInsightsPeriod.AllTime
            SpendingPeriodPreset.THIS_MONTH -> SpendingInsightsPeriod.ThisMonth
            SpendingPeriodPreset.LAST_MONTH -> SpendingInsightsPeriod.LastMonth
            SpendingPeriodPreset.CUSTOM -> {
                val fallbackDate = todayProvider()
                SpendingInsightsPeriod.CustomRange(
                    startDate = selection.customStartDate ?: fallbackDate,
                    endDate = selection.customEndDate ?: fallbackDate
                )
            }
        }
}
