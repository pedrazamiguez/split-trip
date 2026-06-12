package es.pedrazamiguez.splittrip.features.authentication.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.features.authentication.presentation.component.RegisterCollisionDialog
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.screen.RegisterScreen
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.RegisterViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterFeature(
    viewModel: RegisterViewModel = koinViewModel<RegisterViewModel>(),
    onRegisterSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = LocalRootNavController.current

    LaunchedEffect(key1 = true) {
        viewModel.actions.collect { action ->
            when (action) {
                RegisterUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
                RegisterUiAction.RegisterSuccess -> {
                    onRegisterSuccess()
                }
            }
        }
    }

    RegisterScreen(
        uiState = uiState,
        onEvent = { event ->
            viewModel.onEvent(event)
        },
        onLoginClick = {
            navController.popBackStack()
        },
        onBackClick = {
            navController.popBackStack()
        }
    )

    if (uiState.showCollisionDialog) {
        RegisterCollisionDialog(
            onEvent = { event ->
                viewModel.onEvent(event)
            },
            onConfirmGoToLogin = {
                viewModel.onEvent(RegisterUiEvent.DismissCollisionDialog)
                navController.popBackStack()
            }
        )
    }
}
