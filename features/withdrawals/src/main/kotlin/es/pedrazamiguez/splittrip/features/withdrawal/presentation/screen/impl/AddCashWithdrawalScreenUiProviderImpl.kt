package es.pedrazamiguez.splittrip.features.withdrawal.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.withdrawal.R
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.AddCashWithdrawalViewModel
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.event.AddCashWithdrawalUiEvent
import org.koin.androidx.compose.koinViewModel

class AddCashWithdrawalScreenUiProviderImpl(override val route: String = Routes.ADD_CASH_WITHDRAWAL) :
    ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()

        val backStackEntry = navController.currentBackStackEntry
        val vm: AddCashWithdrawalViewModel? = backStackEntry?.let {
            koinViewModel(viewModelStoreOwner = it)
        }

        DynamicTopAppBar(
            title = stringResource(R.string.withdrawal_cash_title),
            subtitle = groupName,
            onBack = { vm?.onEvent(AddCashWithdrawalUiEvent.PreviousStep) ?: navController.popBackStack() },
            onBackLongPress = { vm?.onEvent(AddCashWithdrawalUiEvent.CloseWizard) }
        )
    }
}
