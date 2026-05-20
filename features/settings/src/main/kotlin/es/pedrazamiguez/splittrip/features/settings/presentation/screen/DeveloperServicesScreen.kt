package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedRawTextCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedTextBlockCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractionOperationsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractionResultsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrOperationsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.SelectedAttachmentCard
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ExtractionStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.OcrStatus
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DeveloperServicesScreen(
    uiState: DeveloperServicesUiState,
    onSelectClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    DeveloperServicesList(
        uiState = uiState,
        onSelectClick = onSelectClick,
        onEvent = onEvent
    )
}

@Composable
private fun DeveloperServicesList(
    uiState: DeveloperServicesUiState,
    onSelectClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        item {
            SelectedAttachmentCard(
                selectedFileUri = uiState.selectedFileUri,
                selectedFileName = uiState.selectedFileName,
                selectedFileMimeType = uiState.selectedFileMimeType,
                onSelectClick = onSelectClick,
                onClearClick = { onEvent(DeveloperServicesUiEvent.Reset) }
            )
        }

        item {
            AnimatedVisibility(
                visible = uiState.selectedFileUri != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                OcrOperationsCard(
                    isLoading = uiState.ocrStatus is OcrStatus.Loading,
                    onRunOcrClick = { onEvent(DeveloperServicesUiEvent.RunOcr) }
                )
            }
        }

        item {
            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                FormErrorBanner(error = uiState.errorMessage)
            }
        }

        item {
            AnimatedVisibility(
                visible = uiState.ocrStatus is OcrStatus.Success && uiState.extractedText.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ExtractedRawTextCard(extractedText = uiState.extractedText)
            }
        }

        if (uiState.ocrStatus is OcrStatus.Success && uiState.textBlocks.isNotEmpty()) {
            extractedBlocksSection(uiState.textBlocks)
        }

        extractionSection(uiState, onEvent)
    }
}

private fun LazyListScope.extractionSection(
    uiState: DeveloperServicesUiState,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    if (uiState.ocrStatus !is OcrStatus.Success) return

    item {
        ExtractionOperationsCard(
            isLoading = uiState.extractionStatus is ExtractionStatus.Loading,
            capability = uiState.extractionCapability,
            onRunExtractionClick = { onEvent(DeveloperServicesUiEvent.RunExtraction) }
        )
    }

    if (uiState.extractionErrorMessage != null) {
        item {
            FormErrorBanner(error = uiState.extractionErrorMessage)
        }
    }

    if (uiState.extractionStatus is ExtractionStatus.Success) {
        item {
            ExtractionResultsCard(
                amount = uiState.extractedAmount,
                currency = uiState.extractedCurrency,
                date = uiState.extractedDate,
                title = uiState.extractedTitle,
                source = uiState.extractionSource,
                confidence = uiState.extractionConfidence
            )
        }
    }
}

private fun LazyListScope.extractedBlocksSection(
    textBlocks: ImmutableList<String>
) {
    item {
        Text(
            text = stringResource(R.string.developer_services_extracted_blocks, textBlocks.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = MaterialTheme.spacing.Small)
        )
    }

    itemsIndexed(textBlocks) { index, blockText ->
        ExtractedTextBlockCard(
            blockIndex = index + 1,
            blockText = blockText,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.Small)
        )
    }
}
