package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.PlanAllowancesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.PlanAllowancesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.PlanAllowancesUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun PlanAllowancesFeature(
    navController: NavHostController = LocalRootNavController.current,
    viewModel: PlanAllowancesViewModel = koinViewModel<PlanAllowancesViewModel>(),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                PlanAllowancesUiAction.NavigateToUpgrade -> {
                    upgradeNavigator.navigateToUpgrade(
                        navController = navController,
                        source = MonetizationTelemetry.EntrySource.PLAN_ALLOWANCES
                    )
                }
            }
        }
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_PLAN_ALLOWANCES) {
        PlanAllowancesScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}
