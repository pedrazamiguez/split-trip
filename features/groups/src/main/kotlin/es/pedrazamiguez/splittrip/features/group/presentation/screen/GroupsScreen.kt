package es.pedrazamiguez.splittrip.features.group.presentation.screen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.StickyActionBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.component.GroupItem
import es.pedrazamiguez.splittrip.features.group.presentation.component.SelectedGroupCard
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.GroupsUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

// Compose Screen pattern: all 8 parameters carry defaults for Preview compatibility.
// S107 fires because SonarQube counts parameters unconditionally, unlike detekt's
// LongParameterList which is already configured with ignoreDefaultParameters = true
// and ignoreAnnotatedParameter = ['Composable']. This suppression is intentional.
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

    GroupsScrollEffects(
        uiState = uiState,
        listState = listState,
        hasRestoredScroll = hasRestoredScroll,
        onScrollPositionChanged = onScrollPositionChanged,
        onScrollRestored = { hasRestoredScroll = true }
    )

    GroupsScreenContent(
        uiState = uiState,
        selectedGroupId = selectedGroupId,
        listState = listState,
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

@OptIn(FlowPreview::class)
@Composable
private fun GroupsScrollEffects(
    uiState: GroupsUiState,
    listState: LazyListState,
    hasRestoredScroll: Boolean,
    onScrollPositionChanged: (Int, Int) -> Unit,
    onScrollRestored: () -> Unit
) {
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !hasRestoredScroll && uiState.groups.isNotEmpty()) {
            if (uiState.scrollPosition > 0 || uiState.scrollOffset > 0) {
                listState.scrollToItem(uiState.scrollPosition, uiState.scrollOffset)
            }
            onScrollRestored()
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(UiConstants.SCROLL_POSITION_DEBOUNCE_MS)
            .collect { (index, offset) -> onScrollPositionChanged(index, offset) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GroupsScreenContent(
    uiState: GroupsUiState,
    selectedGroupId: String?,
    listState: LazyListState,
    onCreateGroupClick: () -> Unit,
    onGroupClicked: (String, String, String) -> Unit,
    onGroupLongClicked: (GroupUiModel) -> Unit
) {
    val bottomPadding = LocalBottomPadding.current

    Box(modifier = Modifier.fillMaxSize()) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = { ShimmerLoadingList() }
        ) {
            when {
                uiState.groups.isEmpty() -> {
                    EmptyStateView(
                        title = stringResource(R.string.groups_not_found),
                        icon = TablerIcons.Outline.UsersGroup
                    )
                }
                else -> {
                    GroupsListContent(
                        groups = uiState.groups,
                        selectedGroupId = selectedGroupId,
                        listState = listState,
                        bottomPadding = bottomPadding,
                        onGroupClicked = onGroupClicked,
                        onGroupLongClicked = onGroupLongClicked
                    )
                }
            }
        }
        StickyActionBar(
            text = stringResource(R.string.groups_create),
            icon = TablerIcons.Outline.Plus,
            onClick = onCreateGroupClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(bottom = bottomPadding + MaterialTheme.spacing.ExtraSmall),
            sharedTransitionKey = CREATE_GROUP_SHARED_ELEMENT_KEY
        )
    }
}

@Composable
private fun GroupsScreenOverlays(
    selectedGroup: GroupUiModel?,
    selectedGroupId: String?,
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit,
    onManageSubunits: (String) -> Unit,
    onMenuDismiss: () -> Unit,
    onDeleteRequested: (GroupUiModel) -> Unit
) {
    selectedGroup?.let { group ->
        val isActive = group.id == selectedGroupId
        val selectActionText = if (isActive) {
            stringResource(R.string.action_deselect_group)
        } else {
            stringResource(R.string.action_select_active_group)
        }
        val selectActionIcon = if (isActive) TablerIcons.Outline.X else TablerIcons.Outline.CircleCheck

        ActionBottomSheet(
            title = stringResource(R.string.group_actions_title, group.name),
            icon = TablerIcons.Outline.UsersGroup,
            actions = listOf(
                SheetAction(
                    text = selectActionText,
                    icon = selectActionIcon,
                    onClick = {
                        onSelectGroup(group.id, group.name, group.currency)
                        onMenuDismiss()
                    }
                ),
                SheetAction(
                    text = stringResource(R.string.action_edit_group),
                    icon = TablerIcons.Outline.Edit,
                    onClick = { onMenuDismiss() }
                ),
                SheetAction(
                    text = stringResource(R.string.action_manage_subunits),
                    icon = TablerIcons.Outline.Sitemap,
                    onClick = {
                        onManageSubunits(group.id)
                        onMenuDismiss()
                    }
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete_group),
                    icon = TablerIcons.Outline.Trash,
                    onClick = { onDeleteRequested(group) },
                    isDestructive = true
                )
            ),
            onDismiss = onMenuDismiss
        )
    }
}

@Composable
private fun GroupsListHeader() {
    Column {
        Text(
            text = stringResource(R.string.groups_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.groups_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun GroupsListContent(
    groups: ImmutableList<GroupUiModel>,
    selectedGroupId: String?,
    listState: LazyListState,
    bottomPadding: Dp,
    onGroupClicked: (String, String, String) -> Unit,
    onGroupLongClicked: (GroupUiModel) -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val fabExtraPadding = 72.dp // Space for StickyActionBar

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val unselectedGroups = groups.filter { it.id != selectedGroupId }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.Default,
            end = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + bottomPadding + fabExtraPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        item(key = "header") { GroupsListHeader() }

        if (selectedGroup != null) {
            item(key = "selected-${selectedGroup.id}") {
                SelectedGroupCard(
                    groupUiModel = selectedGroup,
                    modifier = Modifier
                        // Disable alpha fade-in/out for the hero card. animateItem()'s default
                        // alpha animation creates a rectangular offscreen hardware buffer
                        // (Android's alpha compositing layer). FlatCard's shadow uses
                        // graphicsLayer { clip = false } to let the rounded ambient shadow
                        // bleed outside its bounds — but that bleed is silently clipped by the
                        // rectangular buffer edge, producing a hard squared shadow artefact.
                        // Removing the fade eliminates the buffer entirely; the spring placement
                        // animation is retained for smooth repositioning.
                        .animateItem(fadeInSpec = null, fadeOutSpec = null)
                        .sharedElementAnimation(
                            key = "group-${selectedGroup.id}",
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    onClick = onGroupClicked,
                    onLongClick = { onGroupLongClicked(selectedGroup) }
                )
            }
        }

        items(items = unselectedGroups, key = { it.id }) { group ->
            GroupItem(
                modifier = Modifier
                    .animateItem()
                    .sharedElementAnimation(
                        key = "group-${group.id}",
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    ),
                groupUiModel = group,
                onClick = onGroupClicked,
                onLongClick = { onGroupLongClicked(group) }
            )
        }
    }
}
