package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MetricScopeBreakdownUiModel
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MetricBreakdownContent(
    metricType: BalanceMetricType,
    breakdown: ImmutableList<MetricScopeBreakdownUiModel>,
    formattedGrandTotal: String,
    modifier: Modifier = Modifier
) {
    if (breakdown.isEmpty()) {
        val emptyRes = when (metricType) {
            BalanceMetricType.EXTRAS -> R.string.balances_extras_breakdown_empty
            BalanceMetricType.SCHEDULED -> R.string.balances_scheduled_breakdown_empty
            BalanceMetricType.REFUNDABLE -> R.string.balances_refundable_breakdown_empty
            else -> R.string.balances_extras_breakdown_empty
        }
        BodyText(
            text = stringResource(emptyRes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        for (section in breakdown) {
            MetricBreakdownSection(section = section)
        }

        if (formattedGrandTotal.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.Small),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabelText(
                    text = stringResource(R.string.balances_extras_breakdown_total),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedGrandTotal,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
