package es.pedrazamiguez.splittrip.features.expense.presentation.screen.impl

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Edit
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.ReceiptRefund
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.dialog.DestructiveConfirmationDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.topbar.DynamicTopAppBar
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpenseDetailViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpenseDetailUiEvent
import org.koin.androidx.compose.koinViewModel

class ExpenseDetailScreenUiProviderImpl(
    override val route: String = Routes.EXPENSE_DETAIL
) : ScreenUiProvider {

    @OptIn(ExperimentalMaterial3Api::class)
    override val topBar: @Composable () -> Unit = {
        val navController = LocalTabNavController.current
        // Resolve the same ViewModel instance the Feature created inside the NavHost.
        val backStackEntry = navController.currentBackStackEntry
        if (backStackEntry != null) {
            val vm: ExpenseDetailViewModel = koinViewModel(viewModelStoreOwner = backStackEntry)
            val uiState by vm.uiState.collectAsStateWithLifecycle()
            var showDeleteDialog by remember { mutableStateOf(false) }
            var showCancelDialog by remember { mutableStateOf(false) }

            val expenseId = backStackEntry.arguments?.getString("expenseId")
            val expense = uiState.expense

            val titleText = if (expense?.isCancelled == true) {
                val suffix = stringResource(R.string.payment_status_cancelled)
                "${expense.title} ($suffix)"
            } else {
                expense?.title ?: ""
            }

            DynamicTopAppBar(
                title = titleText,
                subtitle = expense?.categoryText?.takeIf { it.isNotEmpty() },
                onBack = { navController.popBackStack() },
                actions = {
                    if (!uiState.isGroupArchived) {
                        if (expense?.isCancelled == false) {
                            IconButton(
                                onClick = {
                                    if (expenseId != null) {
                                        navController.navigate(Routes.editExpenseRoute(expenseId))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Outline.Edit,
                                    contentDescription = stringResource(R.string.action_edit_expense)
                                )
                            }
                        }
                        if (expense?.isRefundable == true) {
                            IconButton(onClick = { showCancelDialog = true }) {
                                Icon(
                                    imageVector = TablerIcons.Outline.ReceiptRefund,
                                    contentDescription = stringResource(R.string.expense_detail_cancel_refund)
                                )
                            }
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = TablerIcons.Outline.Trash,
                                contentDescription = stringResource(R.string.action_delete_expense)
                            )
                        }
                    }
                }
            )

            if (showDeleteDialog && expense != null) {
                DestructiveConfirmationDialog(
                    title = stringResource(R.string.expense_delete_title),
                    text = stringResource(R.string.expense_delete_warning, expense.title),
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        vm.onEvent(ExpenseDetailUiEvent.DeleteConfirmed)
                        showDeleteDialog = false
                    }
                )
            }

            if (showCancelDialog && expense != null) {
                DestructiveConfirmationDialog(
                    title = stringResource(R.string.expense_cancel_dialog_title),
                    text = stringResource(R.string.expense_cancel_dialog_message),
                    confirmLabel = stringResource(R.string.expense_cancel_dialog_confirm),
                    onDismiss = { showCancelDialog = false },
                    onConfirm = {
                        vm.onEvent(ExpenseDetailUiEvent.CancelConfirmed)
                        showCancelDialog = false
                    }
                )
            }
        } else {
            DynamicTopAppBar(
                title = "",
                onBack = { navController.popBackStack() }
            )
        }
    }
}
