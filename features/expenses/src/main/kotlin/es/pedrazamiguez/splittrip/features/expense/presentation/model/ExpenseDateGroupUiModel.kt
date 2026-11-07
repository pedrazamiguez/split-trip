package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * Represents a group of expenses for a single day.
 * Used to render sticky headers in the expense list.
 */
@Immutable
data class ExpenseDateGroupUiModel(
    val dateText: String,
    val formattedDayTotal: String,
    val expenses: ImmutableList<ExpenseUiModel>,
    val formattedDayScheduled: String? = null
)
