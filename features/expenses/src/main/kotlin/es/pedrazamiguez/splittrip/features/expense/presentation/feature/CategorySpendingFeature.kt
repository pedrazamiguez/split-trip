package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.CategorySpendingScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.CategorySpendingViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.CategorySpendingUiAction
import org.koin.androidx.compose.koinViewModel

@Composable
fun CategorySpendingFeature(
    filterCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    viewModel: CategorySpendingViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()
    val tabNavController = LocalTabNavController.current

    // Set filterCriteria before selectedGroupId so initial emission uses the active filters without flashing.
    LaunchedEffect(filterCriteria) {
        viewModel.setFilterCriteria(filterCriteria)
    }

    LaunchedEffect(selectedGroupId) {
        viewModel.setSelectedGroup(selectedGroupId)
    }

    LaunchedEffect(viewModel) {
        viewModel.actions.collect { action ->
            when (action) {
                CategorySpendingUiAction.NavigateBack -> tabNavController.navigateUp()
            }
        }
    }

    CategorySpendingScreen(uiState = uiState)
}
