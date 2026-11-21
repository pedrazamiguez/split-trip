package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class SpendingBurnRateUiModel(
    val formattedTotalSpent: String,
    val formattedExtrasCaption: String?,
    val formattedDailyAverage: String,
    val activeDaysLabel: String,
    val formattedMonthlyPace: String?,
    val monthlyPaceLabel: String?,
    val formattedPeakDayAmount: String?,
    val formattedPeakDayDate: String?,
    val dailyBars: ImmutableList<DailySpendingBarUiModel> = persistentListOf(),
    val defaultSelectedBarId: String? = null
)
