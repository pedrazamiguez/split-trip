package es.pedrazamiguez.splittrip.features.subunit.presentation.screen

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.subunit.presentation.component.SubunitWizard
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.state.CreateEditSubunitUiState

/**
 * Shared element transition key for the Create Subunit FAB -> Screen transition.
 */
const val CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY = "create_edit_subunit_container"

@Composable
fun CreateEditSubunitScreen(
    uiState: CreateEditSubunitUiState = CreateEditSubunitUiState(),
    onEvent: (CreateEditSubunitUiEvent) -> Unit = {}
) {
    SharedTransitionSurface(sharedElementKey = CREATE_EDIT_SUBUNIT_SHARED_ELEMENT_KEY) {
        if (uiState.isLoading) {
            ShimmerLoadingList()
        } else {
            SubunitWizard(uiState = uiState, onEvent = onEvent)
        }
    }
}
