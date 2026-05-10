package es.pedrazamiguez.splittrip.features.expense.presentation.screen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedElementAnimation
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Plus
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Receipt
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalBottomPadding
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.DeferredLoadingContainer
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.EmptyStateView
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.ShimmerLoadingList
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.scaffold.StickyActionBar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ActionBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.SheetAction
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalAnimatedVisibilityScope
import es.pedrazamiguez.splittrip.core.designsystem.transition.LocalSharedTransitionScope
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.DateHeaderItem
import es.pedrazamiguez.splittrip.features.expense.presentation.component.ExpenseItem
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.ExpensesUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun ExpensesScreen(
    uiState: ExpensesUiState = ExpensesUiState(),
    onExpenseClicked: (String) -> Unit = { _ -> },
    onAddExpenseClick: () -> Unit = {},
    onScrollPositionChanged: (Int, Int) -> Unit = { _, _ -> },
    onDeleteExpense: (expenseId: String) -> Unit = {}
) {
    val bottomPadding = LocalBottomPadding.current

    // Local UI State for overlays (Action Sheet & Confirmation Dialog)
    var selectedExpenseForMenu by remember { mutableStateOf<ExpenseUiModel?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseUiModel?>(null) }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = uiState.scrollPosition,
        initialFirstVisibleItemScrollOffset = uiState.scrollOffset
    )

    ExpensesScrollEffects(
        uiState = uiState,
        listState = listState,
        onScrollPositionChanged = onScrollPositionChanged
    )

    ExpensesScreenContent(
        uiState = uiState,
        listState = listState,
        bottomPadding = bottomPadding,
        onExpenseClicked = onExpenseClicked,
        onAddExpenseClick = onAddExpenseClick,
        onExpenseLongClicked = { selectedExpenseForMenu = it }
    )

    ExpensesScreenOverlays(
        selectedExpense = selectedExpenseForMenu,
        expenseToDelete = expenseToDelete,
        onDeleteExpense = onDeleteExpense,
        onMenuDismiss = { selectedExpenseForMenu = null },
        onDeleteRequested = { expense ->
            expenseToDelete = expense
            selectedExpenseForMenu = null
        },
        onDeleteDismiss = { expenseToDelete = null }
    )
}

@OptIn(FlowPreview::class)
@Composable
private fun ExpensesScrollEffects(
    uiState: ExpensesUiState,
    listState: LazyListState,
    onScrollPositionChanged: (Int, Int) -> Unit
) {
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .debounce(UiConstants.SCROLL_POSITION_DEBOUNCE_MS)
            .collect { (index, offset) ->
                onScrollPositionChanged(index, offset)
            }
    }

    // Auto-scroll to top when a new expense is added (list size increases)
    val totalExpenseCount = uiState.expenseGroups.sumOf { it.expenses.size }
    LaunchedEffect(totalExpenseCount) {
        if (totalExpenseCount > 0 && !uiState.isLoading && listState.firstVisibleItemIndex > 0) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun ExpensesScreenContent(
    uiState: ExpensesUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    onExpenseClicked: (String) -> Unit,
    onAddExpenseClick: () -> Unit,
    onExpenseLongClicked: (ExpenseUiModel) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                    ExpensesListContent(
                        expenseGroups = uiState.expenseGroups,
                        listState = listState,
                        bottomPadding = bottomPadding,
                        onExpenseClicked = onExpenseClicked,
                        onExpenseLongClicked = onExpenseLongClicked
                    )
                }
            }
        }

        StickyActionBar(
            text = stringResource(R.string.expenses_add),
            icon = TablerIcons.Outline.Plus,
            onClick = onAddExpenseClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = MaterialTheme.spacing.ExtraLarge)
                .padding(bottom = bottomPadding + MaterialTheme.spacing.ExtraSmall),
            sharedTransitionKey = ADD_EXPENSE_SHARED_ELEMENT_KEY
        )
    }
}

@Composable
private fun ExpensesScreenOverlays(
    selectedExpense: ExpenseUiModel?,
    expenseToDelete: ExpenseUiModel?,
    onDeleteExpense: (String) -> Unit,
    onMenuDismiss: () -> Unit,
    onDeleteRequested: (ExpenseUiModel) -> Unit,
    onDeleteDismiss: () -> Unit
) {
    selectedExpense?.let { expense ->
        ActionBottomSheet(
            title = stringResource(R.string.expense_actions_title, expense.title),
            icon = TablerIcons.Outline.Receipt,
            actions = listOf(
                SheetAction(
                    text = stringResource(R.string.action_edit_expense),
                    icon = TablerIcons.Outline.Edit,
                    onClick = { onMenuDismiss() }
                ),
                SheetAction(
                    text = stringResource(R.string.action_delete_expense),
                    icon = TablerIcons.Outline.Trash,
                    onClick = { onDeleteRequested(expense) },
                    isDestructive = true
                )
            ),
            onDismiss = onMenuDismiss
        )
    }

    expenseToDelete?.let { expense ->
        DestructiveConfirmationDialog(
            title = stringResource(R.string.expense_delete_title),
            text = stringResource(R.string.expense_delete_warning, expense.title),
            onDismiss = onDeleteDismiss,
            onConfirm = {
                onDeleteExpense(expense.id)
                onDeleteDismiss()
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpensesListContent(
    expenseGroups: ImmutableList<ExpenseDateGroupUiModel>,
    listState: LazyListState,
    bottomPadding: Dp,
    onExpenseClicked: (String) -> Unit,
    onExpenseLongClicked: (ExpenseUiModel) -> Unit
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    val fabExtraPadding = 72.dp // Space for StickyActionBar
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaterialTheme.spacing.Default,
            top = MaterialTheme.spacing.Default,
            end = MaterialTheme.spacing.Default,
            bottom = MaterialTheme.spacing.Default + bottomPadding + fabExtraPadding
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        item(key = "header") {
            Column {
                Text(
                    text = stringResource(R.string.expenses_title),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.expenses_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        expenseGroups.forEach { dateGroup ->
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
                    onLongClick = { onExpenseLongClicked(expense) }
                )
            }
        }
    }
}
