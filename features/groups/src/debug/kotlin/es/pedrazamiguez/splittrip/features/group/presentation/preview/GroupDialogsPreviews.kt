package es.pedrazamiguez.splittrip.features.group.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.group.presentation.component.DeleteConfirmationDialog
import es.pedrazamiguez.splittrip.features.group.presentation.component.LeaveConfirmationDialog
import es.pedrazamiguez.splittrip.features.group.presentation.component.QrScannerDialog
import es.pedrazamiguez.splittrip.features.group.presentation.model.GroupUiModel

@PreviewComplete
@Composable
private fun DeleteConfirmationDialogPreview() {
    PreviewThemeWrapper {
        DeleteConfirmationDialog(
            groupToDelete = GroupUiModel(
                id = "1",
                name = "Summer Trip"
            ),
            onDeleteGroup = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun LeaveConfirmationDialogPreview() {
    PreviewThemeWrapper {
        LeaveConfirmationDialog(
            groupToLeave = GroupUiModel(
                id = "1",
                name = "Summer Trip"
            ),
            onLeaveGroup = {},
            onDismiss = {}
        )
    }
}

@PreviewComplete
@Composable
private fun QrScannerDialogPreview() {
    PreviewThemeWrapper {
        QrScannerDialog(
            onDismissRequest = {},
            onScanned = {}
        )
    }
}
