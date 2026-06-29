package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface ExpensesUiAction {
    data class ShowLoadError(val message: UiText) : ExpensesUiAction
    data class ShowDeleteSuccess(val message: UiText) : ExpensesUiAction
    data class ShowDeleteError(val message: UiText) : ExpensesUiAction
    data class ShowCancelSuccess(val message: UiText) : ExpensesUiAction
    data class ShowCancelError(val message: UiText) : ExpensesUiAction
}
