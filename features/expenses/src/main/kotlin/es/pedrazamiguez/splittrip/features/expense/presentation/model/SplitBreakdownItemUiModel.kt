package es.pedrazamiguez.splittrip.features.expense.presentation.model

sealed interface SplitBreakdownItemUiModel {
    data class Solo(val split: SplitDetailUiModel) : SplitBreakdownItemUiModel
    data class Subunit(val group: SubunitSplitGroupUiModel) : SplitBreakdownItemUiModel
}
