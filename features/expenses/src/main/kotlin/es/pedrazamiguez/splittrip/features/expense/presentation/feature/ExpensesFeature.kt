package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.TopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.navigation.KEY_APPLIED_FILTER_CRITERIA
import es.pedrazamiguez.splittrip.features.expense.navigation.KEY_EXPENSE_ADDED
import es.pedrazamiguez.splittrip.features.expense.navigation.KEY_RESET_FILTERS_REQUESTED
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpensesViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesUiEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Suppress("LongMethod")
@Composable
fun ExpensesFeature(
    backStackEntry: NavBackStackEntry? = null,
    expensesViewModel: ExpensesViewModel = koinViewModel<ExpensesViewModel>(),
    sharedViewModel: SharedViewModel = koinViewModel(
        viewModelStoreOwner = LocalContext.current as ViewModelStoreOwner
    )
) {
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by expensesViewModel.uiState.collectAsStateWithLifecycle()
    val selectedGroupId by sharedViewModel.selectedGroupId.collectAsStateWithLifecycle()

    LaunchedEffect(selectedGroupId) {
        expensesViewModel.setSelectedGroup(selectedGroupId)
    }

    // Collect and handle UiActions
    LaunchedEffect(Unit) {
        expensesViewModel.actions.collectLatest { action ->
            handleExpenseUiAction(action, pillController, context)
        }
    }

    val expensesBackStackEntry = backStackEntry ?: navController.currentBackStackEntry

    LaunchedEffect(expensesBackStackEntry, uiState.filterCriteria) {
        expensesBackStackEntry?.savedStateHandle?.set(
            Routes.KEY_EXPENSE_FILTER_CRITERIA,
            uiState.filterCriteria
        )
    }

    LaunchedEffect(expensesBackStackEntry) {
        expensesBackStackEntry?.savedStateHandle?.getStateFlow(KEY_EXPENSE_ADDED, false)?.collectLatest { added ->
            if (added) {
                expensesViewModel.onEvent(ExpensesUiEvent.ExpenseAdded)
                expensesBackStackEntry.savedStateHandle.remove<Boolean>(KEY_EXPENSE_ADDED)
            }
        }
    }

    LaunchedEffect(expensesBackStackEntry) {
        expensesBackStackEntry?.savedStateHandle?.getStateFlow<ExpenseFilterCriteria?>(
            KEY_APPLIED_FILTER_CRITERIA,
            null
        )?.collectLatest { appliedCriteria ->
            if (appliedCriteria != null) {
                expensesViewModel.onEvent(ExpensesUiEvent.FilterCriteriaChanged(appliedCriteria))
                expensesBackStackEntry.savedStateHandle.remove<ExpenseFilterCriteria>(KEY_APPLIED_FILTER_CRITERIA)
            }
        }
    }

    LaunchedEffect(expensesBackStackEntry) {
        expensesBackStackEntry?.savedStateHandle?.getStateFlow(
            KEY_RESET_FILTERS_REQUESTED,
            false
        )?.collectLatest { resetRequested ->
            if (resetRequested) {
                expensesViewModel.onEvent(ExpensesUiEvent.ClearFilters)
                expensesBackStackEntry.savedStateHandle.remove<Boolean>(KEY_RESET_FILTERS_REQUESTED)
            }
        }
    }

    // Prevent stale data flash during group transition
    val isTransitioning = selectedGroupId != null && selectedGroupId != uiState.groupId
    val effectiveUiState = remember(uiState, isTransitioning) {
        if (isTransitioning) {
            uiState.copy(isLoading = true, expenseGroups = persistentListOf())
        } else {
            uiState
        }
    }

    ExpensesScreen(
        uiState = effectiveUiState,
        actions = expensesViewModel.actions,
        onExpenseClicked = { expenseId ->
            navController.navigate(Routes.expenseDetailRoute(expenseId))
        },
        onEditExpenseClick = { expenseId ->
            navController.navigate(Routes.editExpenseRoute(expenseId))
        },
        onScrollPositionChanged = { index, offset ->
            expensesViewModel.onEvent(ExpensesUiEvent.ScrollPositionChanged(index, offset))
        },
        onDeleteExpense = { expenseId ->
            expensesViewModel.onEvent(ExpensesUiEvent.DeleteExpense(expenseId))
        },
        onCancelExpense = { expenseId ->
            expensesViewModel.onEvent(ExpensesUiEvent.CancelExpense(expenseId))
        },
        onSearchQueryChanged = { query ->
            expensesViewModel.onEvent(ExpensesUiEvent.SearchQueryChanged(query))
        }
    )
}

private fun handleExpenseUiAction(
    action: ExpensesUiAction,
    pillController: TopPillController,
    context: Context
) {
    when (action) {
        is ExpensesUiAction.ShowLoadError ->
            pillController.showPill(message = action.message.asString(context))
        is ExpensesUiAction.ShowDeleteSuccess ->
            pillController.showPill(message = action.message.asString(context))
        is ExpensesUiAction.ShowDeleteError ->
            pillController.showPill(message = action.message.asString(context))
        is ExpensesUiAction.ShowCancelSuccess ->
            pillController.showPill(message = action.message.asString(context))
        is ExpensesUiAction.ShowCancelError ->
            pillController.showPill(message = action.message.asString(context))
        is ExpensesUiAction.ScrollToTop -> Unit
    }
}
