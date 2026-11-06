package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class CategorySpendingUiModel(
    val categoryName: String,
    val categoryIcon: ImageVector,
    val formattedAmount: String,
    val formattedPercentage: String,
    val progress: Float,
    val color: Color,
    val subcategories: ImmutableList<SubcategorySpendingUiModel> = persistentListOf()
)
