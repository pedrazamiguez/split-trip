package es.pedrazamiguez.splittrip.features.expense.presentation.model

/** Per-member row in the split breakdown section of the expense detail screen. */
data class SplitDetailUiModel(
    val displayName: String,
    val formattedAmount: String,
    /** Formatted source currency amount; non-null only for foreign-currency expenses. */
    val formattedSourceAmount: String? = null,
    /** Formatted share percentage (e.g. "33.3%"); null for EQUAL splits. */
    val shareText: String? = null,
    val isCurrentUser: Boolean = false,
    val isExcluded: Boolean = false,
    /**
     * Presentation-only grouping hint. When non-null, the mapper groups this row
     * under a [SubunitSplitGroupUiModel] keyed by this id. NOT a domain reference.
     */
    val subunitId: String? = null,
    /** Human-readable subunit name resolved from the lookup. */
    val subunitLabel: String? = null
)
