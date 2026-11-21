package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class CategoryInsightUiModel(
    val category: ExpenseCategory,
    val categoryName: String,
    val categoryIcon: ImageVector,
    val formattedAmount: String,
    val formattedPercentage: String,
    val expenseCountLabel: String,
    val progress: Float,
    val color: Color,
    val subcategories: ImmutableList<SubcategoryInsightUiModel> = persistentListOf()
)
