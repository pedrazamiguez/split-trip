package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.transition.SharedTransitionSurface
import es.pedrazamiguez.splittrip.features.expense.presentation.component.AddExpenseConfigFailedContent
import es.pedrazamiguez.splittrip.features.expense.presentation.component.ExpenseWizard
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState

/**
 * Shared element transition key for the Add Expense FAB -> Screen transition.
 */
const val ADD_EXPENSE_SHARED_ELEMENT_KEY = "add_expense_container"

@Composable
fun AddExpenseScreen(
    groupId: String? = null,
    uiState: AddExpenseUiState,
    onEvent: (AddExpenseUiEvent) -> Unit = {}
) {
    LaunchedEffect(groupId) {
        onEvent(AddExpenseUiEvent.LoadGroupConfig(groupId))
    }

    SharedTransitionSurface(sharedElementKey = ADD_EXPENSE_SHARED_ELEMENT_KEY) {
        when {
            uiState.isReady -> {
                ExpenseWizard(groupId = groupId, uiState = uiState, onEvent = onEvent)
            }

            uiState.configLoadFailed -> {
                AddExpenseConfigFailedContent(
                    onRetry = { onEvent(AddExpenseUiEvent.RetryLoadConfig(groupId)) }
                )
            }

            else -> {
                ShimmerLoadingList()
            }
        }
    }
}
