package es.pedrazamiguez.splittrip.core.designsystem.preview.chart

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.VerticalBarChart
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.VerticalBarChartItem
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val COMPACT_BAR_ITEMS = persistentListOf(
    VerticalBarChartItem(id = "d1", label = "10", valueRatio = 0.25f),
    VerticalBarChartItem(id = "d2", label = "11", valueRatio = 0f),
    VerticalBarChartItem(id = "d3", label = "12", valueRatio = 0.65f),
    VerticalBarChartItem(id = "d4", label = "13", valueRatio = 1.0f, isHighlighted = true),
    VerticalBarChartItem(id = "d5", label = "14", valueRatio = 0.45f),
    VerticalBarChartItem(id = "d6", label = "15", valueRatio = 0.80f),
    VerticalBarChartItem(id = "d7", label = "16", valueRatio = 0.30f)
)

private val SCROLLABLE_BAR_ITEMS = (1..20).map { day ->
    val ratio = when {
        day == 14 -> 1.0f
        day % 5 == 0 -> 0f
        else -> ((day * 17) % 85 + 15) / 100f
    }
    VerticalBarChartItem(
        id = "day-$day",
        label = "$day",
        valueRatio = ratio,
        isHighlighted = day == 14
    )
}.toImmutableList()

@PreviewComplete
@Composable
private fun VerticalBarChartCompactPreview() {
    PreviewThemeWrapper {
        VerticalBarChart(
            items = COMPACT_BAR_ITEMS,
            selectedId = "d4",
            onSelectBar = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@PreviewComplete
@Composable
private fun VerticalBarChartScrollablePreview() {
    PreviewThemeWrapper {
        VerticalBarChart(
            items = SCROLLABLE_BAR_ITEMS,
            selectedId = "day-14",
            onSelectBar = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
