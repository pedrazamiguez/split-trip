package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesFilterScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpensesFilterViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesFilterUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesFilterUiEvent
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpensesFilterFeature(
    initialCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    onApplyFilters: (ExpenseFilterCriteria) -> Unit = {},
    onFiltersReset: (ExpenseFilterCriteria) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    filterViewModel: ExpensesFilterViewModel = koinViewModel(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val uiState by filterViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    LaunchedEffect(selectedGroupId) {
        filterViewModel.setSelectedGroup(selectedGroupId)
    }

    LaunchedEffect(initialCriteria) {
        filterViewModel.onEvent(ExpensesFilterUiEvent.Initialize(initialCriteria))
    }

    LaunchedEffect(Unit) {
        filterViewModel.actions.collectLatest { action ->
            when (action) {
                is ExpensesFilterUiAction.ApplyAndNavigateBack -> {
                    onApplyFilters(action.appliedCriteria)
                }

                is ExpensesFilterUiAction.FiltersReset -> {
                    onFiltersReset(action.clearedCriteria)
                }

                ExpensesFilterUiAction.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    ExpensesFilterScreen(
        uiState = uiState,
        onUpdateDraft = { criteria ->
            filterViewModel.onEvent(ExpensesFilterUiEvent.UpdateDraft(criteria))
        },
        onPresetSelected = { preset ->
            filterViewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(preset))
        },
        onResetFilters = {
            filterViewModel.onEvent(ExpensesFilterUiEvent.ResetDraft)
        },
        onApplyFilters = {
            filterViewModel.onEvent(ExpensesFilterUiEvent.ApplyFilters)
        }
    )
}
