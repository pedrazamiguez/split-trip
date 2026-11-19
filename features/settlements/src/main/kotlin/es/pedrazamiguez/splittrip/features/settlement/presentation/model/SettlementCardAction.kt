package es.pedrazamiguez.splittrip.features.settlement.presentation.model

internal data class SettlementCardAction(
    val label: String,
    val isPrimary: Boolean,
    val isEnabled: Boolean = true,
    val onClick: () -> Unit
)

internal enum class SettlementCardActionLayoutMode {
    ROW_STANDARD,
    ROW_COMPACT,
    COLUMN_STACKED
}

internal fun resolveSettlementCardActionLayoutMode(
    actionCount: Int,
    availableWidthPx: Int,
    spacingPx: Int,
    standardRequiredButtonWidthPx: Int,
    compactRequiredButtonWidthPx: Int
): SettlementCardActionLayoutMode {
    if (actionCount <= 1) return SettlementCardActionLayoutMode.ROW_STANDARD
    val totalSpacingPx = spacingPx * (actionCount - 1)
    val slotWidthPx = (availableWidthPx - totalSpacingPx) / actionCount
    return when {
        slotWidthPx <= 0 -> SettlementCardActionLayoutMode.COLUMN_STACKED
        standardRequiredButtonWidthPx <= slotWidthPx -> SettlementCardActionLayoutMode.ROW_STANDARD
        compactRequiredButtonWidthPx <= slotWidthPx -> SettlementCardActionLayoutMode.ROW_COMPACT
        else -> SettlementCardActionLayoutMode.COLUMN_STACKED
    }
}
