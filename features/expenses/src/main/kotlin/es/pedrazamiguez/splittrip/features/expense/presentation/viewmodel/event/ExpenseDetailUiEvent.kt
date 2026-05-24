package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

sealed interface ExpenseDetailUiEvent {
    /** User confirmed the destructive delete dialog in the TopBar. */
    data object DeleteConfirmed : ExpenseDetailUiEvent

    /** User requested a retry of the background receipt download. */
    data object RetryReceiptDownload : ExpenseDetailUiEvent
}
