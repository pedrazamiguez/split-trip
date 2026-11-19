package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategorySpendingUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategorySpendingUiState(
    val isLoading: Boolean = true,
    val items: ImmutableList<CategorySpendingUiModel> = persistentListOf(),
    val totalFormattedAmount: String = "",
    val isFiltered: Boolean = false
)
