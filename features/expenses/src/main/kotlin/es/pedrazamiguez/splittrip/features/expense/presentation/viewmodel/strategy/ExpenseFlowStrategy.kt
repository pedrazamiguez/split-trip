package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Strategy pattern interface for the expense wizard flow.
 *
 * Decouples creation vs. modification behaviors, exposing screen-level
 * metadata, the initial entry step, configuration loading logic, and
 * the target persistence call (add vs. update use case).
 */
interface ExpenseFlowStrategy {
    val screenTitleRes: Int
    val submitLabelRes: Int
    val isEditMode: Boolean
    val startStep: AddExpenseStep

    fun loadInitialData(
        groupId: String?,
        uiState: MutableStateFlow<AddExpenseUiState>,
        actions: MutableSharedFlow<AddExpenseUiAction>,
        scope: CoroutineScope,
        forceRefresh: Boolean = false,
        onConfigLoaded: suspend () -> Unit
    )

    suspend fun saveExpense(
        groupId: String?,
        expense: Expense,
        uiState: AddExpenseUiState
    ): Result<Unit>
}
