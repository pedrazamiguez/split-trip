package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

sealed interface ExpensesUiEvent {
    data object LoadExpenses : ExpensesUiEvent
    data class ScrollPositionChanged(val index: Int, val offset: Int) : ExpensesUiEvent
    data class DeleteExpense(val expenseId: String) : ExpensesUiEvent
    data class CancelExpense(val expenseId: String) : ExpensesUiEvent
}
