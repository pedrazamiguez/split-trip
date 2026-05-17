package es.pedrazamiguez.splittrip.features.expense.presentation.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Two-level grouping wrapper for splits that belong to the same subunit.
 *
 * Rendered in the split card as a collapsible container — collapsed by default,
 * showing the subunit label and its entity-level total. Expanding reveals each
 * member row underneath. Solo splits (those with `subunitId == null` in the
 * source domain) bypass grouping and render as flat [SplitDetailUiModel] rows.
 */
data class SubunitSplitGroupUiModel(
    val subunitId: String,
    val subunitLabel: String,
    /** Sum of all members' group-currency amounts, already formatted. */
    val formattedTotalAmount: String,
    val memberCount: Int,
    val members: ImmutableList<SplitDetailUiModel>,
    /** The Level 2 (intra-subunit) split type label, e.g. "Porcentaje". */
    val splitTypeText: String
)
