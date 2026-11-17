package es.pedrazamiguez.splittrip.features.balance.presentation.model

internal enum class ActionButtonsLayoutMode {
    ROW_STANDARD,
    ROW_COMPACT,
    COLUMN_STACKED
}

internal fun resolveActionButtonsLayoutMode(
    availableWidthPx: Int,
    spacingPx: Int,
    standardRequiredButtonWidthPx: Int,
    compactRequiredButtonWidthPx: Int
): ActionButtonsLayoutMode {
    val halfRowWidthPx = (availableWidthPx - spacingPx) / 2
    return when {
        halfRowWidthPx <= 0 -> ActionButtonsLayoutMode.COLUMN_STACKED
        standardRequiredButtonWidthPx <= halfRowWidthPx -> ActionButtonsLayoutMode.ROW_STANDARD
        compactRequiredButtonWidthPx <= halfRowWidthPx -> ActionButtonsLayoutMode.ROW_COMPACT
        else -> ActionButtonsLayoutMode.COLUMN_STACKED
    }
}
