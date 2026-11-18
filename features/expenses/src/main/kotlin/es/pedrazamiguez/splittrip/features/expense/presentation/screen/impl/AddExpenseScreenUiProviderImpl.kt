package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

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
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.AddExpenseViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class AddExpenseScreenUiProviderImpl(override val route: String = Routes.ADD_EXPENSE) : ScreenUiProvider {

    override val showBottomBar: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        val sharedViewModel: SharedViewModel = koinViewModel(
            viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
        )
        val groupName by sharedViewModel.selectedGroupName.collectAsStateWithLifecycle()

        val backStackEntry = navController.currentBackStackEntry
        var vm: AddExpenseViewModel? = null
        val title = if (backStackEntry != null) {
            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val resolvedVm: AddExpenseViewModel = koinViewModel(
                viewModelStoreOwner = backStackEntry,
                parameters = { parametersOf(expenseId) }
            )
            vm = resolvedVm
            val uiState by resolvedVm.uiState.collectAsStateWithLifecycle()
            stringResource(uiState.screenTitleRes)
        } else {
            stringResource(R.string.expenses_add)
        }

        DynamicTopAppBar(
            title = title,
            subtitle = groupName,
            onBack = { vm?.onEvent(AddExpenseUiEvent.PreviousStep) ?: navController.popBackStack() },
            onBackLongPress = { vm?.onEvent(AddExpenseUiEvent.CloseWizard) }
        )
    }
}
