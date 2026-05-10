package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButtonDefaults
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.AnimatedAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.transition.fabSharedTransitionModifier
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import kotlinx.collections.immutable.ImmutableList

private val CARD_SHADOW_ELEVATION = 8.dp

@Composable
fun GroupPocketBalanceCard(
    balance: GroupPocketBalanceUiModel,
    modifier: Modifier = Modifier,
    shouldAnimateBalance: Boolean = false,
    previousBalance: String = "",
    balanceRollingUp: Boolean = true,
    onBalanceAnimationComplete: () -> Unit = {},
    onAddMoney: () -> Unit = {},
    onWithdrawCash: () -> Unit = {}
) {
    FlatCard(modifier = modifier.fillMaxWidth(), elevation = CARD_SHADOW_ELEVATION) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PocketBalanceMainSection(
                balance = balance,
                shouldAnimateBalance = shouldAnimateBalance,
                previousBalance = previousBalance,
                balanceRollingUp = balanceRollingUp,
                onBalanceAnimationComplete = onBalanceAnimationComplete
            )

            Spacer(modifier = Modifier.height(24.dp))

            PocketBalanceStatsRow(balance = balance)

            if (balance.cashBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                CashBalancesSection(
                    cashBalances = balance.cashBalances,
                    formattedTotalCashEquivalent = balance.formattedTotalCashEquivalent
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            BalanceCardActionButtons(
                onAddMoney = onAddMoney,
                onWithdrawCash = onWithdrawCash
            )
        }
    }
}

@Composable
private fun PocketBalanceMainSection(
    balance: GroupPocketBalanceUiModel,
    shouldAnimateBalance: Boolean,
    previousBalance: String,
    balanceRollingUp: Boolean,
    onBalanceAnimationComplete: () -> Unit
) {
    if (balance.groupName.isNotBlank()) {
        Text(
            text = balance.groupName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    Text(
        text = stringResource(R.string.balances_remaining),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(4.dp))
    AnimatedAmount(
        formattedAmount = balance.formattedBalance,
        shouldAnimate = shouldAnimateBalance,
        previousAmount = previousBalance,
        rollingUp = balanceRollingUp,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        onAnimationComplete = onBalanceAnimationComplete
    )
    if (balance.formattedAvailableBalance != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.balances_available),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = balance.formattedAvailableBalance,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun PocketBalanceStatsRow(balance: GroupPocketBalanceUiModel) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(
                text = stringResource(R.string.balances_total_contributed),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = balance.formattedTotalContributed,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(R.string.balances_total_spent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = balance.formattedTotalSpent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
    if (balance.formattedTotalExtras != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(R.string.balances_total_extras),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = balance.formattedTotalExtras,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CashBalancesSection(
    cashBalances: ImmutableList<CashBalanceUiModel>,
    formattedTotalCashEquivalent: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.balances_cash_balance_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        if (formattedTotalCashEquivalent.isNotBlank()) {
            Text(
                text = formattedTotalCashEquivalent,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    cashBalances.forEach { cashBalance -> CashBalanceRow(cashBalance = cashBalance) }
}

@Composable
private fun CashBalanceRow(cashBalance: CashBalanceUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = cashBalance.currency,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = cashBalance.formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (cashBalance.formattedEquivalent.isNotBlank()) {
                Text(
                    text = cashBalance.formattedEquivalent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun BalanceCardActionButtons(
    onAddMoney: () -> Unit,
    onWithdrawCash: () -> Unit
) {
    val addMoneySharedModifier = fabSharedTransitionModifier(SharedElementKeys.ADD_CONTRIBUTION)
    val withdrawSharedModifier = fabSharedTransitionModifier(SharedElementKeys.ADD_CASH_WITHDRAWAL)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GradientButton(
            text = stringResource(R.string.balances_add_money),
            onClick = onAddMoney,
            leadingIcon = TablerIcons.Outline.Plus,
            modifier = Modifier.weight(1f).then(addMoneySharedModifier)
        )
        GradientButton(
            text = stringResource(R.string.balances_withdraw_cash),
            onClick = onWithdrawCash,
            colors = GradientButtonDefaults.secondaryColors(),
            leadingIcon = TablerIcons.Outline.CashBanknote,
            modifier = Modifier.weight(1f).then(withdrawSharedModifier)
        )
    }
}
