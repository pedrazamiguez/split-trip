package es.pedrazamiguez.splittrip.features.balance.navigation

import androidx.compose.runtime.remember
import androidx.navigation.NavGraphBuilder
import es.pedrazamiguez.splittrip.core.designsystem.extension.sharedComposable
import es.pedrazamiguez.splittrip.core.designsystem.navigation.LocalTabNavController
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.balance.presentation.feature.BalancesFeature
import es.pedrazamiguez.splittrip.features.balance.presentation.feature.SpendingInsightsFeature

internal const val KEY_EXPORT_REPORT_REQUESTED = "export_report_requested"

fun NavGraphBuilder.balancesGraph() {
    sharedComposable(route = Routes.BALANCES) { backStackEntry ->
        BalancesFeature(backStackEntry = backStackEntry)
    }
    spendingInsightsGraph()
}

fun NavGraphBuilder.spendingInsightsGraph() {
    sharedComposable(route = Routes.SPENDING_INSIGHTS) { backStackEntry ->
        val navController = LocalTabNavController.current
        // Freeze filter criteria at entry so pop-exit animations do not flash unfiltered data.
        val initialFilterCriteria = remember(backStackEntry) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<ExpenseFilterCriteria>(Routes.KEY_EXPENSE_FILTER_CRITERIA)
                ?: ExpenseFilterCriteria()
        }
        SpendingInsightsFeature(
            backStackEntry = backStackEntry,
            initialFilterCriteria = initialFilterCriteria
        )
    }
}
