package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import es.pedrazamiguez.splittrip.core.designsystem.foundation.LocalHazeState
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.features.settings.navigation.SERVICE_TABS
import es.pedrazamiguez.splittrip.features.settings.presentation.component.AiExtractionTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.CategorySuggestionTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

private fun DeveloperServicesTab.toNavId() = when (this) {
    DeveloperServicesTab.Ocr -> "ocr"
    DeveloperServicesTab.AiExtraction -> "ai_extraction"
    DeveloperServicesTab.CategorySuggestion -> "category_suggestion"
}

private fun String.toDeveloperServicesTab() = when (this) {
    "ai_extraction" -> DeveloperServicesTab.AiExtraction
    "category_suggestion" -> DeveloperServicesTab.CategorySuggestion
    else -> DeveloperServicesTab.Ocr
}

@Composable
fun DeveloperServicesScreen(
    uiState: DeveloperServicesUiState,
    onSelectOcrFileClick: () -> Unit,
    onSelectReceiptForAiClick: () -> Unit,
    onEvent: (DeveloperServicesUiEvent) -> Unit
) {
    var navBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomBarPadding = with(LocalDensity.current) { navBarHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomPadding provides bottomBarPadding) {
            AnimatedContent(
                targetState = uiState.selectedTab,
                modifier = Modifier.fillMaxSize(),
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
                    DeveloperServicesTab.CategorySuggestion -> CategorySuggestionTabContent(
                        uiState = uiState,
                        onEvent = onEvent,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        FloatingNavigationBar(
            selectedId = uiState.selectedTab.toNavId(),
            onTabSelected = { id ->
                onEvent(DeveloperServicesUiEvent.SwitchTab(id.toDeveloperServicesTab()))
            },
            items = SERVICE_TABS,
            hazeState = LocalHazeState.current,
            applyWindowInsets = false,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { size -> navBarHeightPx = size.height }
        )
    }
}
