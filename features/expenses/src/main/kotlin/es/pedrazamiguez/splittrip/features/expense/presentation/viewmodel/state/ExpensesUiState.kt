package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExpensesUiState(
    val expenseGroups: ImmutableList<ExpenseDateGroupUiModel> = persistentListOf(),
    val isLoading: Boolean = true,
    val scrollPosition: Int = 0,
    val scrollOffset: Int = 0,
    val groupId: String? = null,
    val isGroupArchived: Boolean = false
) {
    /** True when there are no expenses across all date groups. */
    val isEmpty: Boolean
        get() = expenseGroups.all { it.expenses.isEmpty() }
}
