package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.EmailStamp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.navigation.FloatingNavigationBar
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.navigation.SERVICE_TABS
import es.pedrazamiguez.splittrip.features.settings.presentation.component.AiExtractionTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.component.OcrTabContent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesTab
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesUiState

private fun DeveloperServicesTab.toNavId() = when (this) {
    DeveloperServicesTab.Ocr -> "ocr"
    DeveloperServicesTab.AiExtraction -> "ai_extraction"
    DeveloperServicesTab.AvatarGen -> "avatar_gen"
}

private fun String.toDeveloperServicesTab() = when (this) {
    "ai_extraction" -> DeveloperServicesTab.AiExtraction
    "avatar_gen" -> DeveloperServicesTab.AvatarGen
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
                DeveloperServicesTab.AvatarGen -> {
                    EmptyStateView(
                        icon = TablerIcons.Outline.EmailStamp,
                        title = stringResource(R.string.developer_services_avatar_title),
                        description = stringResource(R.string.developer_services_avatar_subtitle)
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
            applyWindowInsets = false
        )
    }
}
