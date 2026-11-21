package es.pedrazamiguez.splittrip.domain.model

import java.time.LocalDate

data class GroupSpendingInsights(
    val groupId: String,
    val currency: String,
    val period: SpendingInsightsPeriod,
    val hasAnyGroupExpenses: Boolean,
    val oldestExpenseDate: LocalDate?,
    val newestExpenseDate: LocalDate?,
    val totalSpentCents: Long,
    val totalBaseSpentCents: Long,
    val totalExtrasCents: Long,
    val expenseCount: Int,
    val burnRate: SpendingBurnRateInsight,
    val categoryBreakdown: List<CategorySpendingInsight>,
    val memberBreakdown: List<MemberSpendingInsight>,
    val entityBreakdown: List<EntitySpendingInsight>,
    val hasSubunits: Boolean,
    val memberProfiles: Map<String, User>,
    val currentUserId: String?
)
