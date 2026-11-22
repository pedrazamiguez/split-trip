package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal

data class MemberSpendingInsight(
    val userId: String,
    val effectiveSpentCents: Long,
    val expenseCount: Int,
    val percentage: BigDecimal,
    val shareRatio: BigDecimal,
    val breakdownPercentage: BreakdownPercentage
)
