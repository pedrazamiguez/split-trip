package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt

sealed interface SpendingInsightsUiAction {
    data object NavigateBack : SpendingInsightsUiAction
    data class ShowLoadError(val message: UiText) : SpendingInsightsUiAction
    data class ShowExportError(val message: UiText) : SpendingInsightsUiAction
    data class ShowProFeaturePreview(val prompt: ProFeaturePreviewPrompt) : SpendingInsightsUiAction
    data class ShareExportedReport(
        val contentUri: String,
        val mimeType: String,
        val fileName: String
    ) : SpendingInsightsUiAction
    data class LaunchSaveDocumentPicker(
        val fileName: String,
        val mimeType: String
    ) : SpendingInsightsUiAction
    data class ShowExportSaveSuccess(val message: UiText) : SpendingInsightsUiAction
}
