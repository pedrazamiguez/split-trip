package es.pedrazamiguez.splittrip.features.group.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.permission.rememberRequestCameraPermission
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.QrScannerDialog
import es.pedrazamiguez.splittrip.features.group.presentation.screen.CreateGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.CreateGroupViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun CreateGroupFeature(
    createGroupViewModel: CreateGroupViewModel = koinViewModel<CreateGroupViewModel>(),
    onCreateGroupSuccess: () -> Unit = {}
) {
    val state by createGroupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val navController = LocalTabNavController.current

    var showScanner by remember { mutableStateOf(false) }

    val requestCameraPermission = rememberRequestCameraPermission { isGranted ->
        if (isGranted) {
            showScanner = true
        } else {
            pillController.showPill(context.getString(R.string.scanner_permission_required))
        }
    }

    LaunchedEffect(Unit) {
        createGroupViewModel.actions.collectLatest { action ->
            when (action) {
                is CreateGroupUiAction.ShowSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                is CreateGroupUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                CreateGroupUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    if (showScanner) {
        QrScannerDialog(
            onDismissRequest = { showScanner = false },
            onScanned = { payload ->
                showScanner = false
                createGroupViewModel.onEvent(
                    CreateGroupUiEvent.MemberScanned(payload.userId, payload.email),
                    onCreateGroupSuccess
                )
            }
        )
    }

    CreateGroupScreen(
        uiState = state,
        onScannerClick = requestCameraPermission,
        onEvent = { event ->
            createGroupViewModel.onEvent(
                event,
                onCreateGroupSuccess
            )
        }
    )
}
