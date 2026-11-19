package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.spending.CategorySpendingItemRow
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.CategorySpendingUiState

private data class CategorySpendingEmptyConfig(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
)

private fun resolveEmptyConfig(isFiltered: Boolean): CategorySpendingEmptyConfig =
    if (isFiltered) {
        CategorySpendingEmptyConfig(
            titleRes = R.string.expenses_category_spending_filtered_empty_title,
            descriptionRes = R.string.expenses_category_spending_filtered_empty_description,
            icon = TablerIcons.Outline.Search
        )
    } else {
        CategorySpendingEmptyConfig(
            titleRes = R.string.expenses_category_spending_empty_title,
            descriptionRes = R.string.expenses_category_spending_empty_description,
            icon = TablerIcons.Outline.Receipt
        )
    }

@Composable
fun CategorySpendingScreen(
    uiState: CategorySpendingUiState,
    modifier: Modifier = Modifier
) {
    val topPadding = LocalTopPadding.current
    val bottomPadding = LocalBottomPadding.current

    DeferredLoadingContainer(
        isLoading = uiState.isLoading,
        loadingContent = {
            ShimmerLoadingList(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = topPadding),
                itemCount = 5
            )
        }
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)
        ) {
            item { CategorySpendingChart(uiState = uiState) }

            item {
                if (uiState.items.isEmpty()) {
                    val emptyConfig = resolveEmptyConfig(uiState.isFiltered)
                    EmptyStateView(
                        title = stringResource(emptyConfig.titleRes),
                        description = stringResource(emptyConfig.descriptionRes),
                        icon = emptyConfig.icon
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
