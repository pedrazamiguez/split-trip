package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Cash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.InfoCircle
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel

@Composable
internal fun CashInHandRow(
    memberBalance: MemberBalanceUiModel,
    onShowCashBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    DetailRow(
        label = stringResource(R.string.balances_member_cash_in_hand),
        value = memberBalance.formattedCashInHand,
        icon = TablerIcons.Outline.Cash,
        modifier = modifier,
        labelSuffix = if (memberBalance.cashBreakdown.isNotEmpty()) {
            {
                IconButton(
                    onClick = onShowCashBreakdown,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = TablerIcons.Outline.InfoCircle,
                        contentDescription = stringResource(R.string.balances_cash_breakdown_view_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        } else {
            null
        }
    )
    when {
        memberBalance.hasNegativeCashInHand -> {
            EmptyHintText(text = stringResource(R.string.balances_member_negative_cash_hint))
        }
        memberBalance.cashInHandByCurrency.isNotEmpty() -> {
            CurrencyBreakdownRows(items = memberBalance.cashInHandByCurrency)
        }
        else -> {
            EmptyHintText(text = stringResource(R.string.balances_member_no_cash))
        }
    }
}
