package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal
import java.time.LocalDate

data class DailySpendingPoint(
    val date: LocalDate,
    val amountCents: Long,
    val expenseCount: Int,
    val heightRatio: BigDecimal
)
