package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.BuildingBank
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel

@Composable
internal fun MemberBalanceExpandedDetail(
    memberBalance: MemberBalanceUiModel,
    balanceColor: Color,
    onShowCashBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(top = MaterialTheme.spacing.Small)) {
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        DetailRow(
            label = stringResource(R.string.balances_member_pocket_balance),
            value = memberBalance.formattedPocketBalance,
            valueColor = balanceColor,
            icon = TablerIcons.Outline.BuildingBank
        )
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Small))

        // Cash-in-hand row with info button to open the cash breakdown sheet
        CashInHandRow(
            memberBalance = memberBalance,
            onShowCashBreakdown = onShowCashBreakdown
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.Medium))

        SpendingBreakdownSection(memberBalance = memberBalance)
    }
}
