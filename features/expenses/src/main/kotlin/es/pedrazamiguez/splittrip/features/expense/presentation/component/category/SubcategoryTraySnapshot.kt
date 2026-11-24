package es.pedrazamiguez.splittrip.features.expense.presentation.component.category

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class SubcategoryTraySnapshot(
    val categoryId: String?,
    val categoryLabel: String?,
    val items: ImmutableList<SubcategoryStampItem>,
    val badgeText: String?,
    val badgeStyle: PassportTagStyle,
    val badgeContentDescription: String?
)
