package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt

sealed interface BalancesUiAction {
    data class ShowLoadError(val message: UiText) : BalancesUiAction
    data class ShowContributionSuccess(val message: UiText) : BalancesUiAction
    data class ShowContributionError(val message: UiText) : BalancesUiAction
    data class ShowDeleteContributionSuccess(val message: UiText) : BalancesUiAction
    data class ShowDeleteContributionError(val message: UiText) : BalancesUiAction
    data class ShowDeleteWithdrawalSuccess(val message: UiText) : BalancesUiAction
    data class ShowDeleteWithdrawalError(val message: UiText) : BalancesUiAction
    data class ShowExportError(val message: UiText) : BalancesUiAction
    data class ShowProFeaturePreview(val prompt: ProFeaturePreviewPrompt) : BalancesUiAction
    data class ShareExportedReport(
        val contentUri: String,
        val mimeType: String,
        val fileName: String
    ) : BalancesUiAction
    data class LaunchSaveDocumentPicker(
        val fileName: String,
        val mimeType: String
    ) : BalancesUiAction
    data class ShowExportSaveSuccess(val message: UiText) : BalancesUiAction
}
