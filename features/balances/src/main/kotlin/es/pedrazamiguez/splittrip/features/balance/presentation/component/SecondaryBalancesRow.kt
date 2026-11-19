package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SecondaryBalanceMetricItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SecondaryBalancesRow(
    balance: GroupPocketBalanceUiModel,
    onShowMetricInfo: (BalanceMetricType) -> Unit = {}
) {
    val items = buildList {
        if (balance.formattedAvailableBalance != null) {
            add(
                SecondaryBalanceMetricItem(
                    label = stringResource(R.string.balances_available),
                    formattedAmount = balance.formattedAvailableBalance,
                    metricType = BalanceMetricType.AVAILABLE
                )
            )
        }
        if (balance.formattedScheduledHoldAmount != null) {
            add(
                SecondaryBalanceMetricItem(
                    label = stringResource(R.string.balances_scheduled),
                    formattedAmount = balance.formattedScheduledHoldAmount,
                    metricType = BalanceMetricType.SCHEDULED
                )
            )
        }
        if (balance.formattedRefundableHoldAmount != null) {
            add(
                SecondaryBalanceMetricItem(
                    label = stringResource(R.string.balances_refundable),
                    formattedAmount = balance.formattedRefundableHoldAmount,
                    metricType = BalanceMetricType.REFUNDABLE
                )
            )
        }
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        items.forEachIndexed { index, (label, amount, metricType) ->
            val alignment = when {
                items.size == 1 -> Alignment.CenterHorizontally
                index == 0 -> Alignment.Start
                index == items.lastIndex -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
            SecondaryBalanceColumn(
                label = label,
                amount = amount,
                horizontalAlignment = alignment,
                onInfoClick = { onShowMetricInfo(metricType) }
            )
        }
    }
}
