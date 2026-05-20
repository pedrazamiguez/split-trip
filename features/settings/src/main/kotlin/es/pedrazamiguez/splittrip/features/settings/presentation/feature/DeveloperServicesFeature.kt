package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperServicesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperServicesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperServicesFeature(
    viewModel: DeveloperServicesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalRootNavController.current

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEVELOPER_SERVICES) {
        DeveloperServicesScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onBackClick = { navController.popBackStack() }
        )
    }
}
