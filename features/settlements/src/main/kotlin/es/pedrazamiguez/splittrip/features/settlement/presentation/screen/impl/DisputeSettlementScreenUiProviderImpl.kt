package es.pedrazamiguez.splittrip.features.settlement.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.settlement.R

class DisputeSettlementScreenUiProviderImpl(
    override val route: String = Routes.DISPUTE_SETTLEMENT
) : ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        DynamicTopAppBar(
            title = stringResource(R.string.your_balance_dispute_dialog_title),
            onBack = { navController.popBackStack() }
        )
    }
}
