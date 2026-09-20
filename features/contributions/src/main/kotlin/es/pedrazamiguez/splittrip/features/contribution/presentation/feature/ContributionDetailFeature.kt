package es.pedrazamiguez.splittrip.features.contribution.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.ContributionDetailScreen
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.ContributionDetailViewModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.ContributionDetailUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ContributionDetailFeature(
    groupId: String,
    contributionId: String,
    contributionDetailViewModel: ContributionDetailViewModel = koinViewModel<ContributionDetailViewModel>()
) {
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by contributionDetailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId, contributionId) {
        contributionDetailViewModel.setContext(groupId, contributionId)
    }

    LaunchedEffect(Unit) {
        contributionDetailViewModel.actions.collectLatest { action ->
            when (action) {
                is ContributionDetailUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is ContributionDetailUiAction.DeleteSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                    navController.popBackStack()
                }
            }
        }
    }

    SharedTransitionSurface(
        sharedElementKey = es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys.contributionCard(
            contributionId
        )
    ) {
        ContributionDetailScreen(
            uiState = uiState
        )
    }
}
