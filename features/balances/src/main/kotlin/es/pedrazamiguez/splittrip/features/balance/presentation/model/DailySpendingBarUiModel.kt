package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable

@Immutable
data class DailySpendingBarUiModel(
    val id: String,
    val axisLabel: String,
    val fullDateLabel: String,
    val formattedAmount: String,
    val expenseCountLabel: String,
    val heightRatio: Float,
    val isPeakDay: Boolean,
    val contentDescription: String
)
