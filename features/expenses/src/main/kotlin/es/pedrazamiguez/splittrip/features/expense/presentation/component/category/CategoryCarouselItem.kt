package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class CategoryCarouselItem(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val isSelected: Boolean,
    val isFocused: Boolean = isSelected,
    val activeSubcategoryCount: Int = 0
)
