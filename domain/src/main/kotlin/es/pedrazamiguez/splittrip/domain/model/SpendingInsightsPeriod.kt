package es.pedrazamiguez.splittrip.domain.model

import java.time.LocalDate

sealed interface SpendingInsightsPeriod {
    data object AllTime : SpendingInsightsPeriod
    data object ThisMonth : SpendingInsightsPeriod
    data object LastMonth : SpendingInsightsPeriod
    data class CustomRange(
        val startDate: LocalDate,
        val endDate: LocalDate
    ) : SpendingInsightsPeriod
}
