package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.report.ExportedTripReportFile
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.model.report.TripReportExportPayload
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.domain.service.TripReportExportService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.balance.BuildGroupTripReportUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.TripReportExportUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripReportExportEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 11, 20)

    private lateinit var featureGateService: FeatureGateService
    private lateinit var buildGroupTripReportUseCase: BuildGroupTripReportUseCase
    private lateinit var tripReportExportUiMapper: TripReportExportUiMapper
    private lateinit var tripReportExportService: TripReportExportService
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var handler: TripReportExportEventHandlerImpl
    private val emittedActions = mutableListOf<TripReportExportAction>()

    @BeforeEach
    fun setUp() {
        featureGateService = mockk()
        buildGroupTripReportUseCase = mockk()
        tripReportExportUiMapper = mockk()
        tripReportExportService = mockk()
        telemetryTracker = mockk(relaxed = true)
        emittedActions.clear()

        every { tripReportExportUiMapper.formatShortDate(any(), any()) } returns "1 Nov"

        handler = TripReportExportEventHandlerImpl(
            featureGateService = featureGateService,
            buildGroupTripReportUseCase = buildGroupTripReportUseCase,
            tripReportExportUiMapper = tripReportExportUiMapper,
            tripReportExportService = tripReportExportService,
            telemetryTracker = telemetryTracker,
            todayProvider = { fixedToday }
        )
    }

    @Test
    fun `handleExportRequested when feature gate blocked tracks telemetry and emits ShowProFeaturePreview`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(false)

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()

            assertFalse(handler.exportSheetState.value.isVisible)
            assertEquals(
                listOf(
                    TripReportExportAction.ShowProFeaturePreview(
                        ProFeaturePreviewPrompt(
                            previewType = ProFeaturePreviewType.TRIP_REPORT_EXPORT,
                            entrySource = MonetizationTelemetry.EntrySource.TRIP_REPORT_EXPORT
                        )
                    )
                ),
                emittedActions
            )
            verify {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    match { params ->
                        params[MonetizationTelemetry.Param.GATE_NAME] == "trip_report_export" &&
                            params[MonetizationTelemetry.Param.ENTRY_POINT] ==
                            MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION &&
                            params[MonetizationTelemetry.Param.UPGRADE_PROMPTED] == true
                    }
                )
            }
        }

    @Test
    fun `handleExportRequested when feature gate enabled opens export sheet with initial period`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(true)

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.SPENDING_INSIGHTS_EXPORT_ACTION,
                initialPreset = SpendingPeriodPreset.THIS_MONTH,
                oldestExpenseDate = LocalDate.of(2026, 11, 1),
                newestExpenseDate = LocalDate.of(2026, 11, 18)
            )
            advanceUntilIdle()

            val state = handler.exportSheetState.value
            assertTrue(state.isVisible)
            assertEquals(SpendingPeriodPreset.THIS_MONTH, state.selectedPreset)
            assertEquals(LocalDate.of(2026, 11, 1), state.oldestExpenseDate)
            assertEquals(LocalDate.of(2026, 11, 18), state.newestExpenseDate)
            assertTrue(emittedActions.isEmpty())
        }

    @Test
    fun `handleConfirmExport builds report maps payload calls exportService and emits ShareExportedReport`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(true)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/statement.csv",
                fileName = "statement.csv",
                mimeType = "text/csv"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.ThisMonth, fixedToday, any())
            } returns report
            every { tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year) } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.CSV) } returns exportedFile

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION,
                initialPreset = SpendingPeriodPreset.THIS_MONTH
            )
            advanceUntilIdle()
            handler.handleSelectFormat(TripReportFormat.CSV)
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { tripReportExportService.export(payload, TripReportFormat.CSV) }
            assertFalse(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertEquals(
                listOf(
                    TripReportExportAction.ShareExportedReport(
                        contentUri = exportedFile.contentUri,
                        mimeType = exportedFile.mimeType,
                        fileName = exportedFile.fileName
                    )
                ),
                emittedActions
            )
        }

    @Test
    fun `handleConfirmExport when exportService throws emits ShowExportError and resets isExporting`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(true)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every { tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year) } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } throws IOException("Disk full")

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            assertTrue(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.single() is TripReportExportAction.ShowExportError)
        }

    @Test
    fun `period preset custom date clamping and dismiss update exportSheetState`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(true)

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION,
                oldestExpenseDate = LocalDate.of(2026, 11, 2),
                newestExpenseDate = LocalDate.of(2026, 11, 18)
            )
            advanceUntilIdle()

            handler.handleSelectPeriodPreset(SpendingPeriodPreset.CUSTOM)
            assertEquals(SpendingPeriodPreset.CUSTOM, handler.exportSheetState.value.selectedPreset)
            assertEquals(LocalDate.of(2026, 11, 2), handler.exportSheetState.value.customStartDate)
            assertEquals(LocalDate.of(2026, 11, 18), handler.exportSheetState.value.customEndDate)

            // Start date after current end clamps end date forward
            val laterDate = LocalDate.of(2026, 11, 25)
            handler.handleSelectCustomStartDate(laterDate)
            assertEquals(laterDate, handler.exportSheetState.value.customStartDate)
            assertEquals(laterDate, handler.exportSheetState.value.customEndDate)

            // End date before current start clamps start date backward
            val earlierDate = LocalDate.of(2026, 11, 5)
            handler.handleSelectCustomEndDate(earlierDate)
            assertEquals(earlierDate, handler.exportSheetState.value.customStartDate)
            assertEquals(earlierDate, handler.exportSheetState.value.customEndDate)

            handler.handleSelectPeriodPreset(SpendingPeriodPreset.LAST_MONTH)
            assertEquals(SpendingPeriodPreset.LAST_MONTH, handler.exportSheetState.value.selectedPreset)

            handler.handleDismissExportSheet()
            assertFalse(handler.exportSheetState.value.isVisible)
        }

    @Test
    fun `handleConfirmExport resolves LastMonth and CustomRange periods`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.isFeatureEnabled(GatedFeature.TRIP_REPORT_EXPORT, "group-1") } returns
                flowOf(true)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exported = ExportedTripReportFile("content://test/r.pdf", "r.pdf", "application/pdf")
            coEvery { buildGroupTripReportUseCase("group-1", any(), fixedToday, any()) } returns report
            every { tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year) } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exported

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION,
                initialPreset = SpendingPeriodPreset.LAST_MONTH
            )
            advanceUntilIdle()
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            coVerify {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.LastMonth, fixedToday, any())
            }

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION,
                initialPreset = SpendingPeriodPreset.CUSTOM,
                initialCustomStartDate = LocalDate.of(2026, 11, 1),
                initialCustomEndDate = LocalDate.of(2026, 11, 10)
            )
            advanceUntilIdle()
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            coVerify {
                buildGroupTripReportUseCase(
                    "group-1",
                    SpendingInsightsPeriod.CustomRange(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)),
                    fixedToday,
                    any()
                )
            }
        }
}
