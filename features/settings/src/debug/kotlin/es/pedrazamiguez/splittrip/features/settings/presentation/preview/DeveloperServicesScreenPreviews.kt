package es.pedrazamiguez.splittrip.features.settings.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.CategorySuggestionStatus
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
private fun DeveloperServicesScreenAiLoadingPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedTab = DeveloperServicesTab.AiExtraction,
                extractionStatus = ExtractionStatus.Loading,
                activeResolvedModel = AiEngineType.LITE_RT_LM,
                installedLocalAiPack = InstalledLocalAiPackInfo(
                    packId = "qwen2.5-1.5b-litert-v1",
                    displayName = "Balanced (Qwen 2.5 1.5B)",
                    tier = ModelPackTier.BALANCED
                )
            ),
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
                extractedCategory = "SHOPPING",
                extractedSubcategory = "CONVENIENCE_STORE",
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
private fun DeveloperServicesScreenCategoryIdlePreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedTab = DeveloperServicesTab.CategorySuggestion,
                categorySuggestionStatus = CategorySuggestionStatus.Idle
            ),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}

@PreviewComplete
@Composable
private fun DeveloperServicesScreenCategoryMatchedPreview() {
    PreviewThemeWrapper {
        DeveloperServicesScreen(
            uiState = DeveloperServicesUiState(
                selectedTab = DeveloperServicesTab.CategorySuggestion,
                categorySuggestionInput = "Sushi dinner",
                categorySuggestionStatus = CategorySuggestionStatus.Matched,
                suggestedCategory = ExpenseCategory.FOOD,
                suggestedSubcategory = ExpenseSubcategory.RESTAURANT,
                suggestedCategorySource = CategorySuggestionSource.ON_DEVICE_AI,
                activeResolvedModel = AiEngineType.LITE_RT_LM,
                installedLocalAiPack = InstalledLocalAiPackInfo(
                    packId = "qwen3-0.6b-litert-v1",
                    displayName = "Compact (Qwen 3 0.6B)",
                    tier = ModelPackTier.COMPACT
                )
            ),
            onSelectOcrFileClick = {},
            onSelectReceiptForAiClick = {},
            onEvent = {}
        )
    }
}
