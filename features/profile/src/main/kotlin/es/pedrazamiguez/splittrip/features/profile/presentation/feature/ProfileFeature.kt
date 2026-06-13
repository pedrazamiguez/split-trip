package es.pedrazamiguez.splittrip.features.profile.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.FeatureScaffold
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.ProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.ProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileFeature(profileViewModel: ProfileViewModel = koinViewModel<ProfileViewModel>()) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        profileViewModel.actions.collectLatest { action ->
            when (action) {
                is ProfileUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is ProfileUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }
            }
        }
    }

    FeatureScaffold(currentRoute = Routes.PROFILE) {
        ProfileScreen(
            uiState = uiState,
            onEvent = profileViewModel::onEvent
        )
    }
}
