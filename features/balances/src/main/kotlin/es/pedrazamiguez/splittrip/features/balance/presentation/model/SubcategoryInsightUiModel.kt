package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory

@Immutable
data class SubcategoryInsightUiModel(
    val subcategory: ExpenseSubcategory,
    val subcategoryName: String,
    val subcategoryIcon: ImageVector,
    val formattedAmount: String,
    val formattedPercentage: String,
    val expenseCountLabel: String
)
