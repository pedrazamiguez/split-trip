package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupsScreenContent
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupsScreenOverlays
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@Suppress("kotlin:S107")
@OptIn(FlowPreview::class)
@Composable
fun GroupsScreen(
    uiState: GroupsUiState = GroupsUiState(),
    selectedGroupId: String? = null,
    onGroupClicked: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit = { _, _, _ -> },
    onCreateGroupClick: () -> Unit = {},
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onDeleteGroup: (groupId: String) -> Unit = {},
    onManageSubunits: (groupId: String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var hasRestoredScroll by remember { mutableStateOf(false) }
    var selectedGroupForMenu by remember { mutableStateOf<GroupUiModel?>(null) }
    var groupToDelete by remember { mutableStateOf<GroupUiModel?>(null) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !hasRestoredScroll && uiState.groups.isNotEmpty()) {
            if (uiState.scrollPosition > 0 || uiState.scrollOffset > 0) {
                listState.scrollToItem(uiState.scrollPosition, uiState.scrollOffset)
            }
            hasRestoredScroll = true
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(UiConstants.SCROLL_POSITION_DEBOUNCE_MS)
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }

    val bottomPadding = LocalBottomPadding.current

    GroupsScreenContent(
        uiState = uiState,
        selectedGroupId = selectedGroupId,
        listState = listState,
        bottomPadding = bottomPadding,
        onCreateGroupClick = onCreateGroupClick,
        onGroupClicked = onGroupClicked,
        onGroupLongClicked = { selectedGroupForMenu = it }
    )

    GroupsScreenOverlays(
        selectedGroup = selectedGroupForMenu,
        selectedGroupId = selectedGroupId,
        onSelectGroup = onSelectGroup,
        onManageSubunits = onManageSubunits,
        onMenuDismiss = { selectedGroupForMenu = null },
        onDeleteRequested = { group ->
            groupToDelete = group
            selectedGroupForMenu = null
        }
    )

    groupToDelete?.let { group ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.group_delete_title),
            text = stringResource(R.string.group_delete_warning, group.name),
            onDismiss = { groupToDelete = null },
            onConfirm = {
                onDeleteGroup(group.id)
                groupToDelete = null
            }
        )
    }
}
