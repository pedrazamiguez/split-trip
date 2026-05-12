package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

sealed interface ExpenseDetailUiAction {
    data class ShowError(val message: UiText) : ExpenseDetailUiAction
    data class DeleteSuccess(val message: UiText) : ExpenseDetailUiAction
}
