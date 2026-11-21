package es.pedrazamiguez.splittrip.core.designsystem.preview.sheet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.R
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.UsersGroup
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper

@PreviewComplete
@Composable
private fun ActionBottomSheetDefaultPreview() {
    PreviewThemeWrapper {
        ActionBottomSheet(
            title = stringResource(R.string.preview_sheet_group_title, "Shared House 2026"),
            icon = TablerIcons.Outline.UsersGroup,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.preview_action_edit_group),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {}
                ),
                SheetAction(
                    text = stringResource(R.string.preview_action_delete_group),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {},
                    isDestructive = true
                )
            ),
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ActionBottomSheetMultipleActionsPreview() {
    PreviewThemeWrapper {
        ActionBottomSheet(
            title = stringResource(R.string.preview_sheet_expense_title, "Dinner at restaurant"),
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.action_edit),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {}
                ),
                SheetAction(
                    text = stringResource(R.string.preview_action_share),
                    icon = TablerIcons.Outline.Share,
                    onClick = {}
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {},
                    isDestructive = true
                )
            ),
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun ActionBottomSheetWithDisabledActionPreview() {
    PreviewThemeWrapper {
        ActionBottomSheet(
            title = stringResource(R.string.preview_sheet_group_title, "Beach Vacation"),
            icon = TablerIcons.Outline.UsersGroup,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.preview_action_edit_group),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {},
                    enabled = false
                ),
                SheetAction(
                    text = stringResource(R.string.preview_action_delete_group),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {},
                    isDestructive = true
                )
            ),
            onDismiss = {}
        )
    }
}
