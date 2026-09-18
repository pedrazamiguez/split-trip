package es.pedrazamiguez.splittrip.features.main.navigation

import androidx.activity.compose.LocalActivity
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import es.pedrazamiguez.splittrip.core.designsystem.navigation.NavigationProvider
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.screen.ScreenUiProvider
import es.pedrazamiguez.splittrip.features.main.presentation.screen.MainScreen

@Suppress("LongMethod") // Navigation graph builder DSL
fun NavGraphBuilder.mainGraph(
    navigationProviders: List<NavigationProvider>,
    screenUiProviders: List<ScreenUiProvider>
) {
    composable(
        route = Routes.MAIN,
        deepLinks = listOf(
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_GROUP },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_EXPENSES },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_EXPENSE_DETAIL },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_CONTRIBUTION },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_CASH_WITHDRAWAL },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_SETTLEMENT },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_YOUR_POSITION }
        ),
        arguments = listOf(
            navArgument(DeepLinkUtils.ARG_GROUP_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(DeepLinkUtils.ARG_EXPENSE_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(DeepLinkUtils.ARG_CONTRIBUTION_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(DeepLinkUtils.ARG_WITHDRAWAL_ID) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(DeepLinkUtils.ARG_SETTLEMENT_ID) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) { backStackEntry ->
        val deepLinkGroupId = backStackEntry.arguments
            ?.getString(DeepLinkUtils.ARG_GROUP_ID)?.ifBlank { null }
        val deepLinkExpenseId = backStackEntry.arguments
            ?.getString(DeepLinkUtils.ARG_EXPENSE_ID)?.ifBlank { null }
        val deepLinkContributionId = backStackEntry.arguments
            ?.getString(DeepLinkUtils.ARG_CONTRIBUTION_ID)?.ifBlank { null }
        val deepLinkWithdrawalId = backStackEntry.arguments
            ?.getString(DeepLinkUtils.ARG_WITHDRAWAL_ID)?.ifBlank { null }
        val deepLinkSettlementId = backStackEntry.arguments
            ?.getString(DeepLinkUtils.ARG_SETTLEMENT_ID)?.ifBlank { null }

        // Detect the expenses-list deep link (groups/{groupId}/expenses) and
        // your-position deep link (groups/{groupId}/your-position) by inspecting
        // the Activity intent URI. Arguments alone can't distinguish these from the
        // group-only deep link (groups/{groupId}) because they produce the same state.
        val intentUri = LocalActivity.current?.intent?.data
        val isExpensesListPath = deepLinkGroupId != null &&
            deepLinkExpenseId == null &&
            deepLinkContributionId == null &&
            deepLinkWithdrawalId == null &&
            deepLinkSettlementId == null &&
            intentUri?.pathSegments?.lastOrNull() == "expenses"

        val isYourPositionPath = deepLinkGroupId != null &&
            deepLinkExpenseId == null &&
            deepLinkContributionId == null &&
            deepLinkWithdrawalId == null &&
            deepLinkSettlementId == null &&
            intentUri?.pathSegments?.lastOrNull() == "your-position"

        // Resolve target tab only when a deep link group is present
        val deepLinkTargetTab = if (deepLinkGroupId != null) {
            DeepLinkUtils.resolveTargetTab(
                expenseId = deepLinkExpenseId,
                isExpensesListPath = isExpensesListPath,
                contributionId = deepLinkContributionId,
                withdrawalId = deepLinkWithdrawalId,
                settlementId = deepLinkSettlementId,
                isYourPositionPath = isYourPositionPath
            )
        } else {
            null
        }

        val deepLinkInTabDestination = if (deepLinkGroupId != null) {
            DeepLinkUtils.resolveInTabDestination(
                expenseId = deepLinkExpenseId,
                settlementId = deepLinkSettlementId,
                isYourPositionPath = isYourPositionPath
            )
        } else {
            null
        }

        MainScreen(
            navigationProviders = navigationProviders,
            screenUiProviders = screenUiProviders,
            deepLinkGroupId = deepLinkGroupId,
            deepLinkTargetTab = deepLinkTargetTab,
            deepLinkInTabDestination = deepLinkInTabDestination
        )
    }
}
