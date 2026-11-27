package es.pedrazamiguez.splittrip.features.withdrawal.presentation.feature

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen.AddCashWithdrawalScreen
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.AddCashWithdrawalViewModel
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddCashWithdrawalFeature(
    addCashWithdrawalViewModel: AddCashWithdrawalViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onWithdrawalSuccess: () -> Unit = {}
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val navController = LocalTabNavController.current

    val uiState by addCashWithdrawalViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    var showExitConfirmation by remember { mutableStateOf(false) }

    // Intercept system back — delegate to wizard navigation
    BackHandler {
        addCashWithdrawalViewModel.onEvent(AddCashWithdrawalUiEvent.PreviousStep)
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        addCashWithdrawalViewModel.actions.collectLatest { action ->
            when (action) {
                is AddCashWithdrawalUiAction.ShowError -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.ERROR
                    )
                }

                is AddCashWithdrawalUiAction.ShowPill -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.WARNING
                    )
                }

                AddCashWithdrawalUiAction.NavigateBack -> {
                    navController.popBackStack()
                }

                AddCashWithdrawalUiAction.RequestExitConfirmation -> {
                    showExitConfirmation = true
                }

                AddCashWithdrawalUiAction.None -> { /* no-op */ }
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

    AddCashWithdrawalScreen(
        groupId = selectedGroupId,
        uiState = uiState,
        onEvent = { event ->
            addCashWithdrawalViewModel.onEvent(event, onWithdrawalSuccess)
        }
    )
}
