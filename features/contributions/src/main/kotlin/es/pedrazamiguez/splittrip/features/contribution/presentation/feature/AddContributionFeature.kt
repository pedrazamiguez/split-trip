package es.pedrazamiguez.splittrip.features.contribution.presentation.feature

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
import es.pedrazamiguez.splittrip.features.contribution.presentation.screen.AddContributionScreen
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.AddContributionViewModel
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.action.AddContributionUiAction
import es.pedrazamiguez.splittrip.features.contribution.presentation.viewmodel.event.AddContributionUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun AddContributionFeature(
    groupId: String,
    contributionId: String?,
    addContributionViewModel: AddContributionViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    ),
    onContributionSuccess: () -> Unit = {}
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val navController = LocalTabNavController.current

    val uiState by addContributionViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupCurrency by sharedViewModel.selectedGroupCurrency.collectAsStateWithLifecycle()

    var showExitConfirmation by remember { mutableStateOf(false) }

    // Set group context — currency is applied synchronously, config loaded async.
    LaunchedEffect(groupId, contributionId, selectedGroupCurrency) {
        addContributionViewModel.setGroupContext(groupId, contributionId, selectedGroupCurrency)
    }

    // Intercept system back — delegate to wizard navigation
    BackHandler {
        addContributionViewModel.onEvent(AddContributionUiEvent.PreviousStep)
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        addContributionViewModel.actions.collectLatest { action ->
            when (action) {
                is AddContributionUiAction.ShowSuccess -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.SUCCESS
                    )
                }

                is AddContributionUiAction.ShowError -> {
                    pillController.showPill(
                        message = action.message.asString(context),
                        style = TopPillStyle.ERROR
                    )
                }

                AddContributionUiAction.NavigateBack -> {
                    navController.popBackStack()
                }

                AddContributionUiAction.RequestExitConfirmation -> {
                    showExitConfirmation = true
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

    AddContributionScreen(
        uiState = uiState,
        onEvent = { event ->
            addContributionViewModel.onEvent(event, onContributionSuccess)
        }
    )
}
