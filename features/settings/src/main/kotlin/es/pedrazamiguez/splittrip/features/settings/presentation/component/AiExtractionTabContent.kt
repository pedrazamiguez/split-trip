package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.PhotoAi
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.ExtractionStatus

@Suppress("LongMethod")
@Composable
internal fun AiExtractionTabContent(
    uiState: DeveloperServicesUiState,
    onSelectReceiptForAiClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
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
