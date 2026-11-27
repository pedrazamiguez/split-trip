package es.pedrazamiguez.splittrip.features.group.presentation.feature

import android.content.Context
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
import androidx.navigation.NavController
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.UpgradeNavigator
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestCameraPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillStyle
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
        if (isGranted) {
            showScanner = true
        } else {
            pillController.showPill(scannerPermissionRequiredMessage, TopPillStyle.WARNING)
        }
    }

    BackHandler { createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.PreviousStep, onSuccess) }

    LaunchedEffect(groupId) { createEditGroupViewModel.init(groupId) }

    LaunchedEffect(Unit) {
        createEditGroupViewModel.actions.collectLatest { action ->
            handleCreateEditGroupAction(
                action = action,
                context = context,
                pillController = pillController,
                navController = navController,
                rootNavController = rootNavController,
                upgradeNavigator = upgradeNavigator,
                onRequestExitConfirmation = { showExitConfirmation = true }
            )
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
            createEditGroupViewModel.onEvent(CreateEditGroupUiEvent.DismissProPreview, onSuccess)
        }
    )

    CreateEditGroupScreen(
        uiState = state,
        onScannerClick = requestCameraPermission,
        onEvent = { event -> createEditGroupViewModel.onEvent(event, onSuccess) }
    )
}

@Suppress("LongParameterList")
private fun handleCreateEditGroupAction(
    action: CreateEditGroupUiAction,
    context: Context,
    pillController: TopPillController,
    navController: NavController,
    rootNavController: NavController,
    upgradeNavigator: UpgradeNavigator,
    onRequestExitConfirmation: () -> Unit
) {
    when (action) {
        is CreateEditGroupUiAction.ShowSuccess -> pillController.showPill(
            message = action.message.asString(context),
            style = TopPillStyle.SUCCESS
        )
        is CreateEditGroupUiAction.ShowError -> pillController.showPill(
            message = action.message.asString(context),
            style = TopPillStyle.ERROR
        )
        CreateEditGroupUiAction.NavigateBack -> navController.popBackStack()
        CreateEditGroupUiAction.RequestExitConfirmation -> onRequestExitConfirmation()
        is CreateEditGroupUiAction.NavigateToSubscriptions -> {
            upgradeNavigator.navigateToUpgrade(rootNavController, source = action.entrySource)
        }
        is CreateEditGroupUiAction.NavigateToRoute -> rootNavController.navigate(action.route)
    }
}
