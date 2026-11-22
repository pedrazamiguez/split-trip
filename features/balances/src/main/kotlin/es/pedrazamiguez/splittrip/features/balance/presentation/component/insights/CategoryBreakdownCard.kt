package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.DonutChart
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.DonutChartData
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.AmountText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategoryInsightUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val DONUT_DIAMETER = 180.dp
private val DONUT_STROKE_WIDTH = 32.dp

@Composable
internal fun CategoryBreakdownCard(
    items: ImmutableList<CategoryInsightUiModel>,
    totalFormattedAmount: String,
    modifier: Modifier = Modifier
) {
    val chartData = remember(items) {
        items.map { item ->
            DonutChartData(
                label = item.categoryName,
                value = item.progress,
                color = item.color
            )
        }.toImmutableList()
    }

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            SectionHeadingText(text = stringResource(R.string.balances_insights_category_title))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = MaterialTheme.spacing.Small,
                        bottom = MaterialTheme.spacing.Large
                    ),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    data = chartData,
                    modifier = Modifier.size(DONUT_DIAMETER),
                    strokeWidthDp = DONUT_STROKE_WIDTH,
                    centerContent = {
                        if (totalFormattedAmount.isNotBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CaptionText(
                                    text = stringResource(R.string.balances_insights_total_spent_label)
                                )
                                AmountText(text = totalFormattedAmount)
                            }
                        }
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)) {
                items.forEach { item ->
                    CategoryInsightRow(
                        item = item,
                        animationKey = items
                    )
                }
            }
        }
    }
}
