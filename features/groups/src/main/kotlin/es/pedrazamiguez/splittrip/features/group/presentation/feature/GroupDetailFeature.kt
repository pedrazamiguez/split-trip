package es.pedrazamiguez.splittrip.features.group.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupDetailScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.GroupDetailViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.GroupDetailUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun GroupDetailFeature(
    groupId: String,
    groupDetailViewModel: GroupDetailViewModel = koinViewModel<GroupDetailViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by groupDetailViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    LaunchedEffect(groupId) {
        groupDetailViewModel.setGroupId(groupId)
    }

    LaunchedEffect(Unit) {
        groupDetailViewModel.actions.collectLatest { action ->
            when (action) {
                is GroupDetailUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
            }
        }
    }

    GroupDetailScreen(
        uiState = uiState,
        isActiveGroup = selectedGroupId == groupId,
        onSelectGroup = {
            val group = uiState.group
            if (group != null) {
                if (selectedGroupId == groupId) {
                    if (!uiState.isOnlyGroup) {
                        sharedViewModel.selectGroup(null, null, null)
                    }
                } else {
                    sharedViewModel.selectGroup(group.id, group.name, group.currency)
                }
                navController.popBackStack()
            }
        },
        onManageSubunits = {
            navController.navigate(Routes.manageSubunitsRoute(groupId))
        },
        onEvent = groupDetailViewModel::onEvent
    )
}
