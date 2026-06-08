package es.pedrazamiguez.splittrip.features.contribution.presentation.screen

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.contribution.presentation.component.ContributionWizard
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.state.AddContributionUiState

@Composable
fun AddContributionScreen(
    uiState: AddContributionUiState,
    onEvent: (AddContributionUiEvent) -> Unit = {}
) {
    SharedTransitionSurface(sharedElementKey = SharedElementKeys.ADD_CONTRIBUTION) {
        ContributionWizard(
            uiState = uiState,
            onEvent = onEvent
        )
    }
}
