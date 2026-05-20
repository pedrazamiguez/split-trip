package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.EmailStamp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TextScan2
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedRawTextCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractedTextBlockCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.ExtractionResultsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrOperationsCard
import es.pedrazamiguez.splittrip.features.settings.presentation.component.SelectedAttachmentCard
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ExtractionStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.OcrStatus
import kotlinx.collections.immutable.ImmutableList

@Composable
fun DeveloperServicesScreen(
    uiState: DeveloperServicesUiState,
    onSelectOcrFileClick: () -> Unit,
    onSelectReceiptForAiClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.selectedTab,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab_content"
        ) { tab ->
            when (tab) {
                DeveloperServicesTab.Ocr -> OcrTabContent(
                    uiState = uiState,
                    onSelectOcrFileClick = onSelectOcrFileClick,
                    onEvent = onEvent
                )
                DeveloperServicesTab.AiExtraction -> AiExtractionTabContent(
                    uiState = uiState,
                    onSelectReceiptForAiClick = onSelectReceiptForAiClick
                )
                DeveloperServicesTab.AvatarGen -> AvatarGenTabContent()
            }
        }

        ServiceNavigationBar(
            selectedTab = uiState.selectedTab,
            onTabSelected = { onEvent(DeveloperServicesUiEvent.SwitchTab(it)) }
        )
    }
}

@Composable
private fun ServiceNavigationBar(
    selectedTab: DeveloperServicesTab,
    onTabSelected: (DeveloperServicesTab) -> Unit
) {
    val pillShape = RoundedCornerShape(50)
    NavigationBar(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.spacing.Large, vertical = MaterialTheme.spacing.Medium)
            .shadow(elevation = 8.dp, shape = pillShape)
            .clip(pillShape),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = selectedTab is DeveloperServicesTab.Ocr,
            onClick = { onTabSelected(DeveloperServicesTab.Ocr) },
            icon = { Icon(TablerIcons.Outline.TextScan2, contentDescription = null) },
            label = { Text(stringResource(R.string.developer_services_tab_ocr)) }
        )
        NavigationBarItem(
            selected = selectedTab is DeveloperServicesTab.AiExtraction,
            onClick = { onTabSelected(DeveloperServicesTab.AiExtraction) },
            icon = { Icon(TablerIcons.Outline.PhotoAi, contentDescription = null) },
            label = { Text(stringResource(R.string.developer_services_tab_ai_extraction)) }
        )
        NavigationBarItem(
            selected = selectedTab is DeveloperServicesTab.AvatarGen,
            onClick = { onTabSelected(DeveloperServicesTab.AvatarGen) },
            icon = { Icon(TablerIcons.Outline.EmailStamp, contentDescription = null) },
            label = { Text(stringResource(R.string.developer_services_tab_avatar)) }
        )
    }
}

// ─── OCR Tab ──────────────────────────────────────────────────────────────────

@Composable
private fun OcrTabContent(
    uiState: DeveloperServicesUiState,
    onSelectOcrFileClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        item {
            SelectedAttachmentCard(
                selectedFileUri = uiState.selectedFileUri,
                selectedFileName = uiState.selectedFileName,
                selectedFileMimeType = uiState.selectedFileMimeType,
                onSelectClick = onSelectOcrFileClick,
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
    }
}

private fun LazyListScope.extractedBlocksSection(textBlocks: ImmutableList<String>) {
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

// ─── AI Extraction Tab ────────────────────────────────────────────────────────

@Composable
private fun AiExtractionTabContent(
    uiState: DeveloperServicesUiState,
    onSelectReceiptForAiClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            when (uiState.extractionStatus) {
                ExtractionStatus.Loading -> ShimmerLoadingList(itemCount = 1)

                ExtractionStatus.Success -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
                ) {
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

                ExtractionStatus.Error -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
                ) {
                    item { FormErrorBanner(error = uiState.extractionErrorMessage) }
                }

                ExtractionStatus.Idle -> EmptyStateView(
                    icon = TablerIcons.Outline.PhotoAi,
                    title = stringResource(R.string.developer_services_tab_ai_extraction),
                    description = stringResource(R.string.developer_services_ai_no_receipt)
                )
            }
        }

        GradientButton(
            text = stringResource(R.string.developer_services_ai_select_receipt),
            onClick = onSelectReceiptForAiClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
    }
}

// ─── Avatar Generation Tab (placeholder) ─────────────────────────────────────

@Composable
private fun AvatarGenTabContent() {
    EmptyStateView(
        icon = TablerIcons.Outline.EmailStamp,
        title = stringResource(R.string.developer_services_avatar_title),
        description = stringResource(R.string.developer_services_avatar_subtitle)
    )
}
