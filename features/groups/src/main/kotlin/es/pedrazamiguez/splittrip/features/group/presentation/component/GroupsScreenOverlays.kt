package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

@Suppress("LongParameterList")
@Composable
internal fun GroupsScreenOverlays(
    selectedGroup: GroupUiModel?,
    selectedGroupId: String?,
    isSoleGroup: Boolean,
    currentUserId: String?,
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit,
    onEditGroup: (String) -> Unit,
    onManageSubunits: (String) -> Unit,
    onMenuDismiss: () -> Unit,
    onDeleteRequested: (GroupUiModel) -> Unit,
    onArchiveRequested: (GroupUiModel) -> Unit,
    onLeaveRequested: (GroupUiModel) -> Unit
) {
    selectedGroup?.let { group ->
        val isActive = group.id == selectedGroupId
        val selectAction = selectActionForGroup(
            group = group,
            isActive = isActive,
            isSoleGroup = isSoleGroup,
            onSelectGroup = onSelectGroup,
            onMenuDismiss = onMenuDismiss
        )
        val ownerActions = ownerActionsForGroup(
            group = group,
            currentUserId = currentUserId,
            onDeleteRequested = onDeleteRequested,
            onArchiveRequested = onArchiveRequested
        )
        val leaveAction = leaveActionForGroup(
            group = group,
            currentUserId = currentUserId,
            onLeaveRequested = onLeaveRequested,
            onMenuDismiss = onMenuDismiss
        )

        ActionBottomSheet(
            title = stringResource(R.string.group_actions_title, group.name),
            icon = TablerIcons.Outline.UsersGroup,
            actions = sheetActionsForGroup(
                group = group,
                selectAction = selectAction,
                ownerActions = ownerActions,
                leaveAction = leaveAction,
                onEditGroup = onEditGroup,
                onManageSubunits = onManageSubunits,
                onMenuDismiss = onMenuDismiss
            ),
            onDismiss = onMenuDismiss
        )
    }
}

@Composable
private fun selectActionForGroup(
    group: GroupUiModel,
    isActive: Boolean,
    isSoleGroup: Boolean,
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit,
    onMenuDismiss: () -> Unit
): SheetAction? {
    if (isActive && isSoleGroup) return null
    val text = if (isActive) {
        stringResource(
            R.string.action_deselect_group
        )
    } else {
        stringResource(R.string.action_select_active_group)
    }
    val icon = if (isActive) TablerIcons.Outline.X else TablerIcons.Outline.CircleCheck
    return SheetAction(
        text = text,
        icon = icon,
        onClick = {
            onSelectGroup(group.id, group.name, group.currency)
            onMenuDismiss()
        }
    )
}

@Composable
private fun ownerActionsForGroup(
    group: GroupUiModel,
    currentUserId: String?,
    onDeleteRequested: (GroupUiModel) -> Unit,
    onArchiveRequested: (GroupUiModel) -> Unit
): List<SheetAction> {
    if (group.status != GroupStatus.ACTIVE || group.createdBy != currentUserId) return emptyList()
    return listOf(
        SheetAction(
            text = stringResource(DesignSystemR.string.group_detail_end_trip),
            icon = TablerIcons.Outline.Lock,
            onClick = { onArchiveRequested(group) },
            isDestructive = true
        ),
        SheetAction(
            text = stringResource(R.string.action_delete_group),
            icon = TablerIcons.Outline.Trash,
            onClick = { onDeleteRequested(group) },
            isDestructive = true
        )
    )
}

@Composable
private fun leaveActionForGroup(
    group: GroupUiModel,
    currentUserId: String?,
    onLeaveRequested: (GroupUiModel) -> Unit,
    onMenuDismiss: () -> Unit
): SheetAction? {
    if (group.status != GroupStatus.ACTIVE || group.createdBy == currentUserId || currentUserId == null) return null
    return SheetAction(
        text = stringResource(R.string.action_leave_group),
        icon = TablerIcons.Outline.X,
        onClick = {
            onLeaveRequested(group)
            onMenuDismiss()
        },
        isDestructive = true
    )
}

@Composable
private fun sheetActionsForGroup(
    group: GroupUiModel,
    selectAction: SheetAction?,
    ownerActions: List<SheetAction>,
    leaveAction: SheetAction?,
    onEditGroup: (String) -> Unit,
    onManageSubunits: (String) -> Unit,
    onMenuDismiss: () -> Unit
): List<SheetAction> = listOfNotNull(
    selectAction,
    SheetAction(
        text = stringResource(R.string.action_edit_group),
        icon = TablerIcons.Outline.Edit,
        onClick = {
            onEditGroup(group.id)
            onMenuDismiss()
        }
    ),
    SheetAction(
        text = stringResource(R.string.action_manage_subunits),
        icon = TablerIcons.Outline.Sitemap,
        onClick = {
            onManageSubunits(group.id)
            onMenuDismiss()
        }
    ),
    leaveAction
) + ownerActions
