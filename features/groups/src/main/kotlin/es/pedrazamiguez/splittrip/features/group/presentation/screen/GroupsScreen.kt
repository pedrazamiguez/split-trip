package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.rememberConnectedScrollBehavior
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupsScreenContent
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupsScreenOverlays
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@Suppress("kotlin:S107")
@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    uiState: GroupsUiState = GroupsUiState(),
    selectedGroupId: String? = null,
    onUpgradeClicked: () -> Unit = {},
    onGroupClicked: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onEditGroup: (groupId: String) -> Unit = {},
    onDeleteGroup: (groupId: String) -> Unit = {},
    onManageSubunits: (groupId: String) -> Unit = {},
    onArchiveGroup: (groupId: String) -> Unit = {},
    onLeaveGroup: (groupId: String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var hasRestoredScroll by remember { mutableStateOf(false) }
    var selectedGroupForMenu by remember { mutableStateOf<GroupUiModel?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupUiModel?>(null) }
    var groupToArchive by remember { mutableStateOf<GroupUiModel?>(null) }
    var groupToLeave by remember { mutableStateOf<GroupUiModel?>(null) }
    val scrollBehavior = rememberConnectedScrollBehavior()

    RestoreScrollEffect(listState, uiState)

    TrackScrollEffect(listState, onScrollPositionChanged)

    val bottomPadding = LocalBottomPadding.current

    GroupsScreenContent(
        uiState = uiState,
        selectedGroupId = selectedGroupId,
        listState = listState,
        bottomPadding = bottomPadding,
        onGroupClicked = onGroupClicked,
        onGroupLongClicked = { selectedGroupForMenu = it },
        onUpgradeClicked = onUpgradeClicked,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    )

    GroupsScreenOverlays(
        selectedGroup = selectedGroupForMenu,
        selectedGroupId = selectedGroupId,
        isSoleGroup = uiState.groups.size == 1,
        currentUserId = uiState.currentUserId,
        onSelectGroup = onSelectGroup,
        onEditGroup = onEditGroup,
        onManageSubunits = onManageSubunits,
        onMenuDismiss = { selectedGroupForMenu = null },
        onDeleteRequested = { group ->
            groupToDelete = group
            selectedGroupForMenu = null
        },
        onArchiveRequested = { group ->
            groupToArchive = group
            selectedGroupForMenu = null
        },
        onLeaveRequested = { group ->
            groupToLeave = group
            selectedGroupForMenu = null
        }
    )

    DeleteConfirmationDialog(groupToDelete, onDeleteGroup) { groupToDelete = null }

    ArchiveConfirmationDialog(groupToArchive, onArchiveGroup) { groupToArchive = null }

    LeaveConfirmationDialog(groupToLeave, onLeaveGroup) { groupToLeave = null }
}

@Composable
private fun RestoreScrollEffect(listState: LazyListState, uiState: GroupsUiState) {
    var hasRestoredScroll by remember { mutableStateOf(false) }
    val groups = uiState.groups
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !hasRestoredScroll && groups.isNotEmpty()) {
            if (uiState.scrollPosition > 0 || uiState.scrollOffset > 0) {
                listState.scrollToItem(uiState.scrollPosition, uiState.scrollOffset)
            }
            hasRestoredScroll = true
        }
    }
}

@Composable
private fun TrackScrollEffect(
    listState: LazyListState,
    onScrollPositionChanged: (Int, Int) -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(UiConstants.SCROLL_POSITION_DEBOUNCE_MS)
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    groupToDelete: GroupUiModel?,
    onDeleteGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    groupToDelete?.let { group ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.group_delete_title),
            text = stringResource(R.string.group_delete_warning, group.name),
            onDismiss = onDismiss,
            onConfirm = {
                onDeleteGroup(group.id)
                onDismiss()
            }
        )
    }
}

@Composable
private fun ArchiveConfirmationDialog(
    groupToArchive: GroupUiModel?,
    onArchiveGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    groupToArchive?.let { group ->
        DestructiveConfirmationDialog(
            title = stringResource(DesignSystemR.string.group_detail_end_trip_title),
            text = stringResource(DesignSystemR.string.group_detail_end_trip_message, group.name),
            confirmLabel = stringResource(DesignSystemR.string.group_detail_end_trip_confirm),
            onDismiss = onDismiss,
            onConfirm = {
                onArchiveGroup(group.id)
                onDismiss()
            }
        )
    }
}

@Composable
private fun LeaveConfirmationDialog(
    groupToLeave: GroupUiModel?,
    onLeaveGroup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    groupToLeave?.let { group ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.group_leave_title),
            text = stringResource(R.string.group_leave_warning, group.name),
            onDismiss = onDismiss,
            onConfirm = {
                onLeaveGroup(group.id)
                onDismiss()
            }
        )
    }
}
