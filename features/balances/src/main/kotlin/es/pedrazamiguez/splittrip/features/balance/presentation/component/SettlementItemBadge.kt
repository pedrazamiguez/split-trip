package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.features.balance.R

@Composable
internal fun SettlementItemBadge(
    isDebtor: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val badgeBg = if (isDebtor) colorScheme.errorContainer else colorScheme.primaryContainer
    val badgeText = if (isDebtor) colorScheme.onErrorContainer else colorScheme.onPrimaryContainer
    val labelId = if (isDebtor) R.string.balances_settlement_pay else R.string.balances_settlement_receive

    Surface(
        shape = CircleShape,
        color = badgeBg,
        contentColor = badgeText,
        modifier = modifier
    ) {
        Text(
            text = stringResource(labelId),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
