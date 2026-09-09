package es.pedrazamiguez.splittrip.features.balance.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.CategorySpendingItemRow
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.CategorySpendingUiState

@Composable
fun CategorySpendingScreen(
    uiState: CategorySpendingUiState,
    modifier: Modifier = Modifier
) {
    val bottomPadding = LocalBottomPadding.current

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = {
            ShimmerLoadingList(
                modifier = modifier.fillMaxSize(),
                itemCount = 5
            )
        }
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            item {
                CategorySpendingChart(uiState = uiState)
            }

            item {
                if (uiState.items.isEmpty()) {
                    EmptyStateView(
                        title = stringResource(R.string.balances_empty_title),
                        description = stringResource(R.string.balances_empty_description),
                        icon = TablerIcons.Outline.Receipt
                    )
                } else {
                    FlatCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(MaterialTheme.spacing.Medium)
                    ) {
                        Column {
                            uiState.items.forEach { item ->
                                CategorySpendingItemRow(
                                    item = item,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = MaterialTheme.spacing.Default,
                                            vertical = MaterialTheme.spacing.Medium
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
