package es.pedrazamiguez.splittrip.features.balance.presentation.model

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList

internal data class UncoloredCategoryEntry(
    val name: String,
    val icon: ImageVector,
    val amount: Long,
    val formattedAmount: String,
    val formattedPercentage: String = "",
    val progress: Float,
    val subcategories: ImmutableList<SubcategorySpendingUiModel>
)
