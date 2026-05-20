package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.OcrStatus
import kotlinx.collections.immutable.persistentListOf

@PreviewComplete
@Composable
private fun DeveloperServicesScreenIdlePreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedFileUri = null,
                ocrStatus = OcrStatus.Idle
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenSelectedPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedFileUri = "content://media/external/file/123",
                selectedFileName = "receipt_trip_2026.pdf",
                selectedFileMimeType = "application/pdf",
                ocrStatus = OcrStatus.Idle
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenLoadingPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedFileUri = "content://media/external/file/123",
                selectedFileName = "receipt_trip_2026.pdf",
                selectedFileMimeType = "application/pdf",
                ocrStatus = OcrStatus.Loading
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenSuccessPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedFileUri = "content://media/external/file/123",
                selectedFileName = "receipt_trip_2026.pdf",
                selectedFileMimeType = "application/pdf",
                ocrStatus = OcrStatus.Success,
                extractedText = "Total Amount: EUR 42.50\nDate: 2026-05-20",
                textBlocks = persistentListOf("Total Amount: EUR 42.50", "Date: 2026-05-20")
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}
