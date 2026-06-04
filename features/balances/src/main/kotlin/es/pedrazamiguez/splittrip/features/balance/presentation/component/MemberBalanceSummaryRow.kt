package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronDown
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChevronUp
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.User
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel

@Composable
internal fun MemberBalanceSummaryRow(
    memberBalance: MemberBalanceUiModel,
    displayName: String,
    balanceColor: Color,
    isExpanded: Boolean,
    toggleContentDesc: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        Icon(imageVector = TablerIcons.Outline.User, contentDescription = null, tint = balanceColor)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_contributed_label),
                    value = memberBalance.formattedContributed
                )
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_cash_in_hand_label),
                    value = memberBalance.formattedCashInHand
                )
                BreakdownLabel(
                    label = stringResource(R.string.balances_member_spent_label),
                    value = memberBalance.formattedTotalSpent
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = memberBalance.formattedPocketBalance,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = balanceColor
            )
            Icon(
                imageVector = if (isExpanded) TablerIcons.Outline.ChevronUp else TablerIcons.Outline.ChevronDown,
                contentDescription = toggleContentDesc,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
