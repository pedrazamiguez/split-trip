package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Search
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.RestoreScrollEffect
import es.pedrazamiguez.splittrip.features.expense.presentation.component.TrackScrollEffect
import es.pedrazamiguez.splittrip.features.expense.presentation.component.dialog.ResetFiltersConfirmationDialog
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.DateHeaderItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseSearchBar
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpensesTotalSummaryRow
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState
import kotlinx.coroutines.flow.Flow

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(
    uiState: ExpensesUiState = ExpensesUiState(),
    actions: Flow<ExpensesUiAction>? = null,
    onExpenseClicked: (String) -> Unit = { _ -> },
    onEditExpenseClick: (String) -> Unit = {},
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onDeleteExpense: (expenseId: String) -> Unit = {},
    onCancelExpense: (expenseId: String) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onFilterClick: () -> Unit = {},
    onResetFilters: () -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current

    var selectedExpenseForMenu by remember { mutableStateOf<ExpenseUiModel?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseUiModel?>(null) }
    var expenseToCancel by remember { mutableStateOf<ExpenseUiModel?>(null) }
    var showResetFiltersDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.scrollPosition,
        initialFirstVisibleItemScrollOffset = uiState.scrollOffset
    )

    RestoreScrollEffect(listState = listState, uiState = uiState)
    TrackScrollEffect(listState = listState, onScrollPositionChanged = onScrollPositionChanged)

    LaunchedEffect(actions) {
        actions?.collect { action ->
            when (action) {
                is ExpensesUiAction.ScrollToTop -> {
                    listState.animateScrollToItem(0)
                }
                else -> Unit
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = { ShimmerLoadingList() }
        ) {
            when {
                uiState.isGroupEmpty -> {
                    EmptyStateView(
                        title = stringResource(R.string.expenses_not_found),
                        icon = TablerIcons.Outline.Receipt
                    )
                }

                else -> {
                    val sharedTransitionScope = LocalSharedTransitionScope.current
                    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                    Column(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (!uiState.isSearchResultEmpty) {
                                ExpensesTotalSummaryRow(
                                    formattedTotalSpent = uiState.formattedTotalSpent,
                                    formattedTotalScheduled = uiState.formattedTotalScheduled,
                                    visibleExpensesCount = uiState.visibleExpensesCount,
                                    isFiltered = uiState.isFiltered,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            start = MaterialTheme.spacing.Default,
                                            top = MaterialTheme.spacing.Default,
                                            end = MaterialTheme.spacing.Default,
                                            bottom = MaterialTheme.spacing.None
                                        )
                                )
                            }
                            ExpenseSearchBar(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged,
                                activeFilterCount = uiState.activeFilterCount,
                                onFilterClick = onFilterClick,
                                onFilterLongClick = {
                                    if (uiState.activeFilterCount > 0) {
                                        showResetFiltersDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        start = MaterialTheme.spacing.Default,
                                        top = if (!uiState.isSearchResultEmpty) {
                                            MaterialTheme.spacing.Small
                                        } else {
                                            MaterialTheme.spacing.Default
                                        },
                                        end = MaterialTheme.spacing.Default,
                                        bottom = MaterialTheme.spacing.ExtraSmall
                                    )
                            )
                        }

                        when {
                            uiState.isSearchResultEmpty -> {
                                EmptyStateView(
                                    title = stringResource(R.string.expenses_search_empty_title),
                                    description = stringResource(R.string.expenses_search_empty_description),
                                    icon = TablerIcons.Outline.Search
                                )
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = MaterialTheme.spacing.Default,
                                        top = MaterialTheme.spacing.Small,
                                        end = MaterialTheme.spacing.Default,
                                        bottom = MaterialTheme.spacing.Default + bottomPadding
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
                                ) {
                                    uiState.expenseGroups.forEach { dateGroup ->
                                        stickyHeader(key = "header-${dateGroup.dateText}") {
                                            DateHeaderItem(
                                                dateText = dateGroup.dateText,
                                                formattedDayTotal = dateGroup.formattedDayTotal,
                                                formattedDayScheduled = dateGroup.formattedDayScheduled
                                            )
                                        }

                                        items(items = dateGroup.expenses, key = { it.id }) { expense ->
                                            ExpenseItem(
                                                expenseUiModel = expense,
                                                modifier = Modifier
                                                    .animateItem()
                                                    .sharedElementAnimation(
                                                        key = "expense-${expense.id}",
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        animatedVisibilityScope = animatedVisibilityScope
                                                    ),
                                                onClick = onExpenseClicked,
                                                onLongClick = {
                                                    if (!uiState.isGroupArchived) {
                                                        selectedExpenseForMenu = expense
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedExpenseForMenu?.let { expense ->
        val actions = buildList {
            add(
                SheetAction(
                    text = stringResource(R.string.action_edit_expense),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {
                        onEditExpenseClick(expense.id)
                        selectedExpenseForMenu = null
                    }
                )
            )
            if (expense.isRefundable && !expense.isCancelled) {
                add(
                    SheetAction(
                        text = stringResource(R.string.expense_detail_cancel_refund),
                        icon = TablerIcons.Outline.ReceiptRefund,
                        onClick = {
                            expenseToCancel = expense
                            selectedExpenseForMenu = null
                        }
                    )
                )
            }
            add(
                SheetAction(
                    text = stringResource(R.string.action_delete_expense),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {
                        expenseToDelete = expense
                        selectedExpenseForMenu = null
                    },
                    isDestructive = true
                )
            )
        }
        ActionBottomSheet(
            title = stringResource(R.string.expense_actions_title, expense.title),
            icon = TablerIcons.Outline.Receipt,
            actions = actions,
            onDismiss = { selectedExpenseForMenu = null }
        )
    }

    expenseToDelete?.let { expense ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.expense_delete_title),
            text = stringResource(R.string.expense_delete_warning, expense.title),
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                onDeleteExpense(expense.id)
                expenseToDelete = null
            }
        )
    }

    expenseToCancel?.let { expense ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.expense_cancel_dialog_title),
            text = stringResource(R.string.expense_cancel_dialog_message),
            confirmLabel = stringResource(R.string.expense_cancel_dialog_confirm),
            onDismiss = { expenseToCancel = null },
            onConfirm = {
                onCancelExpense(expense.id)
                expenseToCancel = null
            }
        )
    }

    if (showResetFiltersDialog) {
        ResetFiltersConfirmationDialog(
            onDismiss = { showResetFiltersDialog = false },
            onConfirm = {
                onResetFilters()
                showResetFiltersDialog = false
            }
        )
    }
}
