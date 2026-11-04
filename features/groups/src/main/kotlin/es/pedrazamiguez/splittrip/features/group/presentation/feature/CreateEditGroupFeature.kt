package es.pedrazamiguez.splittrip.features.group.presentation.feature

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
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestCameraPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.CreateEditGroupOverlays
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CreateEditGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.CreateEditGroupViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun CreateEditGroupFeature(
    groupId: String?,
    createEditGroupViewModel: CreateEditGroupViewModel = koinViewModel<CreateEditGroupViewModel>(),
    upgradeNavigator: UpgradeNavigator = koinInject<UpgradeNavigator>(),
    onSuccess: () -> Unit = {}
) {
    val state by createEditGroupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val navController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current

    var showScanner by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val scannerPermissionRequiredMessage = stringResource(R.string.scanner_permission_required)

    val requestCameraPermission = rememberRequestCameraPermission { isGranted ->
        if (isGranted) showScanner = true else pillController.showPill(scannerPermissionRequiredMessage)
    }

    BackHandler { createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.PreviousStep, onSuccess) }

    LaunchedEffect(groupId) { createEditGroupViewModel.init(groupId) }

    LaunchedEffect(Unit) {
        createEditGroupViewModel.actions.collectLatest { action ->
            when (action) {
                is CreateEditGroupUiAction.ShowSuccess -> pillController.showPill(action.message.asString(context))
                is CreateEditGroupUiAction.ShowError -> pillController.showPill(action.message.asString(context))
                CreateEditGroupUiAction.NavigateBack -> navController.popBackStack()
                CreateEditGroupUiAction.RequestExitConfirmation -> showExitConfirmation = true
                CreateEditGroupUiAction.NavigateToSubscriptions -> {
                    upgradeNavigator.navigateToUpgrade(rootNavController)
                }
                is CreateEditGroupUiAction.NavigateToRoute -> rootNavController.navigate(action.route)
            }
        }
    }

    CreateEditGroupOverlays(
        state = state,
        showScanner = showScanner,
        showExitConfirmation = showExitConfirmation,
        onDismissScanner = { showScanner = false },
        onMemberScanned = { userId, email ->
            showScanner = false
            createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.MemberScanned(userId, email), onSuccess)
        },
        onConfirmExit = {
            showExitConfirmation = false
            navController.popBackStack()
        },
        onDismissExit = { showExitConfirmation = false },
        onUpgrade = {
            createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.UpgradeClicked, onSuccess)
        },
        onDismissUpgrade = {
            createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.DismissUpgradeDialog, onSuccess)
        }
    )

    CreateEditGroupScreen(
        uiState = state,
        onScannerClick = requestCameraPermission,
        onEvent = { event -> createEditGroupViewModel.onEvent(event, onSuccess) }
    )
}
