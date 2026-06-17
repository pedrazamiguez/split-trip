package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel.SharedViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpensesViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesUiEvent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpensesFeature(
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
            when (action) {
                is ExpensesUiAction.ShowLoadError -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                is ExpensesUiAction.ShowDeleteSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                }

                is ExpensesUiAction.ShowDeleteError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
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
        }
    )
}
