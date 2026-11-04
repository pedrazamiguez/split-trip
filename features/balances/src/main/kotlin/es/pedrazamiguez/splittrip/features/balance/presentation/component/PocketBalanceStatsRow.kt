package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.BalanceMetricType
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel

@Composable
internal fun PocketBalanceStatsRow(
    balance: GroupPocketBalanceUiModel,
    modifier: Modifier = Modifier,
    onShowMetricInfo: (BalanceMetricType) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PocketBalanceStatColumn(
                label = stringResource(R.string.balances_total_contributed),
                amount = balance.formattedTotalContributed,
                metricType = BalanceMetricType.TOTAL_CONTRIBUTED,
                onShowMetricInfo = onShowMetricInfo
            )
            PocketBalanceStatColumn(
                label = stringResource(R.string.balances_total_spent),
                amount = balance.formattedTotalSpent,
                metricType = BalanceMetricType.TOTAL_SPENT,
                onShowMetricInfo = onShowMetricInfo,
                horizontalAlignment = Alignment.End,
                amountColor = MaterialTheme.colorScheme.error
            )
        }
        if (balance.formattedTotalExtras != null) {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))
            ExtrasStatsSection(
                formattedTotalExtras = balance.formattedTotalExtras,
                onShowMetricInfo = onShowMetricInfo
            )
        }
    }
}
