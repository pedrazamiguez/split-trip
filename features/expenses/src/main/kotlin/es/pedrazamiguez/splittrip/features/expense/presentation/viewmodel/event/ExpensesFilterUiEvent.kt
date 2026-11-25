package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset

sealed interface ExpensesFilterUiEvent {
    data class Initialize(val initialCriteria: ExpenseFilterCriteria) : ExpensesFilterUiEvent
    data class UpdateDraft(val criteria: ExpenseFilterCriteria) : ExpensesFilterUiEvent
    data class DatePresetSelected(val preset: DateRangePreset) : ExpensesFilterUiEvent
    data object ResetDraft : ExpensesFilterUiEvent
    data object ApplyFilters : ExpensesFilterUiEvent
}
