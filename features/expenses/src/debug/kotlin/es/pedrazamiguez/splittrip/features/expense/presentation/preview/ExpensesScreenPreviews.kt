package es.pedrazamiguez.splittrip.features.expense.presentation.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ChartDonut
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTopPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.ProfileAvatarButton
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewNarrowThemes
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpensesFilterActionButton
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpensesScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState

@PreviewComplete
@Composable
private fun ExpensesScreenLoadingPreview() {
    PreviewThemeWrapper {
        ExpensesScreen(
            uiState = ExpensesUiState(isLoading = true)
        )
    }
}

@PreviewComplete
@Composable
private fun ExpensesScreenEmptyPreview() {
    PreviewThemeWrapper {
        ExpensesScreen(
            uiState = ExpensesUiState(isLoading = false, groupId = "group-1")
        )
    }
}

@PreviewComplete
@Composable
private fun ExpensesScreenWithDataPreview() {
    ExpenseListPreviewHelper { expenseGroups ->
        val count = expenseGroups.sumOf { it.expenses.size }
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                expenseGroups = expenseGroups,
                totalExpensesCount = count,
                formattedTotalSpent = "€125.50",
                visibleExpensesCount = count,
                isFiltered = false
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ExpensesScreenSearchActivePreview() {
    ExpenseListPreviewHelper { expenseGroups ->
        val count = expenseGroups.sumOf { it.expenses.size }
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                searchQuery = "Dinner",
                expenseGroups = expenseGroups,
                totalExpensesCount = count,
                formattedTotalSpent = "€50.00",
                visibleExpensesCount = count,
                isFiltered = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ExpensesScreenEmptySearchPreview() {
    PreviewThemeWrapper {
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                searchQuery = "Nonexistent",
                totalExpensesCount = 5,
                formattedTotalSpent = "€0.00",
                visibleExpensesCount = 0,
                isFiltered = true
            )
        )
    }
}

@PreviewComplete
@Composable
private fun ExpensesScreenWithScheduledDataPreview() {
    ExpenseListPreviewHelper { expenseGroups ->
        val count = expenseGroups.sumOf { it.expenses.size }
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                expenseGroups = expenseGroups,
                totalExpensesCount = count,
                formattedTotalSpent = "€125.50",
                formattedTotalScheduled = "€234.00",
                visibleExpensesCount = count,
                isFiltered = false
            )
        )
    }
}

@PreviewNarrowThemes
@Composable
private fun ExpensesScreenNarrowPreview() {
    ExpenseListPreviewHelper { expenseGroups ->
        val count = expenseGroups.sumOf { it.expenses.size }
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                expenseGroups = expenseGroups,
                totalExpensesCount = count,
                formattedTotalSpent = "€125.50",
                formattedTotalScheduled = "€234.00",
                visibleExpensesCount = count,
                isFiltered = false
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Compact 320x568 - Light",
    widthDp = 320,
    heightDp = 568,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Compact 320x568 - Dark",
    widthDp = 320,
    heightDp = 568,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
private fun ExpensesScreenCompactViewportWithChromePreview() {
    ExpenseListPreviewHelper { expenseGroups ->
        val count = expenseGroups.sumOf { it.expenses.size }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                DynamicTopAppBar(
                    title = stringResource(R.string.expenses_title),
                    subtitle = "South Africa Safari 2026",
                    actions = {
                        ExpensesFilterActionButton(
                            activeFilterCount = 2,
                            onClick = {},
                            onLongClick = {}
                        )
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = TablerIcons.Outline.ChartDonut,
                                contentDescription = stringResource(R.string.expenses_category_spending_title)
                            )
                        }
                        ProfileAvatarButton(
                            avatarUrl = null,
                            onClick = {}
                        )
                    }
                )
            },
            bottomBar = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Adaptive Banner Ad",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(98.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Floating Navigation Bar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalTopPadding provides innerPadding.calculateTopPadding(),
                LocalBottomPadding provides innerPadding.calculateBottomPadding()
            ) {
                ExpensesScreen(
                    uiState = ExpensesUiState(
                        isLoading = false,
                        groupId = "group-1",
                        expenseGroups = expenseGroups,
                        totalExpensesCount = count,
                        formattedTotalSpent = "€125.50",
                        visibleExpensesCount = count,
                        isFiltered = true
                    )
                )
            }
        }
    }
}

@PreviewNarrowThemes
@Composable
private fun ExpensesScreenNarrowEmptySearchPreview() {
    PreviewThemeWrapper {
        ExpensesScreen(
            uiState = ExpensesUiState(
                isLoading = false,
                groupId = "group-1",
                searchQuery = "Nonexistent",
                totalExpensesCount = 5,
                formattedTotalSpent = "€0.00",
                visibleExpensesCount = 0,
                isFiltered = true
            )
        )
    }
}
