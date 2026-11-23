package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SpendingInsightsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.SpendingInsightsUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.TripReportExportAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.TripReportExportEventHandler
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
    private val tripReportExportEventHandler: TripReportExportEventHandler,
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
    private val _filterCriteria = MutableStateFlow(ExpenseFilterCriteria())
    private val _selectionState = MutableStateFlow(SpendingInsightsSelectionState())
    private val _actions = MutableSharedFlow<SpendingInsightsUiAction>()
    val actions: SharedFlow<SpendingInsightsUiAction> = _actions.asSharedFlow()

    init {
        tripReportExportEventHandler.bind(viewModelScope) { exportAction ->
            val uiAction = when (exportAction) {
                is TripReportExportAction.ShowProFeaturePreview ->
                    SpendingInsightsUiAction.ShowProFeaturePreview(exportAction.prompt)
                is TripReportExportAction.ShareExportedReport ->
                    SpendingInsightsUiAction.ShareExportedReport(
                        contentUri = exportAction.contentUri,
                        mimeType = exportAction.mimeType,
                        fileName = exportAction.fileName
                    )
                is TripReportExportAction.ShowExportError ->
                    SpendingInsightsUiAction.ShowExportError(exportAction.message)
            }
            _actions.emit(uiAction)
        }
    }

    private var latestOldestExpenseDate: LocalDate? = null
    private var latestNewestExpenseDate: LocalDate? = null
    private var lastAppliedInitialCriteria: ExpenseFilterCriteria? = null
    private var pendingOpenStartFromFilter: Boolean = false
    private var pendingOpenEndFromFilter: Boolean = false

    private val periodFlow: Flow<SpendingInsightsPeriod> = _selectionState
        .map(::resolvePeriod)
        .distinctUntilChanged()

    val uiState: StateFlow<SpendingInsightsUiState> = combine(
        _selectedGroupId
            .filterNotNull()
            .flatMapLatest { groupId ->
                combine(
                    getGroupSpendingInsightsFlowUseCase(
                        groupId = groupId,
                        periodFlow = periodFlow,
                        filterCriteriaFlow = _filterCriteria,
                        todayProvider = todayProvider
                    ),
                    _selectionState,
                    _filterCriteria
                ) { insights, selection, filterCriteria ->
                    latestOldestExpenseDate = insights.oldestExpenseDate
                    latestNewestExpenseDate = insights.newestExpenseDate
                    resolvePendingOpenDateBounds(
                        oldestExpenseDate = insights.oldestExpenseDate,
                        newestExpenseDate = insights.newestExpenseDate
                    )

                    val effectiveFilterCount = filterCriteria.activeFilterCount +
                        if (filterCriteria.isSearchFiltered) 1 else 0

                    spendingInsightsUiMapper.mapToUiState(
                        insights = insights,
                        selectedPreset = selection.preset,
                        customStartDate = selection.customStartDate,
                        customEndDate = selection.customEndDate,
                        entityBreakdownMode = selection.entityBreakdownMode,
                        selectedDayBarId = selection.selectedDayBarId,
                        referenceYear = todayProvider().year,
                        activeFilterCount = effectiveFilterCount
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
            },
        tripReportExportEventHandler.exportSheetState
    ) { baseState, exportSheetState ->
        baseState.copy(exportSheetState = exportSheetState)
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
        val previousGroupId = _selectedGroupId.value
        if (groupId != previousGroupId) {
            if (previousGroupId != null && groupId != null) {
                _filterCriteria.value = ExpenseFilterCriteria()
                _selectionState.value = SpendingInsightsSelectionState()
                latestOldestExpenseDate = null
                latestNewestExpenseDate = null
                lastAppliedInitialCriteria = null
                pendingOpenStartFromFilter = false
                pendingOpenEndFromFilter = false
            }
            _selectedGroupId.value = groupId
        }
    }

    fun setFilterCriteria(criteria: ExpenseFilterCriteria) {
        if (criteria == lastAppliedInitialCriteria) return
        lastAppliedInitialCriteria = criteria

        // Period selector is the single source of truth for dates in Spending Insights;
        // pre-fill it from incoming date filters instead of double-filtering underneath.
        val nonDateCriteria = if (criteria.isDateFiltered) {
            criteria.copy(startDate = null, endDate = null)
        } else {
            criteria
        }
        if (nonDateCriteria != _filterCriteria.value) {
            _filterCriteria.value = nonDateCriteria
        }

        if (criteria.isDateFiltered) {
            seedPeriodFromDateFilter(
                startDate = criteria.startDate,
                endDate = criteria.endDate
            )
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
            SpendingInsightsUiEvent.ClearExpenseFilters -> handleClearExpenseFilters()
            SpendingInsightsUiEvent.NavigateBack -> {
                viewModelScope.launch {
                    _actions.emit(SpendingInsightsUiAction.NavigateBack)
                }
            }
            SpendingInsightsUiEvent.RequestExportReport,
            SpendingInsightsUiEvent.DismissExportReportSheet,
            is SpendingInsightsUiEvent.SelectExportPeriodPreset,
            is SpendingInsightsUiEvent.SelectExportCustomStartDate,
            is SpendingInsightsUiEvent.SelectExportCustomEndDate,
            is SpendingInsightsUiEvent.SelectExportFormat,
            SpendingInsightsUiEvent.ConfirmExportReport -> handleExportEvent(event)
        }
    }

    private fun handleExportEvent(event: SpendingInsightsUiEvent) {
        when (event) {
            SpendingInsightsUiEvent.RequestExportReport -> {
                val groupId = _selectedGroupId.value ?: return
                val selection = _selectionState.value
                tripReportExportEventHandler.handleExportRequested(
                    groupId = groupId,
                    gateEntryPoint = MonetizationTelemetry.GateEntryPoint.SPENDING_INSIGHTS_EXPORT_ACTION,
                    initialPreset = selection.preset,
                    initialCustomStartDate = selection.customStartDate,
                    initialCustomEndDate = selection.customEndDate,
                    oldestExpenseDate = latestOldestExpenseDate,
                    newestExpenseDate = latestNewestExpenseDate
                )
            }
            SpendingInsightsUiEvent.DismissExportReportSheet ->
                tripReportExportEventHandler.handleDismissExportSheet()
            is SpendingInsightsUiEvent.SelectExportPeriodPreset ->
                tripReportExportEventHandler.handleSelectPeriodPreset(event.preset)
            is SpendingInsightsUiEvent.SelectExportCustomStartDate ->
                tripReportExportEventHandler.handleSelectCustomStartDate(event.date)
            is SpendingInsightsUiEvent.SelectExportCustomEndDate ->
                tripReportExportEventHandler.handleSelectCustomEndDate(event.date)
            is SpendingInsightsUiEvent.SelectExportFormat ->
                tripReportExportEventHandler.handleSelectFormat(event.format)
            SpendingInsightsUiEvent.ConfirmExportReport -> {
                val groupId = _selectedGroupId.value ?: return
                tripReportExportEventHandler.handleConfirmExport(groupId)
            }
            else -> Unit
        }
    }

    private fun handleClearExpenseFilters() {
        _filterCriteria.value = ExpenseFilterCriteria()
        _selectionState.update { current ->
            current.copy(selectedDayBarId = null)
        }
    }

    private fun seedPeriodFromDateFilter(startDate: LocalDate?, endDate: LocalDate?) {
        when {
            startDate != null && endDate != null -> {
                val start = minOf(startDate, endDate)
                val end = maxOf(startDate, endDate)
                pendingOpenStartFromFilter = false
                pendingOpenEndFromFilter = false
                _selectionState.update { current ->
                    current.copy(
                        preset = SpendingPeriodPreset.CUSTOM,
                        customStartDate = start,
                        customEndDate = end,
                        selectedDayBarId = null
                    )
                }
            }

            startDate != null -> {
                val resolvedEnd = maxOf(latestNewestExpenseDate ?: todayProvider(), startDate)
                pendingOpenStartFromFilter = false
                pendingOpenEndFromFilter = latestNewestExpenseDate == null
                _selectionState.update { current ->
                    current.copy(
                        preset = SpendingPeriodPreset.CUSTOM,
                        customStartDate = startDate,
                        customEndDate = resolvedEnd,
                        selectedDayBarId = null
                    )
                }
            }

            endDate != null -> {
                val resolvedStart = minOf(latestOldestExpenseDate ?: endDate, endDate)
                pendingOpenStartFromFilter = latestOldestExpenseDate == null
                pendingOpenEndFromFilter = false
                _selectionState.update { current ->
                    current.copy(
                        preset = SpendingPeriodPreset.CUSTOM,
                        customStartDate = resolvedStart,
                        customEndDate = endDate,
                        selectedDayBarId = null
                    )
                }
            }
        }
    }

    private fun resolvePendingOpenDateBounds(
        oldestExpenseDate: LocalDate?,
        newestExpenseDate: LocalDate?
    ) {
        if (pendingOpenStartFromFilter && oldestExpenseDate != null) {
            pendingOpenStartFromFilter = false
            _selectionState.update { current ->
                val end = current.customEndDate ?: oldestExpenseDate
                current.copy(customStartDate = minOf(oldestExpenseDate, end))
            }
        }
        if (pendingOpenEndFromFilter && newestExpenseDate != null) {
            pendingOpenEndFromFilter = false
            _selectionState.update { current ->
                val start = current.customStartDate ?: newestExpenseDate
                current.copy(customEndDate = maxOf(newestExpenseDate, start))
            }
        }
    }

    private fun handleSelectPeriodPreset(preset: SpendingPeriodPreset) {
        pendingOpenStartFromFilter = false
        pendingOpenEndFromFilter = false
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
        pendingOpenStartFromFilter = false
        pendingOpenEndFromFilter = false
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
        pendingOpenStartFromFilter = false
        pendingOpenEndFromFilter = false
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
