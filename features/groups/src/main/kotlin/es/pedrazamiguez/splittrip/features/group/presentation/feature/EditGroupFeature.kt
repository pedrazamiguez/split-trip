package es.pedrazamiguez.splittrip.features.group.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.group.presentation.screen.EditGroupScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.EditGroupViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.EditGroupUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditGroupFeature(
    groupId: String,
    modifier: Modifier = Modifier,
    editGroupViewModel: EditGroupViewModel = koinViewModel<EditGroupViewModel>()
) {
    val state by editGroupViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pillController = LocalTopPillController.current
    val navController = LocalTabNavController.current

    LaunchedEffect(groupId) {
        editGroupViewModel.initGroup(groupId)
    }

    LaunchedEffect(Unit) {
        editGroupViewModel.actions.collectLatest { action ->
            when (action) {
                is EditGroupUiAction.ShowNotification -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                EditGroupUiAction.NavigateBack -> {
                    navController.popBackStack()
                }
            }
        }
    }

    EditGroupScreen(
        uiState = state,
        onEvent = editGroupViewModel::onEvent,
        modifier = modifier
    )
}
