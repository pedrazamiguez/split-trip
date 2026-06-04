package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.navigation.SharedElementKeys
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButtonDefaults
import es.pedrazamiguez.splittrip.core.designsystem.transition.fabSharedTransitionModifier
import es.pedrazamiguez.splittrip.features.balance.R

@Composable
internal fun BalanceCardActionButtons(
    onAddMoney: () -> Unit,
    onWithdrawCash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addMoneySharedModifier = fabSharedTransitionModifier(SharedElementKeys.ADD_CONTRIBUTION)
    val withdrawSharedModifier = fabSharedTransitionModifier(SharedElementKeys.ADD_CASH_WITHDRAWAL)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
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
