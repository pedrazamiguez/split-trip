package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingBank
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel

private val CARD_SHADOW_ELEVATION = 8.dp

@Composable
fun GroupPocketBalanceCard(
    balance: GroupPocketBalanceUiModel,
    modifier: Modifier = Modifier,
    shouldAnimateBalance: Boolean = false,
    previousBalance: String = "",
    balanceRollingUp: Boolean = true,
    isGroupArchived: Boolean = false,
    onBalanceAnimationComplete: () -> Unit = {},
    onAddMoney: () -> Unit = {},
    onWithdrawCash: () -> Unit = {},
    onShowExtrasBreakdown: () -> Unit = {},
    onSimplifyDebts: () -> Unit = {}
) {
    FlatCard(modifier = modifier.fillMaxWidth(), elevation = CARD_SHADOW_ELEVATION) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PocketBalanceMainSection(
                balance = balance,
                shouldAnimateBalance = shouldAnimateBalance,
                previousBalance = previousBalance,
                balanceRollingUp = balanceRollingUp,
                onBalanceAnimationComplete = onBalanceAnimationComplete
            )

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

            PocketBalanceStatsRow(
                balance = balance,
                onShowExtrasBreakdown = onShowExtrasBreakdown
            )

            if (balance.cashBalances.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))
                CashBalancesSection(
                    cashBalances = balance.cashBalances,
                    formattedTotalCashEquivalent = balance.formattedTotalCashEquivalent
                )
            }

            if (!isGroupArchived) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

                BalanceCardActionButtons(
                    onAddMoney = onAddMoney,
                    onWithdrawCash = onWithdrawCash
                )
            } else {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraLarge))

                GradientButton(
                    text = stringResource(R.string.balances_simplify_debts),
                    onClick = onSimplifyDebts,
                    leadingIcon = TablerIcons.Outline.BuildingBank,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
