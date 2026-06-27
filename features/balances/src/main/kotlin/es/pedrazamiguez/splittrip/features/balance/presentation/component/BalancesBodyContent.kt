package es.pedrazamiguez.splittrip.features.balance.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Wallet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DashboardShimmer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerBox
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.BalancesUiEvent
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState

@Composable
internal fun BalancesBodyContent(
    uiState: BalancesUiState,
    bottomPadding: Dp,
    onEvent: (BalancesUiEvent) -> Unit,
    onNavigateToContribution: () -> Unit,
    onNavigateToWithdrawal: () -> Unit,
    onShowExtrasBreakdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = {
                DashboardShimmer(
                    bottomPadding = bottomPadding,
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
                        onNavigateToWithdrawal = onNavigateToWithdrawal,
                        onShowExtrasBreakdown = onShowExtrasBreakdown
                    )
                }
            }
        }
    }
}
