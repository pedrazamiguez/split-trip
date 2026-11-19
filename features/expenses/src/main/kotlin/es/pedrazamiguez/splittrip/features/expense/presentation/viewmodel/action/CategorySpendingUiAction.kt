package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action

sealed interface CategorySpendingUiAction {
    data object NavigateBack : CategorySpendingUiAction
}
