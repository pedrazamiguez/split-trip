package es.pedrazamiguez.splittrip.features.settlement.presentation.feature

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.ad.InterstitialAdManager
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.GroupSettlementOverviewScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.GroupSettlementOverviewViewModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.GroupSettlementOverviewUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@Composable
fun GroupSettlementOverviewFeature(
    groupId: String,
    groupSettlementOverviewViewModel: GroupSettlementOverviewViewModel = koinViewModel()
) {
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val koin = getKoin()
    val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }

    val uiState by groupSettlementOverviewViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        groupSettlementOverviewViewModel.setGroupId(groupId)
    }

    LaunchedEffect(Unit) {
        groupSettlementOverviewViewModel.actions.collectLatest { action ->
            when (action) {
                is GroupSettlementOverviewUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is GroupSettlementOverviewUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                    val activity = context as? Activity
                    if (activity != null) {
                        interstitialAdManager.onActionCompleted(activity) {}
                    }
                }
                GroupSettlementOverviewUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
                GroupSettlementOverviewUiAction.NavigateToYourBalance -> {
                    navController.navigate(Routes.YOUR_BALANCE)
                }
            }
        }
    }

    GroupSettlementOverviewScreen(
        uiState = uiState,
        onEvent = groupSettlementOverviewViewModel::onEvent
    )
}
