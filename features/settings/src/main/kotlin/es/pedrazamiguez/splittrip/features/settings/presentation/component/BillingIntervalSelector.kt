package es.pedrazamiguez.splittrip.features.settings.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.domain.enums.BillingInterval
import es.pedrazamiguez.splittrip.features.settings.R

@Composable
fun BillingIntervalSelector(
    selectedInterval: BillingInterval,
    onIntervalSelected: (BillingInterval) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthlyLabel = stringResource(R.string.subscriptions_interval_monthly)
    val annualLabel = stringResource(R.string.subscriptions_interval_annual)
    val saveBadge = stringResource(R.string.subscriptions_annual_save_badge)
    val annualDisplayLabel = "$annualLabel ($saveBadge)"

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(
            MaterialTheme.spacing.Medium,
            Alignment.CenterHorizontally
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PassportChip(
            label = monthlyLabel,
            selected = selectedInterval == BillingInterval.MONTHLY,
            onClick = { onIntervalSelected(BillingInterval.MONTHLY) }
        )
        PassportChip(
            label = annualDisplayLabel,
            selected = selectedInterval == BillingInterval.ANNUAL,
            onClick = { onIntervalSelected(BillingInterval.ANNUAL) }
        )
    }
}
