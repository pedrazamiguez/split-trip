package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.DonutChart
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chart.DonutChartData
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.CategorySpendingUiState
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun CategorySpendingChart(uiState: CategorySpendingUiState) {
    val chartData = uiState.items.map {
        DonutChartData(
            label = it.categoryName,
            value = it.progress,
            color = it.color
        )
    }.toImmutableList()

    DonutChart(
        data = chartData,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 64.dp, vertical = MaterialTheme.spacing.Screen),
        centerContent = {
            if (uiState.totalFormattedAmount.isNotBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.expenses_category_spending_total_spent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.totalFormattedAmount,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}
