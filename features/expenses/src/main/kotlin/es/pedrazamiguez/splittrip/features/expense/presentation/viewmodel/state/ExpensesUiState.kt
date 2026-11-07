package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExpensesUiState(
    val expenseGroups: ImmutableList<ExpenseDateGroupUiModel> = persistentListOf(),
    val isLoading: Boolean = true,
    val groupId: String? = null,
    val isGroupArchived: Boolean = false,
    val searchQuery: String = "",
    val filterCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    val totalExpensesCount: Int = 0,
    val formattedTotalSpent: String = "",
    val formattedTotalScheduled: String? = null,
    val visibleExpensesCount: Int = 0,
    val isFiltered: Boolean = false
) {
    /** Number of active non-search filter dimensions. */
    val activeFilterCount: Int
        get() = filterCriteria.activeFilterCount

    /** True when the group has no expenses at all. */
    val isGroupEmpty: Boolean
        get() = totalExpensesCount == 0

    /** True when search query or filters are active but no expenses match the criteria. */
    val isSearchResultEmpty: Boolean
        get() = (searchQuery.isNotBlank() || isFiltered) &&
            totalExpensesCount > 0 &&
            expenseGroups.all { it.expenses.isEmpty() }

    /** True when there are no expenses across all date groups. */
    val isEmpty: Boolean
        get() = expenseGroups.all { it.expenses.isEmpty() }
}
