package es.pedrazamiguez.splittrip.features.settings.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.settings.presentation.screen.NotificationPreferencesScreen
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.NotificationPreferencesViewModel
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.NotificationPreferencesUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun NotificationPreferencesFeature(viewModel: NotificationPreferencesViewModel = koinViewModel()) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is NotificationPreferencesUiAction.ShowTopPill -> {
                    pillController.showPill(action.message.asString(context))
                }
            }
        }
    }

    FeatureScaffold(currentRoute = Routes.SETTINGS_NOTIFICATIONS) {
        NotificationPreferencesScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent
        )
    }
}
