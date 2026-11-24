package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
internal data class SubcategoryStampItem(
    val id: String,
    val label: String,
    val icon: ImageVector?,
    val isSelected: Boolean
)
