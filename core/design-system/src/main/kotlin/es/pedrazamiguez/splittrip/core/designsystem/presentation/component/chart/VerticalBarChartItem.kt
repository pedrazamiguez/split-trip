package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart

import androidx.compose.runtime.Immutable

@Immutable
data class VerticalBarChartItem(
    val id: String,
    val label: String,
    val valueRatio: Float,
    val isHighlighted: Boolean = false,
    val contentDescription: String = label
)
