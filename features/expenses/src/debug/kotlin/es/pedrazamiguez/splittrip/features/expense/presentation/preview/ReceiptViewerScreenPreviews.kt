package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ReceiptViewerScreen

@PreviewComplete
@Composable
private fun ReceiptViewerScreenPreview() {
    PreviewThemeWrapper {
        ReceiptViewerScreen(
            receiptUri = "android.resource://es.pedrazamiguez.splittrip/drawable/ic_launcher_foreground",
            onClose = {}
        )
    }
}
