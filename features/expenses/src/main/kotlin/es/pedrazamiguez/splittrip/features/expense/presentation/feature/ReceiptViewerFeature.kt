package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ReceiptViewerScreen

@Composable
fun ReceiptViewerFeature(
    receiptUri: String,
    mimeType: String?,
    modifier: Modifier = Modifier
) {
    val navController = LocalTabNavController.current

    ReceiptViewerScreen(
        receiptUri = receiptUri,
        mimeType = mimeType,
        onClose = { navController.popBackStack() },
        modifier = modifier
    )
}
