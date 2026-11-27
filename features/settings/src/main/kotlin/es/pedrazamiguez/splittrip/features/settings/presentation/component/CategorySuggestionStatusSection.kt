package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Category
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.FormErrorBanner
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.CategorySuggestionStatus
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

@Composable
internal fun CategorySuggestionStatusSection(
    uiState: DeveloperServicesUiState,
    modifier: Modifier = Modifier
) {
    when (uiState.categorySuggestionStatus) {
        CategorySuggestionStatus.Idle -> EmptyStateView(
            icon = TablerIcons.Outline.Category,
            title = stringResource(R.string.developer_services_category_idle_title),
            description = stringResource(R.string.developer_services_category_idle_description),
            modifier = modifier
        )
        CategorySuggestionStatus.Loading -> CategorySuggestionLoadingCard(modifier = modifier)
        CategorySuggestionStatus.Matched -> CategorySuggestionResultCard(
            category = uiState.suggestedCategory,
            subcategory = uiState.suggestedSubcategory,
            source = uiState.suggestedCategorySource,
            activeResolvedModel = uiState.activeResolvedModel,
            installedLocalAiPack = uiState.installedLocalAiPack,
            modifier = modifier
        )
        CategorySuggestionStatus.NoMatch -> EmptyStateView(
            icon = TablerIcons.Outline.Search,
            title = stringResource(R.string.developer_services_category_no_match_title),
            description = stringResource(R.string.developer_services_category_no_match_description),
            modifier = modifier
        )
        CategorySuggestionStatus.Error -> FormErrorBanner(
            error = uiState.categorySuggestionErrorMessage,
            modifier = modifier
        )
    }
}
