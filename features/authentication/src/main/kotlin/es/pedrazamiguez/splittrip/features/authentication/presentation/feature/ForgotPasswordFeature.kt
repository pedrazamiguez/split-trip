package es.pedrazamiguez.splittrip.features.authentication.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.ForgotPasswordScreen
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.ForgotPasswordViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ForgotPasswordFeature(
    viewModel: ForgotPasswordViewModel = koinViewModel<ForgotPasswordViewModel>()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalRootNavController.current
    val pillController = LocalTopPillController.current

    val successMessage = stringResource(id = R.string.forgot_password_success_message)

    LaunchedEffect(key1 = viewModel.uiAction) {
        viewModel.uiAction.collectLatest { action ->
            when (action) {
                ForgotPasswordUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    LaunchedEffect(key1 = uiState.isSuccess) {
        if (uiState.isSuccess) {
            pillController.showPill(successMessage)
            navController.popBackStack()
        }
    }

    ForgotPasswordScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}
