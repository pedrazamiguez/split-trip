package es.pedrazamiguez.splittrip.features.subunit.presentation.feature

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.subunit.presentation.screen.CreateEditSubunitScreen
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.CreateEditSubunitViewModel
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.action.CreateEditSubunitUiAction
import es.pedrazamiguez.splittrip.features.subunit.presentation.viewmodel.event.CreateEditSubunitUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CreateEditSubunitFeature(
    groupId: String,
    subunitId: String?,
    viewModel: CreateEditSubunitViewModel = koinViewModel<CreateEditSubunitViewModel>(),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>()
) {
    val navController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }

    // Intercept system back — delegate to wizard navigation
    BackHandler {
        viewModel.onEvent(CreateEditSubunitUiEvent.PreviousStep)
    }

    // Initialize ViewModel with route params
    LaunchedEffect(groupId, subunitId) {
        viewModel.init(groupId, subunitId)
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is CreateEditSubunitUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                is CreateEditSubunitUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                CreateEditSubunitUiAction.NavigateBack -> {
                    navController.popBackStack()
                }

                CreateEditSubunitUiAction.RequestExitConfirmation -> {
                    showExitConfirmation = true
                }

                CreateEditSubunitUiAction.NavigateToSubscriptions -> {
                    upgradeNavigator.navigateToUpgrade(rootNavController)
                }
            }
        }
    }

    if (showExitConfirmation) {
        DestructiveConfirmationDialog(
            title = stringResource(DesignSystemR.string.wizard_exit_dialog_title),
            text = stringResource(DesignSystemR.string.wizard_exit_dialog_message),
            confirmLabel = stringResource(DesignSystemR.string.wizard_exit_dialog_confirm),
            onConfirm = {
                showExitConfirmation = false
                navController.popBackStack()
            },
            onDismiss = { showExitConfirmation = false }
        )
    }

    CreateEditSubunitScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}
