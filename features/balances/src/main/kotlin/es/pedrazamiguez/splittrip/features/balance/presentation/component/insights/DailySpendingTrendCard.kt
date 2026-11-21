package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.VerticalBarChart
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.VerticalBarChartItem
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.DailySpendingBarUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun DailySpendingTrendCard(
    dailyBars: ImmutableList<DailySpendingBarUiModel>,
    selectedBarId: String?,
    onSelectBar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedBar = remember(dailyBars, selectedBarId) {
        dailyBars.firstOrNull { it.id == selectedBarId } ?: dailyBars.lastOrNull()
    }
    val chartItems = remember(dailyBars) {
        dailyBars.map { bar ->
            VerticalBarChartItem(
                id = bar.id,
                label = bar.axisLabel,
                valueRatio = bar.heightRatio,
                isHighlighted = bar.isPeakDay,
                contentDescription = bar.contentDescription
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeadingText(text = stringResource(R.string.balances_insights_daily_trend_title))
                selectedBar?.let { bar ->
                    PassportTag(
                        text = stringResource(
                            R.string.balances_insights_daily_bar_detail,
                            bar.fullDateLabel,
                            bar.formattedAmount,
                            bar.expenseCountLabel
                        ),
                        style = PassportTagStyle.INFO
                    )
                }
            }

            VerticalBarChart(
                items = chartItems,
                selectedId = selectedBar?.id,
                onSelectBar = onSelectBar,
                animationKey = dailyBars
            )
        }
    }
}
