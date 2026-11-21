package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.DeveloperInfoScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.DeveloperInfoViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun DeveloperInfoFeature(
    viewModel: DeveloperInfoViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    FeatureScaffold(currentRoute = Routes.SETTINGS_DEVELOPER_INFO) {
        DeveloperInfoScreen(
            uiState = uiState,
            onLinkClick = { url ->
                if (url.isNotBlank()) {
                    uriHandler.openUri(url)
                }
            }
        )
    }
}
