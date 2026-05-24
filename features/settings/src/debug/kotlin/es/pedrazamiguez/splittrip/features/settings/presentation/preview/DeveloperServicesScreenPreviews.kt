package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ExtractionStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.OcrStatus
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun DeveloperServicesScreenOcrIdlePreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(selectedTab = DeveloperServicesTab.Ocr),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenOcrSuccessPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedTab = DeveloperServicesTab.Ocr,
                selectedFileUri = "content://media/external/file/123",
                selectedFileName = "receipt_trip_2026.pdf",
                selectedFileMimeType = "application/pdf",
                ocrStatus = OcrStatus.Success,
                extractedText = "Total Amount: EUR 42.50\nDate: 2026-05-20",
                textBlocks = persistentListOf("Total Amount: EUR 42.50", "Date: 2026-05-20")
            ),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenAiIdlePreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(selectedTab = DeveloperServicesTab.AiExtraction),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenAiSuccessPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedTab = DeveloperServicesTab.AiExtraction,
                extractionStatus = ExtractionStatus.Success,
                extractedAmount = "991.00",
                extractedCurrency = "THB",
                extractedDate = "2026-05-20",
                extractedTitle = "7-Eleven",
                extractedNotes = "Locator: ABC123D",
                extractionSource = ExtractionSource.AI_CORE,
                extractionConfidence = ExtractionConfidence.HIGH
            ),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenAvatarPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(selectedTab = DeveloperServicesTab.AvatarGen),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}
