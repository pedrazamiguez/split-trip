package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Lock
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
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
    onNavigateToWithdrawal: () -> Unit,
    onShowExtrasBreakdown: () -> Unit
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
        if (uiState.isGroupArchived) {
            item {
                FlatCard(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = TablerIcons.Outline.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = stringResource(
                                es.pedrazamiguez.splittrip.core.designsystem.R.string.group_detail_archived_label
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        item {
            GroupPocketBalanceCard(
                balance = uiState.pocketBalance,
                shouldAnimateBalance = uiState.shouldAnimateBalance,
                previousBalance = uiState.previousBalance,
                balanceRollingUp = uiState.balanceRollingUp,
                isGroupArchived = uiState.isGroupArchived,
                onBalanceAnimationComplete = { onEvent(BalancesUiEvent.BalanceAnimationComplete) },
                onAddMoney = onNavigateToContribution,
                onWithdrawCash = onNavigateToWithdrawal,
                onShowExtrasBreakdown = onShowExtrasBreakdown
            )
        }
        memberBalancesSection(uiState.memberBalances)
        activitySection(uiState.activityItems, uiState.isGroupArchived, onEvent)
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
    isGroupArchived: Boolean,
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
                onLongClick = if (!item.contribution.isLinkedContribution && !isGroupArchived) {
                    { onEvent(BalancesUiEvent.DeleteContributionRequested(item.contribution)) }
                } else {
                    null
                }
            )
            is ActivityItemUiModel.CashWithdrawalItem -> CashWithdrawalHistoryItem(
                withdrawal = item.withdrawal,
                onLongClick = if (!isGroupArchived) {
                    { onEvent(BalancesUiEvent.DeleteWithdrawalRequested(item.withdrawal)) }
                } else {
                    null
                }
            )
        }
    }
}
