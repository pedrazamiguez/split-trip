package es.pedrazamiguez.splittrip.domain.model

import java.time.LocalDate

data class SpendingBurnRateInsight(
    val windowStartDate: LocalDate?,
    val windowEndDate: LocalDate?,
    val elapsedDays: Int,
    val totalWindowDays: Int,
    val activeDaysCount: Int,
    val averageDailySpendCents: Long,
    val averagePerActiveDayCents: Long,
    val projectedMonthlySpendCents: Long?,
    val peakDayDate: LocalDate?,
    val peakDayAmountCents: Long,
    val dailySeries: List<DailySpendingPoint>
)
