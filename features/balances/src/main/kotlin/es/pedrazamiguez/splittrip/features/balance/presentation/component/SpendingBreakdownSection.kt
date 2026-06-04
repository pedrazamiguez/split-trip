package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartBar
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel

@Composable
internal fun SpendingBreakdownSection(
    memberBalance: MemberBalanceUiModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = TablerIcons.Outline.ChartBar,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.balances_member_spent_breakdown),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (memberBalance.cashSpentByCurrency.isEmpty() && memberBalance.nonCashSpentByCurrency.isEmpty()) {
            EmptyHintText(text = stringResource(R.string.balances_member_no_expenses))
        } else {
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
            if (memberBalance.cashSpentByCurrency.isNotEmpty()) {
                DetailRow(
                    label = stringResource(R.string.balances_member_cash_expenses),
                    value = memberBalance.formattedCashSpent
                )
                CurrencyBreakdownRows(items = memberBalance.cashSpentByCurrency)
            }
            if (memberBalance.nonCashSpentByCurrency.isNotEmpty()) {
                DetailRow(
                    label = stringResource(R.string.balances_member_non_cash_expenses),
                    value = memberBalance.formattedNonCashSpent
                )
                CurrencyBreakdownRows(items = memberBalance.nonCashSpentByCurrency)
            }
        }
    }
}
