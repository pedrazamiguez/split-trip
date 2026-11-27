package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

@Composable
internal fun CategorySuggestionTabContent(
    uiState: DeveloperServicesUiState,
    onEvent: (DeveloperServicesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val bottomContentPadding = if (isImeVisible) {
        spacing.ExtraLarge
    } else {
        spacing.ExtraLarge + LocalBottomPadding.current
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentPadding = PaddingValues(
            start = spacing.ExtraLarge,
            top = spacing.ExtraLarge,
            end = spacing.ExtraLarge,
            bottom = bottomContentPadding
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
            CategorySuggestionInputSection(
                titleInput = uiState.categorySuggestionInput,
                selectedContext = uiState.categorySuggestionContext,
                onTitleChanged = { onEvent(DeveloperServicesUiEvent.CategoryTitleChanged(it)) },
                onContextChanged = { onEvent(DeveloperServicesUiEvent.CategoryContextChanged(it)) },
                onClearInput = { onEvent(DeveloperServicesUiEvent.ClearCategoryInput) }
            )
        }
        item {
            CategorySuggestionStatusSection(uiState = uiState)
        }
    }
}
