package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackFeatureGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.domain.service.TripReportExportService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.balance.BuildGroupTripReportUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.RecordReportExportUseCase
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.TripReportExportUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList")
class TripReportExportEventHandlerImpl(
    private val featureGateService: FeatureGateService,
    private val buildGroupTripReportUseCase: BuildGroupTripReportUseCase,
    private val recordReportExportUseCase: RecordReportExportUseCase,
    private val tripReportExportUiMapper: TripReportExportUiMapper,
    private val tripReportExportService: TripReportExportService,
    private val telemetryTracker: TelemetryTracker,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) : TripReportExportEventHandler {

    private val _exportSheetState = MutableStateFlow(TripReportExportSheetState())
    override val exportSheetState: StateFlow<TripReportExportSheetState> = _exportSheetState.asStateFlow()

    private var coroutineScope: CoroutineScope? = null
    private var actionEmitter: (suspend (TripReportExportAction) -> Unit)? = null
    private var activeGateEntryPoint: String = MonetizationTelemetry.GateEntryPoint.BALANCES_EXPORT_ACTION

    override fun bind(
        scope: CoroutineScope,
        onAction: suspend (TripReportExportAction) -> Unit
    ) {
        coroutineScope = scope
        actionEmitter = onAction
    }

    @Suppress("LongParameterList")
    override fun handleExportRequested(
        groupId: String,
        gateEntryPoint: String,
        initialPreset: SpendingPeriodPreset,
        initialCustomStartDate: LocalDate?,
        initialCustomEndDate: LocalDate?,
        oldestExpenseDate: LocalDate?,
        newestExpenseDate: LocalDate?
    ) {
        val scope = coroutineScope ?: return
        activeGateEntryPoint = gateEntryPoint
        scope.launch {
            val quota = featureGateService.observeReportExportQuota().first()
            if (!quota.canExportPdf) {
                emitProFeaturePreview(gateEntryPoint)
                return@launch
            }

            val referenceYear = todayProvider().year
            val isCsvLocked = !quota.canExportCsv
            val resolvedFormat = if (isCsvLocked) {
                TripReportFormat.PDF
            } else {
                _exportSheetState.value.selectedFormat
            }
            _exportSheetState.value = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = initialPreset,
                customStartDate = initialCustomStartDate,
                customEndDate = initialCustomEndDate,
                formattedCustomStartDate = tripReportExportUiMapper.formatShortDate(
                    initialCustomStartDate,
                    referenceYear
                ),
                formattedCustomEndDate = tripReportExportUiMapper.formatShortDate(
                    initialCustomEndDate,
                    referenceYear
                ),
                oldestExpenseDate = oldestExpenseDate,
                newestExpenseDate = newestExpenseDate,
                selectedFormat = resolvedFormat,
                isCsvLocked = isCsvLocked,
                includesItemisedDetails = quota.includesItemisedDetails,
                remainingFreeExports = if (quota.isFreeQuotaTracked) quota.remainingExports else null,
                monthlyFreeExportLimit = if (quota.isFreeQuotaTracked) quota.monthlyFreeLimit else null,
                isExporting = false
            )
        }
    }

    override fun handleDismissExportSheet() {
        _exportSheetState.update { it.copy(isVisible = false, isExporting = false) }
    }

    override fun handleSelectPeriodPreset(preset: SpendingPeriodPreset) {
        val today = todayProvider()
        val referenceYear = today.year
        _exportSheetState.update { current ->
            if (preset == SpendingPeriodPreset.CUSTOM) {
                val defaultEnd = current.newestExpenseDate ?: today
                val defaultStart = current.oldestExpenseDate ?: defaultEnd
                val startDate = current.customStartDate ?: defaultStart
                val endDate = current.customEndDate ?: defaultEnd
                current.copy(
                    selectedPreset = preset,
                    customStartDate = startDate,
                    customEndDate = endDate,
                    formattedCustomStartDate = tripReportExportUiMapper.formatShortDate(startDate, referenceYear),
                    formattedCustomEndDate = tripReportExportUiMapper.formatShortDate(endDate, referenceYear)
                )
            } else {
                current.copy(selectedPreset = preset)
            }
        }
    }

    override fun handleSelectCustomStartDate(date: LocalDate) {
        val referenceYear = todayProvider().year
        _exportSheetState.update { current ->
            val clampedEnd = maxOf(current.customEndDate ?: date, date)
            current.copy(
                selectedPreset = SpendingPeriodPreset.CUSTOM,
                customStartDate = date,
                customEndDate = clampedEnd,
                formattedCustomStartDate = tripReportExportUiMapper.formatShortDate(date, referenceYear),
                formattedCustomEndDate = tripReportExportUiMapper.formatShortDate(clampedEnd, referenceYear)
            )
        }
    }

    override fun handleSelectCustomEndDate(date: LocalDate) {
        val referenceYear = todayProvider().year
        _exportSheetState.update { current ->
            val clampedStart = minOf(current.customStartDate ?: date, date)
            current.copy(
                selectedPreset = SpendingPeriodPreset.CUSTOM,
                customStartDate = clampedStart,
                customEndDate = date,
                formattedCustomStartDate = tripReportExportUiMapper.formatShortDate(clampedStart, referenceYear),
                formattedCustomEndDate = tripReportExportUiMapper.formatShortDate(date, referenceYear)
            )
        }
    }

    override fun handleSelectFormat(format: TripReportFormat) {
        if (format == TripReportFormat.CSV && _exportSheetState.value.isCsvLocked) {
            val scope = coroutineScope ?: return
            _exportSheetState.update { it.copy(isVisible = false, isExporting = false) }
            scope.launch {
                emitProFeaturePreview(activeGateEntryPoint)
            }
            return
        }
        _exportSheetState.update { it.copy(selectedFormat = format) }
    }

    override fun handleConfirmExport(groupId: String) {
        val currentState = _exportSheetState.value
        if (currentState.isExporting) return
        val scope = coroutineScope ?: return

        _exportSheetState.update { it.copy(isExporting = true) }
        scope.launch {
            try {
                val quota = featureGateService.observeReportExportQuota().first()
                val blockedByQuota = !quota.canExportPdf
                val blockedByFormat = currentState.selectedFormat == TripReportFormat.CSV && !quota.canExportCsv
                if (blockedByQuota || blockedByFormat) {
                    _exportSheetState.update { it.copy(isVisible = false, isExporting = false) }
                    emitProFeaturePreview(activeGateEntryPoint)
                    return@launch
                }

                val today = todayProvider()
                val period = resolvePeriod(currentState, today)
                val report = buildGroupTripReportUseCase(
                    groupId = groupId,
                    period = period,
                    today = today
                )
                val payload = tripReportExportUiMapper.mapToExportPayload(
                    report = report,
                    referenceYear = today.year,
                    includeItemisedDetails = quota.includesItemisedDetails
                )
                val exportedFile = tripReportExportService.export(
                    payload = payload,
                    format = currentState.selectedFormat
                )
                recordReportExportUseCase()
                _exportSheetState.update { it.copy(isVisible = false, isExporting = false) }
                actionEmitter?.invoke(
                    TripReportExportAction.ShareExportedReport(
                        contentUri = exportedFile.contentUri,
                        mimeType = exportedFile.mimeType,
                        fileName = exportedFile.fileName
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to export group summary for group $groupId")
                _exportSheetState.update { it.copy(isExporting = false) }
                actionEmitter?.invoke(
                    TripReportExportAction.ShowExportError(
                        UiText.StringResource(R.string.balances_export_error_failed)
                    )
                )
            }
        }
    }

    private suspend fun emitProFeaturePreview(entryPoint: String) {
        telemetryTracker.trackFeatureGateBlocked(
            feature = GatedFeature.TRIP_REPORT_EXPORT,
            entryPoint = entryPoint,
            upgradePrompted = true
        )
        actionEmitter?.invoke(
            TripReportExportAction.ShowProFeaturePreview(
                ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.TRIP_REPORT_EXPORT,
                    entrySource = MonetizationTelemetry.EntrySource.TRIP_REPORT_EXPORT
                )
            )
        )
    }

    private fun resolvePeriod(
        state: TripReportExportSheetState,
        today: LocalDate
    ): SpendingInsightsPeriod = when (state.selectedPreset) {
        SpendingPeriodPreset.ALL_TIME -> SpendingInsightsPeriod.AllTime
        SpendingPeriodPreset.THIS_MONTH -> SpendingInsightsPeriod.ThisMonth
        SpendingPeriodPreset.LAST_MONTH -> SpendingInsightsPeriod.LastMonth
        SpendingPeriodPreset.CUSTOM -> SpendingInsightsPeriod.CustomRange(
            startDate = state.customStartDate ?: today,
            endDate = state.customEndDate ?: today
        )
    }
}
