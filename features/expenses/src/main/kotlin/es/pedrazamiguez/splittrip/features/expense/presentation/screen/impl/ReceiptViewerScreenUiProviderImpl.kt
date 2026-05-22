package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import androidx.compose.runtime.Composable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider

class ReceiptViewerScreenUiProviderImpl(
    override val route: String = Routes.RECEIPT_VIEWER
) : ScreenUiProvider {

    override val topBar: @Composable () -> Unit = {
        // Return an empty top bar to prevent statusBarsPadding in MainScreen's AnimatedTopBar
    }
}
