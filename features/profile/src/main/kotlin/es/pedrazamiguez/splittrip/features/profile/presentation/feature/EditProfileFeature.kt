package es.pedrazamiguez.splittrip.features.profile.presentation.feature

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.profile.presentation.component.AvatarAttachmentHandler
import es.pedrazamiguez.splittrip.features.profile.presentation.component.AvatarCropOverlay
import es.pedrazamiguez.splittrip.features.profile.presentation.screen.EditProfileScreen
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.EditProfileViewModel
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.EditProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.EditProfileUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditProfileFeature(
    viewModel: EditProfileViewModel = koinViewModel<EditProfileViewModel>()
) {
    val pillController = LocalTopPillController.current
    val context = LocalContext.current
    val rootNavController = LocalRootNavController.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAvatarSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.actions.collectLatest { action ->
            when (action) {
                is EditProfileUiAction.ShowNotification -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                EditProfileUiAction.NavigateBack -> {
                    rootNavController.popBackStack()
                }
            }
        }
    }

    if (uiState.showCropOverlay && uiState.cropSourceUri != null) {
        Dialog(
            onDismissRequest = { viewModel.onEvent(EditProfileUiEvent.OnCropCancelled) },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AvatarCropOverlay(
                imageUri = uiState.cropSourceUri!!,
                onConfirm = { cropRect ->
                    viewModel.onEvent(EditProfileUiEvent.OnCropConfirmed(cropRect))
                },
                onCancel = {
                    viewModel.onEvent(EditProfileUiEvent.OnCropCancelled)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        EditProfileScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onAvatarClick = { showAvatarSourceSheet = true }
        )
    }

    AvatarAttachmentHandler(
        showSheet = showAvatarSourceSheet,
        showRemoveOption = uiState.avatarUrl != null || uiState.localAvatarPath != null,
        onDismissSheet = { showAvatarSourceSheet = false },
        onAvatarSelected = { uri, mimeType ->
            viewModel.onEvent(EditProfileUiEvent.OnAvatarPicked(uri, mimeType))
        },
        onAvatarRemoved = {
            viewModel.onEvent(EditProfileUiEvent.OnAvatarRemoved)
        }
    )
}
