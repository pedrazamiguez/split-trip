package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

sealed interface TripReportExportAction {
    data class ShowProFeaturePreview(val prompt: ProFeaturePreviewPrompt) : TripReportExportAction
    data class ShareExportedReport(
        val contentUri: String,
        val mimeType: String,
        val fileName: String
    ) : TripReportExportAction
    data class LaunchSaveDocumentPicker(
        val fileName: String,
        val mimeType: String
    ) : TripReportExportAction
    data class ShowExportSaveSuccess(val message: UiText) : TripReportExportAction
    data class ShowExportError(val message: UiText) : TripReportExportAction
}

interface TripReportExportEventHandler {
    val exportSheetState: StateFlow<TripReportExportSheetState>

    fun bind(
        scope: CoroutineScope,
        onAction: suspend (TripReportExportAction) -> Unit
    )

    @Suppress("LongParameterList")
    fun handleExportRequested(
        groupId: String,
        gateEntryPoint: String,
        initialPreset: SpendingPeriodPreset = SpendingPeriodPreset.ALL_TIME,
        initialCustomStartDate: LocalDate? = null,
        initialCustomEndDate: LocalDate? = null,
        oldestExpenseDate: LocalDate? = null,
        newestExpenseDate: LocalDate? = null
    )

    fun handleDismissExportSheet()
    fun handleSelectPeriodPreset(preset: SpendingPeriodPreset)
    fun handleSelectCustomStartDate(date: LocalDate)
    fun handleSelectCustomEndDate(date: LocalDate)
    fun handleSelectFormat(format: TripReportFormat)
    fun handleConfirmExport(groupId: String)
    fun handleSaveExport(groupId: String)
    fun handleCompleteSaveExport(destinationUri: String)
    fun handleCancelSaveExport()
}
