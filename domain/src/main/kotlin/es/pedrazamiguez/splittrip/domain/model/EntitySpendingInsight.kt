package es.pedrazamiguez.splittrip.domain.model

import java.math.BigDecimal

data class EntitySpendingInsight(
    val entityId: String,
    val type: EntitySpendingType,
    val subunitName: String?,
    val memberIds: List<String>,
    val effectiveSpentCents: Long,
    val expenseCount: Int,
    val percentage: BigDecimal,
    val shareRatio: BigDecimal,
    val breakdownPercentage: BreakdownPercentage
)
