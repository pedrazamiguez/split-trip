package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

sealed interface CategorySpendingUiEvent {
    data object OnNavigateBack : CategorySpendingUiEvent
}
