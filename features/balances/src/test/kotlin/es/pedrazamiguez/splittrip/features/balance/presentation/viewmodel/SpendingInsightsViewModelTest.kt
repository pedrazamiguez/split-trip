package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingBurnRateInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.SpendingInsightsUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.SpendingInsightsUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.SpendingInsightsUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.TripReportExportAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler.TripReportExportEventHandler
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.SpendingInsightsUiState
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SpendingInsightsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 11, 20)

    private lateinit var getGroupSpendingInsightsFlowUseCase: GetGroupSpendingInsightsFlowUseCase
    private lateinit var spendingInsightsUiMapper: SpendingInsightsUiMapper
    private lateinit var tripReportExportEventHandler: TripReportExportEventHandler
    private lateinit var exportSheetStateFlow: MutableStateFlow<TripReportExportSheetState>
    private var boundExportActionEmitter: (suspend (TripReportExportAction) -> Unit)? = null
    private lateinit var viewModel: SpendingInsightsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupSpendingInsightsFlowUseCase = mockk()
        spendingInsightsUiMapper = mockk()
        exportSheetStateFlow = MutableStateFlow(TripReportExportSheetState())
        tripReportExportEventHandler = mockk(relaxed = true) {
            every { exportSheetState } returns exportSheetStateFlow
            every { bind(any(), any()) } answers {
                boundExportActionEmitter = secondArg()
            }
        }

        every { getGroupSpendingInsightsFlowUseCase(any(), any(), any(), any()) } answers {
            val groupId = firstArg<String>()
            val periodFlow = secondArg<Flow<SpendingInsightsPeriod>>()
            val filterFlow = thirdArg<Flow<ExpenseFilterCriteria>>()
            kotlinx.coroutines.flow.combine(periodFlow, filterFlow) { period, filter ->
                createInsights(groupId = groupId, period = period, isFiltered = filter.isActive)
            }
        }

        every {
            spendingInsightsUiMapper.mapToUiState(
                insights = any(),
                selectedPreset = any(),
                customStartDate = any(),
                customEndDate = any(),
                entityBreakdownMode = any(),
                selectedDayBarId = any(),
                referenceYear = any(),
                activeFilterCount = any()
            )
        } answers {
            val insights = arg<GroupSpendingInsights>(0)
            val preset = arg<SpendingPeriodPreset>(1)
            val start = arg<LocalDate?>(2)
            val end = arg<LocalDate?>(3)
            val mode = arg<EntityBreakdownMode>(4)
            val selectedDayBarId = arg<String?>(5)
            val activeFilterCount = arg<Int>(7)
            SpendingInsightsUiState(
                isLoading = false,
                groupId = insights.groupId,
                selectedPreset = preset,
                customStartDate = start,
                customEndDate = end,
                oldestExpenseDate = insights.oldestExpenseDate,
                newestExpenseDate = insights.newestExpenseDate,
                hasAnyGroupExpenses = true,
                hasPeriodExpenses = true,
                isFiltered = insights.isFiltered,
                activeFilterCount = activeFilterCount,
                selectedDayBarId = selectedDayBarId,
                entityBreakdownMode = mode
            )
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading before selected group is set`() = runTest(testDispatcher) {
        viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
        assertNull(state.groupId)
    }

    @Test
    fun `setSelectedGroup observes insights and maps to populated UiState`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("group-1", state.groupId)
        assertEquals(SpendingPeriodPreset.ALL_TIME, state.selectedPreset)
        collectJob.cancel()
    }

    @Test
    fun `SelectPeriodPreset switches between AllTime ThisMonth LastMonth and Custom resetting selectedDayBarId`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SpendingInsightsUiEvent.SelectDayBar("2026-11-12"))
            advanceUntilIdle()
            assertEquals("2026-11-12", viewModel.uiState.value.selectedDayBarId)

            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.THIS_MONTH))
            advanceUntilIdle()
            assertEquals(SpendingPeriodPreset.THIS_MONTH, viewModel.uiState.value.selectedPreset)
            assertNull(viewModel.uiState.value.selectedDayBarId)

            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.LAST_MONTH))
            advanceUntilIdle()
            assertEquals(SpendingPeriodPreset.LAST_MONTH, viewModel.uiState.value.selectedPreset)

            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.CUSTOM))
            advanceUntilIdle()
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(LocalDate.of(2026, 11, 1), viewModel.uiState.value.customStartDate)
            assertEquals(LocalDate.of(2026, 11, 18), viewModel.uiState.value.customEndDate)

            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.ALL_TIME))
            advanceUntilIdle()
            assertEquals(SpendingPeriodPreset.ALL_TIME, viewModel.uiState.value.selectedPreset)
            collectJob.cancel()
        }

    @Test
    fun `SelectCustomStartDate after current endDate clamps endDate to startDate`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectCustomEndDate(LocalDate.of(2026, 11, 10)))
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectCustomStartDate(LocalDate.of(2026, 11, 15)))
        advanceUntilIdle()

        assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
        assertEquals(LocalDate.of(2026, 11, 15), viewModel.uiState.value.customStartDate)
        assertEquals(LocalDate.of(2026, 11, 15), viewModel.uiState.value.customEndDate)
        collectJob.cancel()
    }

    @Test
    fun `SelectCustomEndDate before current startDate clamps startDate to endDate`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectCustomStartDate(LocalDate.of(2026, 11, 15)))
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectCustomEndDate(LocalDate.of(2026, 11, 10)))
        advanceUntilIdle()

        assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
        assertEquals(LocalDate.of(2026, 11, 10), viewModel.uiState.value.customStartDate)
        assertEquals(LocalDate.of(2026, 11, 10), viewModel.uiState.value.customEndDate)
        collectJob.cancel()
    }

    @Test
    fun `SelectDayBar updates selectedDayBarId without restarting upstream flow`() = runTest(testDispatcher) {
        val upstreamTrigger = MutableStateFlow(0)
        every { getGroupSpendingInsightsFlowUseCase("group-1", any(), any(), any()) } answers {
            upstreamTrigger.map { createInsights("group-1", SpendingInsightsPeriod.AllTime) }
        }
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectDayBar("2026-11-08"))
        advanceUntilIdle()

        assertEquals("2026-11-08", viewModel.uiState.value.selectedDayBarId)
        verify(exactly = 1) { getGroupSpendingInsightsFlowUseCase("group-1", any(), any(), any()) }
        collectJob.cancel()
    }

    @Test
    fun `SelectEntityBreakdownMode toggles between BY_SUBUNIT and BY_MEMBER`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()
        assertEquals(EntityBreakdownMode.BY_SUBUNIT, viewModel.uiState.value.entityBreakdownMode)

        viewModel.onEvent(SpendingInsightsUiEvent.SelectEntityBreakdownMode(EntityBreakdownMode.BY_MEMBER))
        advanceUntilIdle()
        assertEquals(EntityBreakdownMode.BY_MEMBER, viewModel.uiState.value.entityBreakdownMode)

        viewModel.onEvent(SpendingInsightsUiEvent.SelectEntityBreakdownMode(EntityBreakdownMode.BY_SUBUNIT))
        advanceUntilIdle()
        assertEquals(EntityBreakdownMode.BY_SUBUNIT, viewModel.uiState.value.entityBreakdownMode)
        collectJob.cancel()
    }

    @Test
    fun `NavigateBack event emits SpendingInsightsUiAction NavigateBack`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val actions = mutableListOf<SpendingInsightsUiAction>()
        val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.actions.collect { actions.add(it) }
        }

        viewModel.onEvent(SpendingInsightsUiEvent.NavigateBack)
        advanceUntilIdle()

        assertTrue(actions.any { it is SpendingInsightsUiAction.NavigateBack })
        actionsJob.cancel()
    }

    @Test
    fun `upstream flow error emits ShowLoadError action and clears loading state`() = runTest(testDispatcher) {
        every { getGroupSpendingInsightsFlowUseCase(any(), any(), any(), any()) } returns flow {
            throw IOException("Boom")
        }
        viewModel = createViewModel()
        val actions = mutableListOf<SpendingInsightsUiAction>()
        val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.actions.collect { actions.add(it) }
        }
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("group-1", viewModel.uiState.value.groupId)
        assertTrue(actions.any { it is SpendingInsightsUiAction.ShowLoadError })
        actionsJob.cancel()
        collectJob.cancel()
    }

    @Test
    fun `switching to a different group resets period preset and selection state`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.THIS_MONTH))
        viewModel.onEvent(SpendingInsightsUiEvent.SelectEntityBreakdownMode(EntityBreakdownMode.BY_MEMBER))
        viewModel.onEvent(SpendingInsightsUiEvent.SelectDayBar("2026-11-12"))
        advanceUntilIdle()

        assertEquals(SpendingPeriodPreset.THIS_MONTH, viewModel.uiState.value.selectedPreset)
        assertEquals(EntityBreakdownMode.BY_MEMBER, viewModel.uiState.value.entityBreakdownMode)

        viewModel.setSelectedGroup("group-2")
        advanceUntilIdle()

        assertEquals("group-2", viewModel.uiState.value.groupId)
        assertEquals(SpendingPeriodPreset.ALL_TIME, viewModel.uiState.value.selectedPreset)
        assertEquals(EntityBreakdownMode.BY_SUBUNIT, viewModel.uiState.value.entityBreakdownMode)
        assertNull(viewModel.uiState.value.selectedDayBarId)
        collectJob.cancel()
    }

    @Test
    fun `setFilterCriteria seeds custom date strips banner dates and keeps period on clear`() =
        runTest(testDispatcher) {
            val start = LocalDate.of(2026, 11, 2)
            val end = LocalDate.of(2026, 11, 10)
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD),
                startDate = start,
                endDate = end
            )

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setFilterCriteria(criteria)
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isFiltered)
            assertEquals(1, viewModel.uiState.value.activeFilterCount)
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(start, viewModel.uiState.value.customStartDate)
            assertEquals(end, viewModel.uiState.value.customEndDate)

            viewModel.onEvent(SpendingInsightsUiEvent.ClearExpenseFilters)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(0, viewModel.uiState.value.activeFilterCount)
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(start, viewModel.uiState.value.customStartDate)
            assertEquals(end, viewModel.uiState.value.customEndDate)
            collectJob.cancel()
        }

    @Test
    fun `setFilterCriteria with date-only filter pre-fills custom period without activating filter banner`() =
        runTest(testDispatcher) {
            val start = LocalDate.of(2026, 11, 3)
            val end = LocalDate.of(2026, 11, 14)
            val capturedFilters = mutableListOf<ExpenseFilterCriteria>()
            every { getGroupSpendingInsightsFlowUseCase(any(), any(), any(), any()) } answers {
                val groupId = firstArg<String>()
                val periodFlow = secondArg<Flow<SpendingInsightsPeriod>>()
                val filterFlow = thirdArg<Flow<ExpenseFilterCriteria>>()
                filterFlow.map { filter ->
                    capturedFilters.add(filter)
                    createInsights(
                        groupId = groupId,
                        period = SpendingInsightsPeriod.AllTime,
                        isFiltered = filter.isActive
                    )
                }
            }

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setFilterCriteria(ExpenseFilterCriteria(startDate = start, endDate = end))
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(0, viewModel.uiState.value.activeFilterCount)
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(start, viewModel.uiState.value.customStartDate)
            assertEquals(end, viewModel.uiState.value.customEndDate)
            assertTrue(capturedFilters.all { !it.isDateFiltered && !it.isActive })

            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.ALL_TIME))
            advanceUntilIdle()

            assertEquals(SpendingPeriodPreset.ALL_TIME, viewModel.uiState.value.selectedPreset)
            collectJob.cancel()
        }

    @Test
    fun `setFilterCriteria with start-date-only resolves open end date from group insights`() =
        runTest(testDispatcher) {
            val start = LocalDate.of(2026, 11, 5)
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setFilterCriteria(ExpenseFilterCriteria(startDate = start))
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(start, viewModel.uiState.value.customStartDate)
            assertEquals(LocalDate.of(2026, 11, 18), viewModel.uiState.value.customEndDate)
            collectJob.cancel()
        }

    @Test
    fun `setFilterCriteria with end-date-only resolves open start date from group insights`() =
        runTest(testDispatcher) {
            val end = LocalDate.of(2026, 11, 15)
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setFilterCriteria(ExpenseFilterCriteria(endDate = end))
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(SpendingPeriodPreset.CUSTOM, viewModel.uiState.value.selectedPreset)
            assertEquals(LocalDate.of(2026, 11, 1), viewModel.uiState.value.customStartDate)
            assertEquals(end, viewModel.uiState.value.customEndDate)
            collectJob.cancel()
        }

    @Test
    fun `re-invoking setFilterCriteria with identical criteria preserves user period and clear changes`() =
        runTest(testDispatcher) {
            val criteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD),
                startDate = LocalDate.of(2026, 11, 2),
                endDate = LocalDate.of(2026, 11, 10)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setFilterCriteria(criteria)
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SpendingInsightsUiEvent.ClearExpenseFilters)
            viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.THIS_MONTH))
            advanceUntilIdle()

            viewModel.setFilterCriteria(criteria)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isFiltered)
            assertEquals(SpendingPeriodPreset.THIS_MONTH, viewModel.uiState.value.selectedPreset)
            collectJob.cancel()
        }

    @Test
    fun `RequestExportReport pre-fills active insights period and bounds`() = runTest(testDispatcher) {
        viewModel = createViewModel()
        val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setSelectedGroup("group-1")
        viewModel.onEvent(SpendingInsightsUiEvent.SelectPeriodPreset(SpendingPeriodPreset.LAST_MONTH))
        advanceUntilIdle()

        viewModel.onEvent(SpendingInsightsUiEvent.RequestExportReport)
        advanceUntilIdle()

        verify {
            tripReportExportEventHandler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.SPENDING_INSIGHTS_EXPORT_ACTION,
                initialPreset = SpendingPeriodPreset.LAST_MONTH,
                initialCustomStartDate = null,
                initialCustomEndDate = null,
                oldestExpenseDate = LocalDate.of(2026, 11, 1),
                newestExpenseDate = LocalDate.of(2026, 11, 18)
            )
        }
        collectJob.cancel()
    }

    @Test
    fun `bound export handler action forwards ShareExportedReport to viewModel actions`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            val emittedActions = mutableListOf<SpendingInsightsUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }

            boundExportActionEmitter?.invoke(
                TripReportExportAction.ShareExportedReport(
                    contentUri = "content://test/export.csv",
                    mimeType = "text/csv",
                    fileName = "export.csv"
                )
            )
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is SpendingInsightsUiAction.ShareExportedReport })
            collectJob.cancel()
        }

    @Test
    fun `SaveExportReport delegates to handler and forwards picker and save success actions`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            val emittedActions = mutableListOf<SpendingInsightsUiAction>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { emittedActions.add(it) }
            }
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SpendingInsightsUiEvent.SaveExportReport)
            boundExportActionEmitter?.invoke(
                TripReportExportAction.LaunchSaveDocumentPicker(
                    fileName = "export.csv",
                    mimeType = "text/csv"
                )
            )
            boundExportActionEmitter?.invoke(
                TripReportExportAction.ShowExportSaveSuccess(UiText.DynamicString("Saved export.csv"))
            )
            advanceUntilIdle()

            verify { tripReportExportEventHandler.handleSaveExport("group-1") }
            assertTrue(
                emittedActions.any {
                    it == SpendingInsightsUiAction.LaunchSaveDocumentPicker(
                        fileName = "export.csv",
                        mimeType = "text/csv"
                    )
                }
            )
            assertTrue(
                emittedActions.any {
                    it == SpendingInsightsUiAction.ShowExportSaveSuccess(UiText.DynamicString("Saved export.csv"))
                }
            )
            collectJob.cancel()
        }

    @Test
    fun `CompleteSaveExportedReport and CancelSaveExportedReport delegate to tripReportExportEventHandler`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()
            viewModel.setSelectedGroup("group-1")
            advanceUntilIdle()

            viewModel.onEvent(SpendingInsightsUiEvent.CompleteSaveExportedReport("content://documents/export.csv"))
            viewModel.onEvent(SpendingInsightsUiEvent.CancelSaveExportedReport)
            advanceUntilIdle()

            verify { tripReportExportEventHandler.handleCompleteSaveExport("content://documents/export.csv") }
            verify { tripReportExportEventHandler.handleCancelSaveExport() }
        }

    private fun createViewModel(): SpendingInsightsViewModel = SpendingInsightsViewModel(
        getGroupSpendingInsightsFlowUseCase = getGroupSpendingInsightsFlowUseCase,
        spendingInsightsUiMapper = spendingInsightsUiMapper,
        tripReportExportEventHandler = tripReportExportEventHandler,
        computationDispatcher = testDispatcher,
        todayProvider = { fixedToday }
    )

    private fun createInsights(
        groupId: String,
        period: SpendingInsightsPeriod,
        isFiltered: Boolean = false
    ): GroupSpendingInsights = GroupSpendingInsights(
        groupId = groupId,
        currency = "EUR",
        period = period,
        hasAnyGroupExpenses = true,
        oldestExpenseDate = LocalDate.of(2026, 11, 1),
        newestExpenseDate = LocalDate.of(2026, 11, 18),
        totalSpentCents = 10000L,
        totalBaseSpentCents = 10000L,
        totalExtrasCents = 0L,
        isFiltered = isFiltered,
        expenseCount = 2,
        burnRate = SpendingBurnRateInsight(
            windowStartDate = LocalDate.of(2026, 11, 1),
            windowEndDate = LocalDate.of(2026, 11, 18),
            elapsedDays = 18,
            totalWindowDays = 18,
            activeDaysCount = 2,
            averageDailySpendCents = 556L,
            averagePerActiveDayCents = 5000L,
            projectedMonthlySpendCents = 16667L,
            peakDayDate = LocalDate.of(2026, 11, 12),
            peakDayAmountCents = 6000L,
            dailySeries = emptyList()
        ),
        categoryBreakdown = emptyList(),
        memberBreakdown = emptyList(),
        entityBreakdown = emptyList(),
        hasSubunits = false,
        memberProfiles = emptyMap(),
        currentUserId = "u1"
    )
}
