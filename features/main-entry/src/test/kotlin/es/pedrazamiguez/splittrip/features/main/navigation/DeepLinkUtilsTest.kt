package es.pedrazamiguez.splittrip.features.main.navigation

import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DeepLinkUtils")
class DeepLinkUtilsTest {

    @Nested
    @DisplayName("resolveTargetTab")
    inner class ResolveTargetTab {

        @Test
        fun `returns EXPENSES when expenseId is present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = "expense-123",
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = null
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `returns EXPENSES when isExpensesListPath is true`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = true,
                    contributionId = null,
                    withdrawalId = null
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `returns BALANCES when contributionId is present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = "contribution-456",
                    withdrawalId = null
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `returns BALANCES when withdrawalId is present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = "withdrawal-789"
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `returns BALANCES when settlementId is present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = null,
                    settlementId = "settlement-101"
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `returns BALANCES when isYourPositionPath is true`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = null,
                    settlementId = null,
                    isYourPositionPath = true
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `returns GROUPS when no specific ID is present and not expenses path`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = null
                )
            )
            assertEquals(Routes.GROUPS, result)
        }

        @Test
        fun `prioritizes EXPENSES when both expenseId and contributionId are present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = "expense-123",
                    isExpensesListPath = false,
                    contributionId = "contribution-456",
                    withdrawalId = null
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `prioritizes EXPENSES when both expenseId and withdrawalId are present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = "expense-123",
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = "withdrawal-789"
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `prioritizes EXPENSES when both expenseId and settlementId are present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = "expense-123",
                    isExpensesListPath = false,
                    contributionId = null,
                    withdrawalId = null,
                    settlementId = "settlement-101"
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `returns BALANCES when both contributionId and withdrawalId are present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = "contribution-456",
                    withdrawalId = "withdrawal-789"
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `returns BALANCES when both settlementId and contributionId are present`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = null,
                    isExpensesListPath = false,
                    contributionId = "contribution-456",
                    withdrawalId = null,
                    settlementId = "settlement-101"
                )
            )
            assertEquals(Routes.BALANCES, result)
        }

        @Test
        fun `isExpensesListPath is overridden by expenseId`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    expenseId = "expense-123",
                    isExpensesListPath = true,
                    contributionId = null,
                    withdrawalId = null
                )
            )
            assertEquals(Routes.EXPENSES, result)
        }

        @Test
        fun `returns GROUPS when isGroupsListPath is true`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    isGroupsListPath = true
                )
            )
            assertEquals(Routes.GROUPS, result)
        }

        @Test
        fun `returns GROUPS when isMembersPath is true`() {
            val result = DeepLinkUtils.resolveTargetTab(
                DeepLinkResolutionParams(
                    isMembersPath = true
                )
            )
            assertEquals(Routes.GROUPS, result)
        }
    }

    @Nested
    @DisplayName("resolveInTabDestination")
    inner class ResolveInTabDestination {

        @Test
        fun `returns expenseDetailRoute when expenseId is present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                expenseId = "expense-123"
            )
            assertEquals(Routes.expenseDetailRoute("expense-123"), result)
        }

        @Test
        fun `returns contributionDetailRoute when contributionId and groupId are present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                groupId = "group-123",
                contributionId = "contribution-456"
            )
            assertEquals(Routes.contributionDetailRoute("group-123", "contribution-456"), result)
        }

        @Test
        fun `returns null when contributionId is present but groupId is null`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                contributionId = "contribution-456"
            )
            assertNull(result)
        }

        @Test
        fun `returns groupDetailRoute when isMembersPath is true and groupId is present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                groupId = "group-123",
                isMembersPath = true
            )
            assertEquals(Routes.groupDetailRoute("group-123"), result)
        }

        @Test
        fun `returns null when isMembersPath is true but groupId is null`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                isMembersPath = true
            )
            assertNull(result)
        }

        @Test
        fun `returns YOUR_POSITION when settlementId is present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                settlementId = "settlement-456"
            )
            assertEquals(Routes.YOUR_POSITION, result)
        }

        @Test
        fun `returns YOUR_POSITION when isYourPositionPath is true`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                isYourPositionPath = true
            )
            assertEquals(Routes.YOUR_POSITION, result)
        }

        @Test
        fun `returns null when no in-tab destination args are present`() {
            val result = DeepLinkUtils.resolveInTabDestination()
            assertNull(result)
        }

        @Test
        fun `returns null when isExpensesListPath is true without expenseId`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                expenseId = null,
                settlementId = null,
                isYourPositionPath = false
            )
            assertNull(result)
        }

        @Test
        fun `prioritizes expenseId when both expenseId and settlementId are present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                expenseId = "expense-123",
                settlementId = "settlement-456"
            )
            assertEquals(Routes.expenseDetailRoute("expense-123"), result)
        }

        @Test
        fun `prioritizes expenseId when both expenseId and contributionId are present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                groupId = "group-123",
                expenseId = "expense-123",
                contributionId = "contribution-456"
            )
            assertEquals(Routes.expenseDetailRoute("expense-123"), result)
        }

        @Test
        fun `prioritizes contributionId when both contributionId and settlementId are present`() {
            val result = DeepLinkUtils.resolveInTabDestination(
                groupId = "group-123",
                contributionId = "contribution-456",
                settlementId = "settlement-456"
            )
            assertEquals(Routes.contributionDetailRoute("group-123", "contribution-456"), result)
        }
    }

    @Nested
    @DisplayName("constants")
    inner class Constants {

        @Test
        fun `DEEP_LINK_SCHEME is splittrip`() {
            assertEquals("splittrip", DeepLinkUtils.DEEP_LINK_SCHEME)
        }

        @Test
        fun `PATTERN_GROUP contains groupId placeholder`() {
            assertEquals(
                "splittrip://groups/{groupId}",
                DeepLinkUtils.PATTERN_GROUP
            )
        }

        @Test
        fun `PATTERN_GROUPS is splittrip groups`() {
            assertEquals(
                "splittrip://groups",
                DeepLinkUtils.PATTERN_GROUPS
            )
        }

        @Test
        fun `PATTERN_EXPENSES contains groupId placeholder`() {
            assertEquals(
                "splittrip://groups/{groupId}/expenses",
                DeepLinkUtils.PATTERN_EXPENSES
            )
        }

        @Test
        fun `PATTERN_EXPENSE_DETAIL contains groupId and expenseId placeholders`() {
            assertEquals(
                "splittrip://groups/{groupId}/expenses/{expenseId}",
                DeepLinkUtils.PATTERN_EXPENSE_DETAIL
            )
        }

        @Test
        fun `PATTERN_CONTRIBUTION contains groupId and contributionId placeholders`() {
            assertEquals(
                "splittrip://groups/{groupId}/contributions/{contributionId}",
                DeepLinkUtils.PATTERN_CONTRIBUTION
            )
        }

        @Test
        fun `PATTERN_CASH_WITHDRAWAL contains groupId and withdrawalId placeholders`() {
            assertEquals(
                "splittrip://groups/{groupId}/cash_withdrawals/{withdrawalId}",
                DeepLinkUtils.PATTERN_CASH_WITHDRAWAL
            )
        }

        @Test
        fun `PATTERN_SETTLEMENT contains groupId and settlementId placeholders`() {
            assertEquals(
                "splittrip://groups/{groupId}/settlements/{settlementId}",
                DeepLinkUtils.PATTERN_SETTLEMENT
            )
        }

        @Test
        fun `PATTERN_YOUR_POSITION contains groupId placeholder`() {
            assertEquals(
                "splittrip://groups/{groupId}/your-position",
                DeepLinkUtils.PATTERN_YOUR_POSITION
            )
        }

        @Test
        fun `PATTERN_MEMBERS contains groupId placeholder`() {
            assertEquals(
                "splittrip://groups/{groupId}/members",
                DeepLinkUtils.PATTERN_MEMBERS
            )
        }

        @Test
        fun `ARG_SETTLEMENT_ID is settlementId`() {
            assertEquals("settlementId", DeepLinkUtils.ARG_SETTLEMENT_ID)
        }
    }
}
