package es.pedrazamiguez.splittrip.features.expense.presentation.model

import androidx.annotation.StringRes

data class BreakdownSummaryUiModel(
    @StringRes val labelRes: Int,
    val formattedAmount: String
)
