package es.pedrazamiguez.splittrip.features.settings.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.settings.presentation.component.PlanAllowancesContent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.PlanAllowancesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState

@Composable
fun PlanAllowancesScreen(
    uiState: PlanAllowancesUiState,
    onEvent: (PlanAllowancesUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = { ShimmerLoadingList() }
    ) {
        PlanAllowancesContent(
            uiState = uiState,
            onEvent = onEvent,
            modifier = modifier
        )
    }
}
