package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel

data class ExpenseDetailUiState(
    val expense: ExpenseDetailUiModel? = null,
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val isGroupArchived: Boolean = false
)
