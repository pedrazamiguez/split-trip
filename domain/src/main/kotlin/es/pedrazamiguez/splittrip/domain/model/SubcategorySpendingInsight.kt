package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import java.math.BigDecimal

data class SubcategorySpendingInsight(
    val subcategory: ExpenseSubcategory,
    val totalAmountCents: Long,
    val expenseCount: Int,
    val percentage: BigDecimal,
    val shareRatio: BigDecimal,
    val breakdownPercentage: BreakdownPercentage
)
