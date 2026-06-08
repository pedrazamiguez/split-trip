package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.BodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SectionHeadingText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun BalancesListContent(
    modifier: Modifier = Modifier,
    uiState: BalancesUiState,
    bottomPadding: Dp,
    onEvent: (BalancesUiEvent) -> Unit,
    onNavigateToContribution: () -> Unit,
    onNavigateToWithdrawal: () -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.Default,
            end = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + bottomPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = stringResource(R.string.balances_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                BodyText(
                    text = stringResource(R.string.balances_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            GroupPocketBalanceCard(
                balance = uiState.pocketBalance,
                shouldAnimateBalance = uiState.shouldAnimateBalance,
                previousBalance = uiState.previousBalance,
                balanceRollingUp = uiState.balanceRollingUp,
                onBalanceAnimationComplete = { onEvent(BalancesUiEvent.BalanceAnimationComplete) },
                onAddMoney = onNavigateToContribution,
                onWithdrawCash = onNavigateToWithdrawal
            )
        }
        memberBalancesSection(uiState.memberBalances)
        activitySection(uiState.activityItems, onEvent)
    }
}

private fun LazyListScope.memberBalancesSection(memberBalances: ImmutableList<MemberBalanceUiModel>) {
    if (memberBalances.isEmpty()) return
    item {
        SectionHeadingText(
            text = stringResource(R.string.balances_member_balances_title),
            modifier = Modifier.padding(top = MaterialTheme.spacing.Small)
        )
    }
    items(items = memberBalances, key = { "mb-${it.userId}" }) { memberBalance ->
        MemberBalanceItem(memberBalance = memberBalance)
    }
}

private fun LazyListScope.activitySection(
    activityItems: ImmutableList<ActivityItemUiModel>,
    onEvent: (BalancesUiEvent) -> Unit
) {
    if (activityItems.isEmpty()) return
    item {
        SectionHeadingText(
            text = stringResource(R.string.balances_history_title),
            modifier = Modifier.padding(top = MaterialTheme.spacing.Small)
        )
    }
    items(
        items = activityItems,
        key = { item ->
            when (item) {
                is ActivityItemUiModel.ContributionItem -> "c-${item.contribution.id}"
                is ActivityItemUiModel.CashWithdrawalItem -> "cw-${item.withdrawal.id}"
            }
        }
    ) { item ->
        when (item) {
            is ActivityItemUiModel.ContributionItem -> ContributionHistoryItem(
                contribution = item.contribution,
                onLongClick = if (!item.contribution.isLinkedContribution) {
                    { onEvent(BalancesUiEvent.DeleteContributionRequested(item.contribution)) }
                } else {
                    null
                }
            )
            is ActivityItemUiModel.CashWithdrawalItem -> CashWithdrawalHistoryItem(
                withdrawal = item.withdrawal,
                onLongClick = { onEvent(BalancesUiEvent.DeleteWithdrawalRequested(item.withdrawal)) }
            )
        }
    }
}
