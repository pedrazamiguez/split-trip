package es.pedrazamiguez.splittrip.features.onboarding.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.features.onboarding.presentation.screen.ReconciliationScreen
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.ReconciliationViewModel
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.ReconciliationUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.ReconciliationUiEvent
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReconciliationFeature(
    onReconciliationComplete: () -> Unit,
    viewModel: ReconciliationViewModel = koinViewModel()
) {
    val koin = getKoin()
    val authenticationService = remember(koin) { koin.get<AuthenticationService>() }
    val email = authenticationService.currentUserEmail().orEmpty()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    LaunchedEffect(viewModel.actions) {
        viewModel.actions.collect { action ->
            when (action) {
                ReconciliationUiAction.NavigationToNext -> {
                    onReconciliationComplete()
                }
                is ReconciliationUiAction.ShowError -> {
                    val messageStr = action.message.asString(context)
                    pillController.showPill(messageStr)
                }
            }
        }
    }

    ReconciliationScreen(
        uiState = uiState,
        email = email,
        onMigrateClick = {
            viewModel.onEvent(ReconciliationUiEvent.MigrateData)
        }
    )
}
