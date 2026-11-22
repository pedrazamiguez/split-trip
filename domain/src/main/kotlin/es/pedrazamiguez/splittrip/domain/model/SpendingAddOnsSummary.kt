package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal

data class SpendingAddOnsSummary(
    val totalExtrasCents: Long = 0L,
    val expenseExtrasCents: Long = 0L,
    val withdrawalFeesCents: Long = 0L,
    val totalDiscountsCents: Long = 0L,
    val extrasShareRatio: BigDecimal = BigDecimal.ZERO,
    val extrasBreakdownPercentage: BreakdownPercentage = BreakdownPercentage.Zero,
    val typeBreakdown: List<AddOnTypeSpendingInsight> = emptyList()
)
