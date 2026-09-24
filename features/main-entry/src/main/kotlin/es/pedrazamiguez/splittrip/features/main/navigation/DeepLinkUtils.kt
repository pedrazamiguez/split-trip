package es.pedrazamiguez.splittrip.features.main.navigation

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes

/**
 * Pure utility functions for deep link argument resolution.
 *
 * Determines which tab to auto-switch to based on the deep link arguments
 * extracted from the URI by Navigation Compose.
 */
object DeepLinkUtils {

    /**
     * Deep link URI scheme used by the app.
     */
    const val DEEP_LINK_SCHEME = "splittrip"

    /**
     * URI patterns for deep link matching in Navigation Compose.
     */
    const val PATTERN_GROUP = "$DEEP_LINK_SCHEME://groups/{groupId}"
    const val PATTERN_GROUPS = "$DEEP_LINK_SCHEME://groups"
    const val PATTERN_EXPENSES = "$DEEP_LINK_SCHEME://groups/{groupId}/expenses"
    const val PATTERN_EXPENSE_DETAIL = "$DEEP_LINK_SCHEME://groups/{groupId}/expenses/{expenseId}"
    const val PATTERN_CONTRIBUTION = "$DEEP_LINK_SCHEME://groups/{groupId}/contributions/{contributionId}"
    const val PATTERN_CASH_WITHDRAWAL = "$DEEP_LINK_SCHEME://groups/{groupId}/cash_withdrawals/{withdrawalId}"
    const val PATTERN_SETTLEMENT = "$DEEP_LINK_SCHEME://groups/{groupId}/settlements/{settlementId}"
    const val PATTERN_YOUR_POSITION = "$DEEP_LINK_SCHEME://groups/{groupId}/your-position"
    const val PATTERN_MEMBERS = "$DEEP_LINK_SCHEME://groups/{groupId}/members"

    /**
     * Navigation argument keys extracted from deep link URIs.
     */
    const val ARG_GROUP_ID = "groupId"
    const val ARG_EXPENSE_ID = "expenseId"
    const val ARG_CONTRIBUTION_ID = "contributionId"
    const val ARG_WITHDRAWAL_ID = "withdrawalId"
    const val ARG_SETTLEMENT_ID = "settlementId"

    /**
     * Resolves which tab to auto-switch to based on which deep link arguments are present.
     *
     * | Deep link pattern                                      | Target tab       |
     * |--------------------------------------------------------|------------------|
     * | `groups`                                               | `Routes.GROUPS`  |
     * | `groups/{groupId}`                                     | `Routes.GROUPS`  |
     * | `groups/{groupId}/members`                             | `Routes.GROUPS`  |
     * | `groups/{groupId}/expenses`                            | `Routes.EXPENSES`|
     * | `groups/{groupId}/expenses/{expenseId}`                | `Routes.EXPENSES`|
     * | `groups/{groupId}/contributions/{contributionId}`      | `Routes.BALANCES`|
     * | `groups/{groupId}/cash_withdrawals/{withdrawalId}`     | `Routes.BALANCES`|
     * | `groups/{groupId}/settlements/{settlementId}`          | `Routes.BALANCES`|
     * | `groups/{groupId}/your-position`                       | `Routes.BALANCES`|
     *
     * @param params The extracted deep link parameters [DeepLinkResolutionParams].
     * @return The route of the tab to switch to, defaulting to [Routes.GROUPS].
     */
    fun resolveTargetTab(params: DeepLinkResolutionParams = DeepLinkResolutionParams()): String = when {
        params.expenseId != null -> Routes.EXPENSES
        params.isExpensesListPath -> Routes.EXPENSES
        params.contributionId != null -> Routes.BALANCES
        params.withdrawalId != null -> Routes.BALANCES
        params.settlementId != null -> Routes.BALANCES
        params.isYourPositionPath -> Routes.BALANCES
        params.isMembersPath -> Routes.GROUPS
        params.isGroupsListPath -> Routes.GROUPS
        else -> Routes.GROUPS
    }

    /**
     * Resolves the in-tab destination route for a deep link, if any.
     *
     * @param groupId The group ID from the deep link, or `null`.
     * @param expenseId The expense ID from the deep link, or `null`.
     * @param contributionId The contribution ID from the deep link, or `null`.
     * @param settlementId The settlement ID from the deep link, or `null`.
     * @param isYourPositionPath `true` when the deep link matched the `/your-position` path.
     * @param isMembersPath `true` when the deep link matched the `/members` path.
     * @return The sub-destination route inside the target tab, or `null` if the deep link
     *   only targets the tab root.
     */
    fun resolveInTabDestination(
        groupId: String? = null,
        expenseId: String? = null,
        contributionId: String? = null,
        settlementId: String? = null,
        isYourPositionPath: Boolean = false,
        isMembersPath: Boolean = false
    ): String? = when {
        expenseId != null -> Routes.expenseDetailRoute(expenseId)
        contributionId != null && groupId != null -> Routes.contributionDetailRoute(groupId, contributionId)
        settlementId != null || isYourPositionPath -> Routes.YOUR_POSITION
        isMembersPath && groupId != null -> Routes.groupDetailRoute(groupId)
        else -> null
    }
}
