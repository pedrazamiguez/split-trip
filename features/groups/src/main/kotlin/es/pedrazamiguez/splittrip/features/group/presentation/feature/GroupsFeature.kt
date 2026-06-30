package es.pedrazamiguez.splittrip.features.group.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalRootNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.screen.GroupsScreen
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.GroupsViewModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.GroupsUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun GroupsFeature(
    groupsViewModel: GroupsViewModel = koinViewModel<GroupsViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val navController = LocalTabNavController.current
    val rootNavController = LocalRootNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by groupsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        groupsViewModel.onEvent(GroupsUiEvent.LoadGroups)
    }

    // Auto-select the first/only group when there is exactly 1 group and none is selected
    LaunchedEffect(uiState.isLoading, uiState.groups, selectedGroupId) {
        if (!uiState.isLoading && uiState.groups.size == 1 && selectedGroupId == null) {
            val soleGroup = uiState.groups.first()
            sharedViewModel.selectGroup(soleGroup.id, soleGroup.name, soleGroup.currency)
        }
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        groupsViewModel.actions.collectLatest { action ->
            pillController.showPill(message = action.message.asString(context))
        }
    }

    GroupsScreen(
        uiState = uiState,
        selectedGroupId = selectedGroupId,
        onUpgradeClicked = {
            rootNavController.navigate(Routes.SETTINGS_ACCOUNT_STATUS)
        },
        onGroupClicked = { groupId, _, _ ->
            navController.navigate(Routes.groupDetailRoute(groupId))
        },
        onSelectGroup = { groupId, groupName, currency ->
            if (groupId != selectedGroupId) {
                sharedViewModel.selectGroup(groupId, groupName, currency)
            } else {
                sharedViewModel.selectGroup(null, null, null)
            }
        },
        onScrollPositionChanged = { index, offset ->
            groupsViewModel.onEvent(
                GroupsUiEvent.ScrollPositionChanged(index, offset)
            )
        },
        onDeleteGroup = { groupId ->
            // Clear selection if deleting the currently selected group
            if (groupId == selectedGroupId) {
                sharedViewModel.selectGroup(null, null, null)
            }
            groupsViewModel.onEvent(GroupsUiEvent.DeleteGroup(groupId))
        },
        onEditGroup = { groupId ->
            navController.navigate(Routes.editGroupRoute(groupId))
        },
        onManageSubunits = { groupId ->
            navController.navigate(Routes.manageSubunitsRoute(groupId))
        },
        onArchiveGroup = { groupId ->
            groupsViewModel.onEvent(GroupsUiEvent.ArchiveGroup(groupId))
        }
    )
}
