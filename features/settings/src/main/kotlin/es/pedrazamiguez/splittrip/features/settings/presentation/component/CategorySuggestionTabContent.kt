package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.CategorySuggestionStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

@Composable
internal fun CategorySuggestionTabContent(
    uiState: DeveloperServicesUiState,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.ExtraLarge,
            top = spacing.ExtraLarge,
            end = spacing.ExtraLarge,
            bottom = spacing.ExtraLarge + LocalBottomPadding.current
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.Large)
    ) {
        item {
            AiModelSelector(
                availableModels = uiState.availableModels,
                selectedModel = uiState.developerOverrideModel,
                resolvedModel = uiState.activeResolvedModel,
                installedLocalAiPack = uiState.installedLocalAiPack,
                onModelSelected = { onEvent(DeveloperServicesUiEvent.SelectModel(it)) }
            )
        }
        item {
            CategorySuggestionInputCard(
                titleInput = uiState.categorySuggestionInput,
                selectedContext = uiState.categorySuggestionContext,
                onTitleChanged = { onEvent(DeveloperServicesUiEvent.CategoryTitleChanged(it)) },
                onContextChanged = { onEvent(DeveloperServicesUiEvent.CategoryContextChanged(it)) },
                onClearInput = { onEvent(DeveloperServicesUiEvent.ClearCategoryInput) }
            )
        }
        item {
            when (uiState.categorySuggestionStatus) {
                CategorySuggestionStatus.Idle -> EmptyStateView(
                    icon = TablerIcons.Outline.Category,
                    title = stringResource(R.string.developer_services_category_idle_title),
                    description = stringResource(R.string.developer_services_category_idle_description)
                )
                CategorySuggestionStatus.Loading -> CategorySuggestionLoadingCard()
                CategorySuggestionStatus.Matched -> CategorySuggestionResultCard(
                    category = uiState.suggestedCategory,
                    subcategory = uiState.suggestedSubcategory,
                    source = uiState.suggestedCategorySource,
                    activeResolvedModel = uiState.activeResolvedModel,
                    installedLocalAiPack = uiState.installedLocalAiPack
                )
                CategorySuggestionStatus.NoMatch -> EmptyStateView(
                    icon = TablerIcons.Outline.Search,
                    title = stringResource(R.string.developer_services_category_no_match_title),
                    description = stringResource(R.string.developer_services_category_no_match_description)
                )
                CategorySuggestionStatus.Error -> FormErrorBanner(error = uiState.categorySuggestionErrorMessage)
            }
        }
    }
}
