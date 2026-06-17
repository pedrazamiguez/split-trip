package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.rememberConnectedScrollBehavior
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.DateHeaderItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.list.ExpenseItem
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@OptIn(FlowPreview::class, ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(
    uiState: ExpensesUiState = ExpensesUiState(),
    onExpenseClicked: (String) -> Unit = { _ -> },
    onEditExpenseClick: (String) -> Unit = {},
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onDeleteExpense: (expenseId: String) -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current
    val scrollBehavior = rememberConnectedScrollBehavior()

    var selectedExpenseForMenu by remember { mutableStateOf<ExpenseUiModel?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseUiModel?>(null) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.scrollPosition,
        initialFirstVisibleItemScrollOffset = uiState.scrollOffset
    )

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(UiConstants.SCROLL_POSITION_DEBOUNCE_MS)
            .collect { (index, offset) ->
                onScrollPositionChanged(index, offset)
            }
    }

    val totalExpenseCount = uiState.expenseGroups.sumOf { it.expenses.size }
    LaunchedEffect(totalExpenseCount) {
        if (totalExpenseCount > 0 && !uiState.isLoading && listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
        DeferredLoadingContainer(
            isLoading = uiState.isLoading,
            loadingContent = { ShimmerLoadingList() }
        ) {
            when {
                uiState.isEmpty -> {
                    EmptyStateView(
                        title = stringResource(R.string.expenses_not_found),
                        icon = TablerIcons.Outline.Receipt
                    )
                }

                else -> {
                    val sharedTransitionScope = LocalSharedTransitionScope.current
                    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = MaterialTheme.spacing.Default,
                            top = MaterialTheme.spacing.Default,
                            end = MaterialTheme.spacing.Default,
                            bottom = MaterialTheme.spacing.Default + bottomPadding
                        ),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
                    ) {
                        uiState.expenseGroups.forEach { dateGroup ->
                            stickyHeader(key = "header-${dateGroup.dateText}") {
                                DateHeaderItem(
                                    dateText = dateGroup.dateText,
                                    formattedDayTotal = dateGroup.formattedDayTotal
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
                                    onLongClick = { selectedExpenseForMenu = expense }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedExpenseForMenu?.let { expense ->
        ActionBottomSheet(
            title = stringResource(R.string.expense_actions_title, expense.title),
            icon = TablerIcons.Outline.Receipt,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.action_edit_expense),
                    icon = TablerIcons.Outline.Edit,
                    onClick = {
                        onEditExpenseClick(expense.id)
                        selectedExpenseForMenu = null
                    }
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete_expense),
                    icon = TablerIcons.Outline.Trash,
                    onClick = {
                        expenseToDelete = expense
                        selectedExpenseForMenu = null
                    },
                    isDestructive = true
                )
            ),
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
}
