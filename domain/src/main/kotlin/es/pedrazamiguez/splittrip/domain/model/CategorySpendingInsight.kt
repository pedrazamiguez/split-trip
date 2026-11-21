package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import java.math.BigDecimal

data class CategorySpendingInsight(
    val category: ExpenseCategory,
    val totalAmountCents: Long,
    val expenseCount: Int,
    val percentage: BigDecimal,
    val shareRatio: BigDecimal,
    val breakdownPercentage: BreakdownPercentage
)
