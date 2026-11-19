package es.pedrazamiguez.splittrip.features.expense.presentation.model

import kotlinx.collections.immutable.ImmutableList

data class CategorizedExpenseSplits(
    val directSplits: ImmutableList<SplitUiModel>,
    val subunitSplits: ImmutableList<SplitUiModel>
)
