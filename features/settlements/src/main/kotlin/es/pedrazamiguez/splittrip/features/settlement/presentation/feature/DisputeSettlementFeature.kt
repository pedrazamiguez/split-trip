package es.pedrazamiguez.splittrip.features.settlement.presentation.feature

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.ad.InterstitialAdManager
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.features.settlement.presentation.screen.DisputeSettlementScreen
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.DisputeSettlementViewModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.DisputeSettlementUiAction
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.getKoin

@Composable
fun DisputeSettlementFeature(
    groupId: String,
    settlementId: String,
    disputeSettlementViewModel: DisputeSettlementViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by disputeSettlementViewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val koin = getKoin()
    val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }

    LaunchedEffect(groupId, settlementId) {
        disputeSettlementViewModel.setContext(groupId = groupId, settlementId = settlementId)
    }

    LaunchedEffect(Unit) {
        disputeSettlementViewModel.actions.collect { action ->
            when (action) {
                is DisputeSettlementUiAction.ShowError -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.ERROR
                    )
                }
                is DisputeSettlementUiAction.ShowSuccess -> {
                    val activity = context as? Activity
                    if (activity != null) {
                        interstitialAdManager.onActionCompleted(activity) {}
                    }
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.SUCCESS
                    )
                }
                DisputeSettlementUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    FeatureScaffold(
        currentRoute = Routes.DISPUTE_SETTLEMENT,
        modifier = modifier
    ) {
        DisputeSettlementScreen(
            uiState = uiState,
            onEvent = disputeSettlementViewModel::onEvent
        )
    }
}
