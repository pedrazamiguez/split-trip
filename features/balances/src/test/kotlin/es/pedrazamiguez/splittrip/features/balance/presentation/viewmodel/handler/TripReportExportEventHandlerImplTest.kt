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
import es.pedrazamiguez.splittrip.domain.service.featuregate.ReportExportQuota
import es.pedrazamiguez.splittrip.domain.usecase.balance.BuildGroupTripReportUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.RecordReportExportUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.TripReportExportUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripReportExportEventHandlerImplTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedToday = LocalDate.of(2026, 11, 20)

    private val proQuota = ReportExportQuota(
        usedExports = 2,
        monthlyFreeLimit = 1,
        isProTier = true,
        isGatingEnabled = true
    )
    private val freeAvailableQuota = ReportExportQuota(
        usedExports = 0,
        monthlyFreeLimit = 1,
        isProTier = false,
        isGatingEnabled = true
    )
    private val freeExhaustedQuota = ReportExportQuota(
        usedExports = 1,
        monthlyFreeLimit = 1,
        isProTier = false,
        isGatingEnabled = true
    )

    private lateinit var featureGateService: FeatureGateService
    private lateinit var buildGroupTripReportUseCase: BuildGroupTripReportUseCase
    private lateinit var recordReportExportUseCase: RecordReportExportUseCase
    private lateinit var tripReportExportUiMapper: TripReportExportUiMapper
    private lateinit var tripReportExportService: TripReportExportService
    private lateinit var telemetryTracker: TelemetryTracker
    private lateinit var handler: TripReportExportEventHandlerImpl
    private val emittedActions = mutableListOf<TripReportExportAction>()

    @BeforeEach
    fun setUp() {
        featureGateService = mockk()
        buildGroupTripReportUseCase = mockk()
        recordReportExportUseCase = mockk()
        tripReportExportUiMapper = mockk()
        tripReportExportService = mockk()
        telemetryTracker = mockk(relaxed = true)
        emittedActions.clear()

        every { tripReportExportUiMapper.formatShortDate(any(), any()) } returns "1 Nov"
        coEvery { recordReportExportUseCase() } just runs

        handler = TripReportExportEventHandlerImpl(
            featureGateService = featureGateService,
            buildGroupTripReportUseCase = buildGroupTripReportUseCase,
            recordReportExportUseCase = recordReportExportUseCase,
            tripReportExportUiMapper = tripReportExportUiMapper,
            tripReportExportService = tripReportExportService,
            telemetryTracker = telemetryTracker,
            todayProvider = { fixedToday }
        )
    }

    @Test
    fun `handleExportRequested when Free monthly quota exhausted tracks telemetry and emits ShowProFeaturePreview`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeExhaustedQuota)

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
    fun `handleExportRequested for Free user with quota opens PDF-only sheet with CSV locked and quota pill`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()

            val state = handler.exportSheetState.value
            assertTrue(state.isVisible)
            assertEquals(TripReportFormat.PDF, state.selectedFormat)
            assertTrue(state.isCsvLocked)
            assertFalse(state.includesItemisedDetails)
            assertEquals(1, state.remainingFreeExports)
            assertEquals(1, state.monthlyFreeExportLimit)

            // Tapping CSV on Free closes export sheet and shows Pro preview
            handler.handleSelectFormat(TripReportFormat.CSV)
            advanceUntilIdle()

            assertFalse(handler.exportSheetState.value.isVisible)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.single() is TripReportExportAction.ShowProFeaturePreview)
        }

    @Test
    fun `handleConfirmExport on Free omits itemised details and increments quota on success`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf",
                fileName = "summary.pdf",
                mimeType = "application/pdf"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(
                    report = report,
                    referenceYear = fixedToday.year,
                    includeItemisedDetails = false
                )
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exportedFile

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            coVerify(exactly = 1) { recordReportExportUseCase() }
            assertFalse(handler.exportSheetState.value.isVisible)
            assertTrue(emittedActions.single() is TripReportExportAction.ShareExportedReport)
        }

    @Test
    fun `handleExportRequested for Pro user opens full export sheet with initial period`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(proQuota)

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
            assertFalse(state.isCsvLocked)
            assertTrue(state.includesItemisedDetails)
            assertNull(state.remainingFreeExports)
            assertEquals(SpendingPeriodPreset.THIS_MONTH, state.selectedPreset)
            assertEquals(LocalDate.of(2026, 11, 1), state.oldestExpenseDate)
            assertEquals(LocalDate.of(2026, 11, 18), state.newestExpenseDate)
            assertTrue(emittedActions.isEmpty())
        }

    @Test
    fun `handleConfirmExport for Pro user supports CSV includes itemised details and records export`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(proQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.csv",
                fileName = "summary.csv",
                mimeType = "text/csv"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.ThisMonth, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(
                    report = report,
                    referenceYear = fixedToday.year,
                    includeItemisedDetails = true
                )
            } returns payload
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
            coVerify(exactly = 1) { recordReportExportUseCase() }
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
    fun `handleConfirmExport when exportService throws does not consume quota and emits ShowExportError`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(proQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, true)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } throws IOException("Disk full")

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleConfirmExport("group-1")
            advanceUntilIdle()

            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertTrue(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.single() is TripReportExportAction.ShowExportError)
        }

    @Test
    fun `period preset custom date clamping and dismiss update exportSheetState`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(proQuota)

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

            val laterDate = LocalDate.of(2026, 11, 25)
            handler.handleSelectCustomStartDate(laterDate)
            assertEquals(laterDate, handler.exportSheetState.value.customStartDate)
            assertEquals(laterDate, handler.exportSheetState.value.customEndDate)

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
    fun `handleSaveExport for Free user emits LaunchSaveDocumentPicker without consuming quota`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf",
                fileName = "summary.pdf",
                mimeType = "application/pdf"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, false)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exportedFile

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSaveExport("group-1")
            advanceUntilIdle()

            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertTrue(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertNull(handler.exportSheetState.value.activeExportTarget)
            assertEquals(
                listOf(
                    TripReportExportAction.LaunchSaveDocumentPicker(
                        fileName = "summary.pdf",
                        mimeType = "application/pdf"
                    )
                ),
                emittedActions
            )
        }

    @Test
    fun `handleSaveExport for Pro user with CSV emits LaunchSaveDocumentPicker without quota`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(proQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.csv",
                fileName = "summary.csv",
                mimeType = "text/csv"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, true)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.CSV) } returns exportedFile

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSelectFormat(TripReportFormat.CSV)
            handler.handleSaveExport("group-1")
            advanceUntilIdle()

            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertEquals(
                listOf(
                    TripReportExportAction.LaunchSaveDocumentPicker(
                        fileName = "summary.csv",
                        mimeType = "text/csv"
                    )
                ),
                emittedActions
            )
        }

    @Test
    fun `handleSaveExport when Free quota exhausted or CSV locked dismisses sheet and emits ShowProFeaturePreview`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returnsMany listOf(
                flowOf(freeAvailableQuota),
                flowOf(freeExhaustedQuota)
            )

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSaveExport("group-1")
            advanceUntilIdle()

            assertFalse(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertNull(handler.exportSheetState.value.activeExportTarget)
            coVerify(exactly = 0) { tripReportExportService.export(any(), any()) }
            assertTrue(emittedActions.single() is TripReportExportAction.ShowProFeaturePreview)
            verify {
                telemetryTracker.trackEvent(
                    MonetizationTelemetry.Event.FEATURE_GATE_BLOCKED,
                    any()
                )
            }
        }

    @Test
    fun `handleCompleteSaveExport copies file, consumes quota, closes sheet, emits success`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf",
                fileName = "summary.pdf",
                mimeType = "application/pdf"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, false)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exportedFile
            coEvery {
                tripReportExportService.saveToUri(exportedFile.contentUri, "content://documents/saved.pdf")
            } just runs

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSaveExport("group-1")
            advanceUntilIdle()
            handler.handleCompleteSaveExport("content://documents/saved.pdf")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                tripReportExportService.saveToUri(exportedFile.contentUri, "content://documents/saved.pdf")
            }
            coVerify(exactly = 1) { recordReportExportUseCase() }
            assertFalse(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertNull(handler.exportSheetState.value.activeExportTarget)
            assertEquals(2, emittedActions.size)
            assertTrue(emittedActions[1] is TripReportExportAction.ShowExportSaveSuccess)
        }

    @Test
    fun `handleCancelSaveExport clears pending save, keeps sheet visible, and skips quota`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf",
                fileName = "summary.pdf",
                mimeType = "application/pdf"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, false)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exportedFile

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSaveExport("group-1")
            advanceUntilIdle()
            handler.handleCancelSaveExport()
            advanceUntilIdle()

            coVerify(exactly = 0) { tripReportExportService.saveToUri(any(), any()) }
            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertTrue(handler.exportSheetState.value.isVisible)
            assertFalse(handler.exportSheetState.value.isExporting)
            assertNull(handler.exportSheetState.value.activeExportTarget)
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.single() is TripReportExportAction.LaunchSaveDocumentPicker)
        }

    @Test
    fun `handleCompleteSaveExport on IOException skips quota and emits ShowExportError`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }
            every { featureGateService.observeReportExportQuota() } returns flowOf(freeAvailableQuota)
            val report = mockk<GroupTripReport>()
            val payload = mockk<TripReportExportPayload>()
            val exportedFile = ExportedTripReportFile(
                contentUri = "content://es.pedrazamiguez.splittrip.fileprovider/exports/summary.pdf",
                fileName = "summary.pdf",
                mimeType = "application/pdf"
            )
            coEvery {
                buildGroupTripReportUseCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, any())
            } returns report
            every {
                tripReportExportUiMapper.mapToExportPayload(report, fixedToday.year, false)
            } returns payload
            coEvery { tripReportExportService.export(payload, TripReportFormat.PDF) } returns exportedFile
            coEvery {
                tripReportExportService.saveToUri(exportedFile.contentUri, "content://documents/saved.pdf")
            } throws IOException("Permission denied")

            handler.handleExportRequested(
                groupId = "group-1",
                gateEntryPoint = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION
            )
            advanceUntilIdle()
            handler.handleSaveExport("group-1")
            advanceUntilIdle()
            handler.handleCompleteSaveExport("content://documents/saved.pdf")
            advanceUntilIdle()

            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertFalse(handler.exportSheetState.value.isExporting)
            assertNull(handler.exportSheetState.value.activeExportTarget)
            assertEquals(2, emittedActions.size)
            assertTrue(emittedActions[1] is TripReportExportAction.ShowExportError)
        }

    @Test
    fun `handleCompleteSaveExport without pending save file emits ShowExportError without consuming quota`() =
        runTest(testDispatcher) {
            handler.bind(this) { emittedActions.add(it) }

            handler.handleCompleteSaveExport("content://documents/orphan.pdf")
            advanceUntilIdle()

            coVerify(exactly = 0) { recordReportExportUseCase() }
            assertEquals(1, emittedActions.size)
            assertTrue(emittedActions.single() is TripReportExportAction.ShowExportError)
        }
}
