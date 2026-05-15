package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

sealed interface ExpenseDetailUiEvent {
    /** User confirmed the destructive delete dialog in the TopBar. */
    data object DeleteConfirmed : ExpenseDetailUiEvent
}
