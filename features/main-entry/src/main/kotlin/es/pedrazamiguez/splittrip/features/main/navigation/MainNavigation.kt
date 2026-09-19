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
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_GROUPS },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_GROUP },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_EXPENSES },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_EXPENSE_DETAIL },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_CONTRIBUTION },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_CASH_WITHDRAWAL },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_SETTLEMENT },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_YOUR_POSITION },
            navDeepLink { uriPattern = DeepLinkUtils.PATTERN_MEMBERS }
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

        // Detect non-entity deep links (groups, expenses-list, your-position, members)
        // by inspecting the Activity intent URI. Arguments alone can't distinguish these from the
        // group-only deep link (groups/{groupId}) because they produce the same state.
        val intentUri = LocalActivity.current?.intent?.data
        val lastPathSegment = intentUri?.pathSegments?.lastOrNull()
        val isNoEntityDeepLink = deepLinkExpenseId == null &&
            deepLinkContributionId == null &&
            deepLinkWithdrawalId == null &&
            deepLinkSettlementId == null

        val isExpensesListPath = deepLinkGroupId != null &&
            isNoEntityDeepLink &&
            lastPathSegment == "expenses"

        val isYourPositionPath = deepLinkGroupId != null &&
            isNoEntityDeepLink &&
            lastPathSegment == "your-position"

        val isMembersPath = deepLinkGroupId != null &&
            isNoEntityDeepLink &&
            lastPathSegment == "members"

        val isGroupsListPath = deepLinkGroupId == null &&
            lastPathSegment == "groups"

        // Resolve target tab when a deep link group is present or targeting the groups list
        val deepLinkTargetTab = if (deepLinkGroupId != null || isGroupsListPath) {
            DeepLinkUtils.resolveTargetTab(
                expenseId = deepLinkExpenseId,
                isExpensesListPath = isExpensesListPath,
                contributionId = deepLinkContributionId,
                withdrawalId = deepLinkWithdrawalId,
                settlementId = deepLinkSettlementId,
                isYourPositionPath = isYourPositionPath,
                isGroupsListPath = isGroupsListPath,
                isMembersPath = isMembersPath
            )
        } else {
            null
        }

        val deepLinkInTabDestination = if (deepLinkGroupId != null) {
            DeepLinkUtils.resolveInTabDestination(
                groupId = deepLinkGroupId,
                expenseId = deepLinkExpenseId,
                contributionId = deepLinkContributionId,
                settlementId = deepLinkSettlementId,
                isYourPositionPath = isYourPositionPath,
                isMembersPath = isMembersPath
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
