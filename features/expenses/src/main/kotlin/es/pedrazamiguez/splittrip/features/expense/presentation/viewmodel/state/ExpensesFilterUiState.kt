package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberOptionUiModel
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ExpensesFilterUiState(
    val draftCriteria: ExpenseFilterCriteria = ExpenseFilterCriteria(),
    val availableMembers: ImmutableList<MemberOptionUiModel> = persistentListOf(),
    val matchingExpensesCount: Int = 0,
    val totalExpensesCount: Int = 0,
    val isLoading: Boolean = false,
    val groupId: String? = null,
    val oldestExpenseDate: LocalDate? = null,
    val newestExpenseDate: LocalDate? = null,
    val formattedStartDate: String = "",
    val formattedEndDate: String = "",
    val activePreset: DateRangePreset? = null,
    val groupCurrency: String = "EUR",
    val groupCurrencies: ImmutableList<String> = persistentListOf()
) {
    val canReset: Boolean
        get() = draftCriteria.activeFilterCount > 0
}
