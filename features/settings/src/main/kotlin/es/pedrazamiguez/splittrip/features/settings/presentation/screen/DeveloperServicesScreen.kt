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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.EmailStamp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.TextScan2
import es.pedrazamiguez.splittrip.core.designsystem.navigation.FloatingNavTab
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.domain.model.AiEngineType
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

// ─── Tab items ────────────────────────────────────────────────────────────────

private object OcrTabItem : FloatingNavTab {
    override val id = "ocr"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) =
        Icon(TablerIcons.Outline.TextScan2, contentDescription = null, tint = tint)

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_ocr)
}

private object AiExtractionTabItem : FloatingNavTab {
    override val id = "ai_extraction"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) =
        Icon(TablerIcons.Outline.PhotoAi, contentDescription = null, tint = tint)

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_ai_extraction)
}

private object AvatarGenTabItem : FloatingNavTab {
    override val id = "avatar_gen"

    @Composable
    override fun Icon(isSelected: Boolean, tint: Color) =
        Icon(TablerIcons.Outline.EmailStamp, contentDescription = null, tint = tint)

    @Composable
    override fun getLabel() = stringResource(R.string.developer_services_tab_avatar)
}

private val SERVICE_TABS = listOf(OcrTabItem, AiExtractionTabItem, AvatarGenTabItem)

private fun DeveloperServicesTab.toNavId() = when (this) {
    DeveloperServicesTab.Ocr -> OcrTabItem.id
    DeveloperServicesTab.AiExtraction -> AiExtractionTabItem.id
    DeveloperServicesTab.AvatarGen -> AvatarGenTabItem.id
}

private fun String.toDeveloperServicesTab() = when (this) {
    AiExtractionTabItem.id -> DeveloperServicesTab.AiExtraction
    AvatarGenTabItem.id -> DeveloperServicesTab.AvatarGen
    else -> DeveloperServicesTab.Ocr
}

// ─── Screen ───────────────────────────────────────────────────────────────────

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
                    onSelectReceiptForAiClick = onSelectReceiptForAiClick,
                    onEvent = onEvent
                )
                DeveloperServicesTab.AvatarGen -> AvatarGenTabContent()
            }
        }

        FloatingNavigationBar(
            selectedId = uiState.selectedTab.toNavId(),
            onTabSelected = { id ->
                onEvent(DeveloperServicesUiEvent.SwitchTab(id.toDeveloperServicesTab()))
            },
            items = SERVICE_TABS
            // No hazeState — this screen has no scrollable hazeSource behind the bar.
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
    onSelectReceiptForAiClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MaterialTheme.spacing.Large),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Large)
    ) {
        AiModelSelector(
            availableModels = uiState.availableModels,
            selectedModel = uiState.developerOverrideModel,
            resolvedModel = uiState.activeResolvedModel,
            onModelSelected = { onEvent(DeveloperServicesUiEvent.SelectModel(it)) }
        )

        AiExtractionStatusContent(
            uiState = uiState,
            modifier = Modifier.weight(1f)
        )

        AnimatedVisibility(
            visible = uiState.extractionStatus !is ExtractionStatus.Loading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column {
                GradientButton(
                    text = stringResource(R.string.developer_services_ai_select_receipt),
                    onClick = onSelectReceiptForAiClick,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
            }
        }
    }
}

@Composable
private fun AiExtractionStatusContent(
    uiState: DeveloperServicesUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
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
                        time = uiState.extractedTime,
                        title = uiState.extractedTitle,
                        vendor = uiState.extractedVendor,
                        paymentMethod = uiState.extractedPaymentMethod,
                        category = uiState.extractedCategory,
                        notes = uiState.extractedNotes,
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

// ─── AI Model Selector ────────────────────────────────────────────────────────

@Composable
private fun AiModelSelector(
    availableModels: ImmutableList<AiEngineType?>,
    selectedModel: AiEngineType?,
    resolvedModel: AiEngineType?,
    onModelSelected: (AiEngineType?) -> Unit,
    modifier: Modifier = Modifier
) {
    FlatCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.Medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            Text(
                text = stringResource(R.string.developer_services_ai_model_selection),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                availableModels.forEach { model ->
                    val label = when (model) {
                        null -> stringResource(R.string.developer_services_ai_model_automatic)
                        AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
                        AiEngineType.LITE_RT_LM -> stringResource(R.string.developer_services_ai_model_lite_rt)
                    }

                    PassportChip(
                        label = label,
                        selected = selectedModel == model,
                        onClick = { onModelSelected(model) }
                    )
                }
            }

            resolvedModel?.let { resolved ->
                val resolvedLabel = when (resolved) {
                    AiEngineType.AI_CORE_GEMMA_4 -> stringResource(R.string.developer_services_ai_model_ai_core)
                    AiEngineType.LITE_RT_LM -> stringResource(R.string.developer_services_ai_model_lite_rt)
                }
                Text(
                    text = stringResource(R.string.developer_services_ai_model_resolved_label, resolvedLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
