package es.pedrazamiguez.splittrip.domain.model

sealed interface BreakdownPercentage {
    data object Zero : BreakdownPercentage
    data object SubOnePercent : BreakdownPercentage
    data class Whole(val percent: Int) : BreakdownPercentage
}
