package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CircleCheck
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Sitemap
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

@Suppress("LongParameterList")
@Composable
internal fun GroupsScreenOverlays(
    selectedGroup: GroupUiModel?,
    selectedGroupId: String?,
    isSoleGroup: Boolean,
    onSelectGroup: (groupId: String, groupName: String, currency: String) -> Unit,
    onEditGroup: (String) -> Unit,
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

        val selectAction = if (isActive && isSoleGroup) {
            null
        } else {
            SheetAction(
                text = selectActionText,
                icon = selectActionIcon,
                onClick = {
                    onSelectGroup(group.id, group.name, group.currency)
                    onMenuDismiss()
                }
            )
        }

        ActionBottomSheet(
            title = stringResource(R.string.group_actions_title, group.name),
            icon = TablerIcons.Outline.UsersGroup,
            actions = listOfNotNull(
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
