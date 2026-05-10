package es.pedrazamiguez.splittrip.features.balance.presentation.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.CashBanknote
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DashboardShimmer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerBox
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.CashWithdrawalHistoryItem
import es.pedrazamiguez.splittrip.features.balance.presentation.component.ContributionHistoryItem
import es.pedrazamiguez.splittrip.features.balance.presentation.component.GroupPocketBalanceCard
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceItem
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState
import kotlinx.collections.immutable.ImmutableList

@Composable
fun BalancesScreen(
    uiState: BalancesUiState = BalancesUiState(),
    onEvent: (BalancesUiEvent) -> Unit = {},
    onNavigateToContribution: () -> Unit = {},
    onNavigateToWithdrawal: () -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current

    // Local state for the DestructiveConfirmationDialog (ephemeral, driven from sheet action)
    var contributionPendingDelete by remember { mutableStateOf<ContributionUiModel?>(null) }
    var withdrawalPendingDelete by remember { mutableStateOf<CashWithdrawalUiModel?>(null) }

    BalancesBodyContent(
        uiState = uiState,
        bottomPadding = bottomPadding,
        onEvent = onEvent,
        onNavigateToContribution = onNavigateToContribution,
        onNavigateToWithdrawal = onNavigateToWithdrawal
    )

    // ActionBottomSheet overlays (driven by UiState — survives recomposition)
    BalancesScreenOverlays(
        uiState = uiState,
        onEvent = onEvent,
        onContributionDeleteRequested = { contribution ->
            contributionPendingDelete = contribution
        },
        onWithdrawalDeleteRequested = { withdrawal ->
            withdrawalPendingDelete = withdrawal
        }
    )

    // DestructiveConfirmationDialog for contribution
    contributionPendingDelete?.let { contribution ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.balances_delete_contribution_dialog_title),
            text = stringResource(
                R.string.balances_delete_contribution_dialog_text,
                contribution.formattedAmount
            ),
            onDismiss = { contributionPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteContributionConfirmed(contribution.id))
                contributionPendingDelete = null
            }
        )
    }

    // DestructiveConfirmationDialog for cash withdrawal
    withdrawalPendingDelete?.let { withdrawal ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.balances_delete_withdrawal_dialog_title),
            text = stringResource(
                R.string.balances_delete_withdrawal_dialog_text,
                withdrawal.formattedAmount
            ),
            onDismiss = { withdrawalPendingDelete = null },
            onConfirm = {
                onEvent(BalancesUiEvent.DeleteWithdrawalConfirmed(withdrawal.id))
                withdrawalPendingDelete = null
            }
        )
    }
}

@Composable
private fun BalancesBodyContent(
    uiState: BalancesUiState,
    bottomPadding: Dp,
    onEvent: (BalancesUiEvent) -> Unit,
    onNavigateToContribution: () -> Unit,
    onNavigateToWithdrawal: () -> Unit
) {
    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = {
            DashboardShimmer(
                bottomPadding = bottomPadding,
                // Mirror the title+subtitle header above GroupPocketBalanceCard to keep the
                // vertical layout stable during the loading→content transition.
                headerContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
                        ShimmerBox(height = 36.dp, width = 200.dp)
                        ShimmerBox(height = 14.dp, width = 260.dp)
                    }
                }
            )
        }
    ) {
        when {
            uiState.pocketBalance.formattedBalance.isEmpty() &&
                uiState.activityItems.isEmpty() -> {
                EmptyStateView(
                    title = stringResource(R.string.balances_empty_title),
                    description = stringResource(R.string.balances_empty_description),
                    icon = TablerIcons.Outline.Wallet
                )
            }

            else -> {
                BalancesListContent(
                    uiState = uiState,
                    bottomPadding = bottomPadding,
                    onEvent = onEvent,
                    onNavigateToContribution = onNavigateToContribution,
                    onNavigateToWithdrawal = onNavigateToWithdrawal
                )
            }
        }
    }
}

@Composable
private fun BalancesListContent(
    uiState: BalancesUiState,
    bottomPadding: Dp,
    onEvent: (BalancesUiEvent) -> Unit,
    onNavigateToContribution: () -> Unit,
    onNavigateToWithdrawal: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
                Text(
                    text = stringResource(R.string.balances_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
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
        Text(
            text = stringResource(R.string.balances_member_balances_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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
        Text(
            text = stringResource(R.string.balances_history_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
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

// ── Overlays (ActionBottomSheet) ──────────────────────────────────────────────

@Composable
private fun BalancesScreenOverlays(
    uiState: BalancesUiState,
    onEvent: (BalancesUiEvent) -> Unit,
    onContributionDeleteRequested: (ContributionUiModel) -> Unit,
    onWithdrawalDeleteRequested: (CashWithdrawalUiModel) -> Unit
) {
    uiState.contributionToDelete?.let { contribution ->
        ActionBottomSheet(
            title = stringResource(
                R.string.balances_contribution_actions_title,
                contribution.displayName
            ),
            icon = TablerIcons.Outline.Wallet,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.balances_delete_contribution_action),
                    icon = TablerIcons.Outline.Trash,
                    isDestructive = true,
                    onClick = {
                        onEvent(BalancesUiEvent.DeleteContributionDismissed)
                        onContributionDeleteRequested(contribution)
                    }
                )
            ),
            onDismiss = { onEvent(BalancesUiEvent.DeleteContributionDismissed) }
        )
    }

    uiState.withdrawalToDelete?.let { withdrawal ->
        ActionBottomSheet(
            title = stringResource(
                R.string.balances_withdrawal_actions_title,
                withdrawal.displayName
            ),
            icon = TablerIcons.Outline.CashBanknote,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.balances_delete_withdrawal_action),
                    icon = TablerIcons.Outline.Trash,
                    isDestructive = true,
                    onClick = {
                        onEvent(BalancesUiEvent.DeleteWithdrawalDismissed)
                        onWithdrawalDeleteRequested(withdrawal)
                    }
                )
            ),
            onDismiss = { onEvent(BalancesUiEvent.DeleteWithdrawalDismissed) }
        )
    }
}
