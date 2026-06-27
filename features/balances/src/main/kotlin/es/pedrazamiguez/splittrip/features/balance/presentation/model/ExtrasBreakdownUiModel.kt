package es.pedrazamiguez.splittrip.features.balance.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * UI model representing a category group of extras (e.g., Fees, Surcharges, Tips)
 * in the Extras breakdown bottom sheet.
 */
data class ExtrasBreakdownUiModel(
    val typeLabel: String = "",
    val items: ImmutableList<ExtraItemUiModel> = persistentListOf(),
    val formattedSubtotal: String = ""
)
