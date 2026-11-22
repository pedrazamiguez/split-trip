package es.pedrazamiguez.splittrip.features.expense.navigation

import android.app.Activity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import es.pedrazamiguez.splittrip.core.designsystem.ad.InterstitialAdManager
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.AddExpenseFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ExpenseDetailFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ExpensesFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ExpensesFilterFeature
import es.pedrazamiguez.splittrip.features.expense.presentation.feature.ReceiptViewerFeature
import org.koin.compose.getKoin

internal const val KEY_APPLIED_FILTER_CRITERIA = "appliedFilterCriteria"
internal const val KEY_EXPENSE_ADDED = "expenseAdded"
internal const val KEY_RESET_FILTERS_REQUESTED = "resetFiltersRequested"

@Suppress("LongMethod")
fun NavGraphBuilder.expensesGraph() {
    sharedComposable(route = Routes.EXPENSES) { backStackEntry ->
        ExpensesFeature(backStackEntry = backStackEntry)
    }

    sharedComposable(route = Routes.EXPENSES_FILTER) {
        val navController = LocalTabNavController.current
        val initialCriteria = navController.previousBackStackEntry
            ?.savedStateHandle
            ?.get<ExpenseFilterCriteria>(Routes.KEY_EXPENSE_FILTER_CRITERIA) ?: ExpenseFilterCriteria()
        ExpensesFilterFeature(
            initialCriteria = initialCriteria,
            onApplyFilters = { appliedCriteria ->
                val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                savedStateHandle?.set(KEY_APPLIED_FILTER_CRITERIA, appliedCriteria)
                savedStateHandle?.set(Routes.KEY_EXPENSE_FILTER_CRITERIA, appliedCriteria)
                navController.popBackStack()
            },
            onFiltersReset = { clearedCriteria ->
                val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                savedStateHandle?.set(KEY_APPLIED_FILTER_CRITERIA, clearedCriteria)
                savedStateHandle?.set(Routes.KEY_EXPENSE_FILTER_CRITERIA, clearedCriteria)
                navController.popBackStack()
            },
            onNavigateBack = {
                navController.popBackStack()
            }
        )
    }

    sharedComposable(route = Routes.ADD_EXPENSE) {
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        AddExpenseFeature(
            onAddExpenseSuccess = {
                navController.previousBackStackEntry?.savedStateHandle?.set(KEY_EXPENSE_ADDED, true)
                val activity = context as? Activity
                if (activity != null) {
                    interstitialAdManager.onActionCompleted(activity) {
                        navController.popBackStack()
                    }
                } else {
                    navController.popBackStack()
                }
            }
        )
    }

    expenseDetailGraph()
}

fun NavGraphBuilder.expenseDetailGraph() {
    sharedComposable(
        route = Routes.EDIT_EXPENSE,
        arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
    ) { backStackEntry ->
        val expenseId = backStackEntry.arguments?.getString("expenseId") ?: return@sharedComposable
        val navController = LocalTabNavController.current
        val context = LocalContext.current
        val koin = getKoin()
        val interstitialAdManager = remember(koin) { koin.get<InterstitialAdManager>() }
        AddExpenseFeature(
            expenseId = expenseId,
            onAddExpenseSuccess = {
                val activity = context as? Activity
                if (activity != null) {
                    interstitialAdManager.onActionCompleted(activity) {
                        navController.popBackStack()
                    }
                } else {
                    navController.popBackStack()
                }
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
        arguments = listOf(
            navArgument("receiptUri") { type = NavType.StringType },
            navArgument("mimeType") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        val receiptUri = backStackEntry.arguments?.getString("receiptUri") ?: return@sharedComposable
        val mimeType = backStackEntry.arguments?.getString("mimeType")
        ReceiptViewerFeature(receiptUri = receiptUri, mimeType = mimeType)
    }
}
