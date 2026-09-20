package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.dialog.ResetFiltersConfirmationDialog

@PreviewComplete
@Composable
private fun ResetFiltersConfirmationDialogPreview() {
    PreviewThemeWrapper {
        ResetFiltersConfirmationDialog(
            onDismiss = {},
            onConfirm = {}
        )
    }
}
