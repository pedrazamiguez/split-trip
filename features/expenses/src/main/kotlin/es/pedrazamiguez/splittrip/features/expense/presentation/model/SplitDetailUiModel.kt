package es.pedrazamiguez.splittrip.features.expense.presentation.model

/** Per-member row in the split breakdown section of the expense detail screen. */
data class SplitDetailUiModel(
    val displayName: String,
    val formattedAmount: String,
    /** Formatted share percentage (e.g. "33.3%"); null for EQUAL splits. */
    val shareText: String? = null,
    val isCurrentUser: Boolean = false,
    val isExcluded: Boolean = false
)
