package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.group.presentation.component.CreateGroupForm
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState

const val CREATE_GROUP_SHARED_ELEMENT_KEY = "create_group_container"

@Composable
fun CreateGroupScreen(
    uiState: CreateGroupUiState,
    onScannerClick: () -> Unit,
    onEvent: (CreateGroupUiEvent) -> Unit = {}
) {
    SharedTransitionSurface(sharedElementKey = CREATE_GROUP_SHARED_ELEMENT_KEY) {
        CreateGroupForm(
            uiState = uiState,
            onEvent = onEvent,
            onScannerClick = onScannerClick
        )
    }
}
