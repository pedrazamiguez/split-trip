package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.features.settings.navigation.SERVICE_TABS
import es.pedrazamiguez.splittrip.features.settings.presentation.component.AiExtractionTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.BillingTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

private fun DeveloperServicesTab.toNavId() = when (this) {
    DeveloperServicesTab.Ocr -> "ocr"
    DeveloperServicesTab.AiExtraction -> "ai_extraction"
    DeveloperServicesTab.Billing -> "billing"
}

private fun String.toDeveloperServicesTab() = when (this) {
    "ai_extraction" -> DeveloperServicesTab.AiExtraction
    "billing" -> DeveloperServicesTab.Billing
    else -> DeveloperServicesTab.Ocr
}

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
                DeveloperServicesTab.Billing -> BillingTabContent(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        FloatingNavigationBar(
            selectedId = uiState.selectedTab.toNavId(),
            onTabSelected = { id ->
                onEvent(DeveloperServicesUiEvent.SwitchTab(id.toDeveloperServicesTab()))
            },
            items = SERVICE_TABS,
            applyWindowInsets = false
        )
    }
}
