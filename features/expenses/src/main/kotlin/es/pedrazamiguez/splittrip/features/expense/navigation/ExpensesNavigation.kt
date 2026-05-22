package es.pedrazamiguez.splittrip.features.expense.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.AddExpenseFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ExpenseDetailFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ExpensesFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ReceiptViewerFeature

fun NavGraphBuilder.expensesGraph() {
    sharedComposable(route = Routes.EXPENSES) {
        ExpensesFeature()
    }

    sharedComposable(route = Routes.ADD_EXPENSE) {
        val navController = LocalTabNavController.current
        AddExpenseFeature(
            onAddExpenseSuccess = {
                navController.popBackStack()
            }
        )
    }

    sharedComposable(
        route = Routes.EXPENSE_DETAIL,
        arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
    ) { backStackEntry ->
        val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@sharedComposable
        ExpenseDetailFeature(expenseId = expenseId)
    }

    sharedComposable(
        route = Routes.RECEIPT_VIEWER,
        arguments = listOf(navArgument("receiptUri") { type = NavType.StringType })
    ) { backStackEntry ->
        val receiptUri = backStackEntry.arguments?.getString("receiptUri") ?: return@sharedComposable
        val decodedUri = java.net.URLDecoder.decode(receiptUri, "UTF-8")
        ReceiptViewerFeature(receiptUri = decodedUri)
    }
}
