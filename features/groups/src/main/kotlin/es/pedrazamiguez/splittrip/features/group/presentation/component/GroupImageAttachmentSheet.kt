package es.pedrazamiguez.splittrip.features.group.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Camera
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Photo
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.group.R

@Composable
internal fun GroupImageAttachmentSheet(
    showSheet: Boolean,
    showRemoveOption: Boolean,
    onDismissSheet: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onImageRemoved: () -> Unit
) {
    if (!showSheet) return

    val actions = buildList {
        add(
            SheetAction(
                text = stringResource(R.string.group_image_sheet_camera),
                icon = TablerIcons.Outline.Camera,
                onClick = onCameraClick
            )
        )
        add(
            SheetAction(
                text = stringResource(R.string.group_image_sheet_gallery),
                icon = TablerIcons.Outline.Photo,
                onClick = onGalleryClick
            )
        )
        if (showRemoveOption) {
            add(
                SheetAction(
                    text = stringResource(R.string.group_image_sheet_remove),
                    icon = TablerIcons.Outline.Trash,
                    onClick = onImageRemoved
                )
            )
        }
    }

    ActionBottomSheet(
        title = stringResource(R.string.group_image_sheet_title),
        icon = TablerIcons.Outline.Camera,
        actions = actions,
        onDismiss = onDismissSheet
    )
}
