package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.group.presentation.component.CreateEditGroupForm
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState

const val CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY = "create_edit_group_container"

@Composable
fun CreateEditGroupScreen(
    uiState: CreateEditGroupUiState,
    onScannerClick: () -> Unit,
    onEvent: (CreateEditGroupUiEvent) -> Unit = {}
) {
    SharedTransitionSurface(sharedElementKey = CREATE_EDIT_GROUP_SHARED_ELEMENT_KEY) {
        CreateEditGroupForm(
            uiState = uiState,
            onEvent = onEvent,
            onScannerClick = onScannerClick
        )
    }
}
