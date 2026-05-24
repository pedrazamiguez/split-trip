package es.pedrazamiguez.splittrip.features.expense.presentation.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.pedrazamiguez.splittrip.core.common.presentation.asString
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.notification.LocalTopPillController
import es.pedrazamiguez.splittrip.features.expense.presentation.screen.ExpenseDetailScreen
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.ExpenseDetailViewModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpenseDetailUiAction
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpenseDetailFeature(
    expenseId: String,
    expenseDetailViewModel: ExpenseDetailViewModel = koinViewModel<ExpenseDetailViewModel>()
) {
    val navController = LocalTabNavController.current
    val pillController = LocalTopPillController.current
    val context = LocalContext.current

    val uiState by expenseDetailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(expenseId) {
        expenseDetailViewModel.setExpenseId(expenseId)
    }

    LaunchedEffect(Unit) {
        expenseDetailViewModel.actions.collectLatest { action ->
            when (action) {
                is ExpenseDetailUiAction.ShowError -> {
                    pillController.showPill(message = action.message.asString(context))
                }
                is ExpenseDetailUiAction.DeleteSuccess -> {
                    pillController.showPill(message = action.message.asString(context))
                    navController.popBackStack()
                }
            }
        }
    }

    val expense = uiState.expense
    ExpenseDetailScreen(
        uiState = uiState,
        onReceiptTap = expense?.receiptUri?.let { uri ->
            { navController.navigate(Routes.receiptViewerRoute(uri, expense.receiptMimeType)) }
        }
    )
}
