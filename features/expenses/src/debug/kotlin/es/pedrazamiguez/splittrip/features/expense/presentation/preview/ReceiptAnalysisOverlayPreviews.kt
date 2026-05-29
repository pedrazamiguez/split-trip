package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.receipt.ReceiptAnalysisOverlay

@PreviewComplete
@Composable
private fun ReceiptAnalysisOverlayVisiblePreview() {
    PreviewThemeWrapper {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            ReceiptAnalysisOverlay(visible = true)
        }
    }
}
