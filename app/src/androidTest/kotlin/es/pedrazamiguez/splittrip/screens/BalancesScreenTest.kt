package es.pedrazamiguez.splittrip.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.MemberDisplay
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.GroupPocketBalanceCard
import es.pedrazamiguez.splittrip.features.balance.presentation.component.MemberBalanceItem
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.BalancesScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val NARROW_SCREEN_WIDTH = 320.dp
private val COMPACT_CARD_CONTAINER_WIDTH = 328.dp
private val NARROW_CARD_CONTAINER_WIDTH = 288.dp
private val SINGLE_LINE_LABEL_MAX_HEIGHT = 24.dp

/**
 * Smoke tests for empty states on [BalancesScreen].
 */
@RunWith(AndroidJUnit4::class)
class BalancesScreenTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun rendersBalancesScreen_emptyState_displaysOutOfPocketAndPocketPrompt() {
        val emptyTitle = context.getString(R.string.balances_empty_title)
        val emptyDescription = context.getString(R.string.balances_empty_description)

        composeRule.setContent {
            SplitTripTheme {
                BalancesScreen(
                    uiState = BalancesUiState(
                        isLoading = false,
                        groupId = "group-1",
                        pocketBalance = GroupPocketBalanceUiModel(),
                        activityItems = persistentListOf()
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).assertIsDisplayed()
    }

    @Test
    fun rendersMemberBalanceItem_narrowWidth_displaysBreakdownWithoutCharacterWrapping() {
        val contributedLabel = context.getString(R.string.balances_member_contributed_label)
        val cashInHandLabel = context.getString(R.string.balances_member_cash_in_hand_label)
        val spentLabel = context.getString(R.string.balances_member_spent_label)
        val formattedRon = "RON0.00"

        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(NARROW_SCREEN_WIDTH)) {
                    MemberBalanceItem(
                        memberBalance = MemberBalanceUiModel(
                            userId = "user-1",
                            memberDisplay = MemberDisplay.Active("user-1", "Alex"),
                            formattedContributed = formattedRon,
                            formattedCashInHand = formattedRon,
                            formattedTotalSpent = formattedRon,
                            formattedPocketBalance = formattedRon,
                            formattedTotalBalance = "RON50.00"
                        )
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(contributedLabel).assertIsDisplayed()
        composeRule.onNodeWithText(cashInHandLabel).assertIsDisplayed()
        val spentNode = composeRule.onNodeWithText(spentLabel)
        spentNode.assertIsDisplayed()
        composeRule.onAllNodesWithText(formattedRon).assertCountEquals(3)

        val spentBounds = spentNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Expected '$spentLabel' on a single line " +
                "(height <= $SINGLE_LINE_LABEL_MAX_HEIGHT), got ${spentBounds.height}",
            spentBounds.height <= SINGLE_LINE_LABEL_MAX_HEIGHT
        )
    }

    @Test
    fun rendersGroupPocketBalanceCard_compactWidth_displaysActionButtonsSideBySide() {
        val contributeText = context.getString(R.string.balances_add_money)
        val withdrawText = context.getString(R.string.balances_withdraw_cash)
        var addMoneyClicks = 0
        var withdrawClicks = 0

        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(COMPACT_CARD_CONTAINER_WIDTH)) {
                    GroupPocketBalanceCard(
                        balance = GroupPocketBalanceUiModel(
                            groupName = "Romania",
                            formattedBalance = "RON120.00",
                            formattedTotalContributed = "RON200.00",
                            formattedTotalSpent = "RON80.00",
                            currency = "RON"
                        ),
                        onAddMoney = { addMoneyClicks++ },
                        onWithdrawCash = { withdrawClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val contributeNode = composeRule.onNodeWithText(contributeText)
        val withdrawNode = composeRule.onNodeWithText(withdrawText)
        contributeNode.assertIsDisplayed()
        withdrawNode.assertIsDisplayed()

        val contributeBounds = contributeNode.getUnclippedBoundsInRoot()
        val withdrawBounds = withdrawNode.getUnclippedBoundsInRoot()
        assertEquals(contributeBounds.top, withdrawBounds.top)

        contributeNode.performClick()
        withdrawNode.performClick()
        composeRule.waitForIdle()

        assertEquals(1, addMoneyClicks)
        assertEquals(1, withdrawClicks)
    }

    @Test
    fun rendersGroupPocketBalanceCard_narrowWidth_stacksActionButtonsWithoutTruncation() {
        val contributeText = context.getString(R.string.balances_add_money)
        val withdrawText = context.getString(R.string.balances_withdraw_cash)
        var addMoneyClicks = 0
        var withdrawClicks = 0

        composeRule.setContent {
            SplitTripTheme {
                Box(modifier = Modifier.width(NARROW_CARD_CONTAINER_WIDTH)) {
                    GroupPocketBalanceCard(
                        balance = GroupPocketBalanceUiModel(
                            groupName = "Romania",
                            formattedBalance = "RON120.00",
                            formattedTotalContributed = "RON200.00",
                            formattedTotalSpent = "RON80.00",
                            currency = "RON"
                        ),
                        onAddMoney = { addMoneyClicks++ },
                        onWithdrawCash = { withdrawClicks++ }
                    )
                }
            }
        }

        composeRule.waitForIdle()

        val contributeNode = composeRule.onNodeWithText(contributeText)
        val withdrawNode = composeRule.onNodeWithText(withdrawText)
        contributeNode.assertIsDisplayed()
        withdrawNode.assertIsDisplayed()

        val contributeBounds = contributeNode.getUnclippedBoundsInRoot()
        val withdrawBounds = withdrawNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Expected stacked layout with withdraw.top (${withdrawBounds.top}) > " +
                "contribute.bottom (${contributeBounds.bottom})",
            withdrawBounds.top > contributeBounds.bottom
        )

        contributeNode.performClick()
        withdrawNode.performClick()
        composeRule.waitForIdle()

        assertEquals(1, addMoneyClicks)
        assertEquals(1, withdrawClicks)
    }

    @Test
    fun rendersGroupPocketBalanceCard_archived_displaysArchivedTagAndReadOnlyNoticeAndHidesActions() {
        val archivedLabel = context.getString(DesignSystemR.string.group_detail_archived_label)
        val readOnlyNotice = context.getString(R.string.balances_archived_readonly_notice)
        val contributeText = context.getString(R.string.balances_add_money)
        val withdrawText = context.getString(R.string.balances_withdraw_cash)

        composeRule.setContent {
            SplitTripTheme {
                GroupPocketBalanceCard(
                    balance = GroupPocketBalanceUiModel(
                        groupName = "Romania",
                        formattedBalance = "RON120.00",
                        formattedTotalContributed = "RON200.00",
                        formattedTotalSpent = "RON80.00",
                        currency = "RON"
                    ),
                    isGroupArchived = true
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(archivedLabel).assertIsDisplayed()
        composeRule.onNodeWithText(readOnlyNotice).assertIsDisplayed()
        composeRule.onNodeWithText(contributeText).assertDoesNotExist()
        composeRule.onNodeWithText(withdrawText).assertDoesNotExist()
    }

    @Test
    fun rendersBalancesScreen_archivedWithData_displaysArchivedIndicatorInCard() {
        val archivedLabel = context.getString(DesignSystemR.string.group_detail_archived_label)
        val readOnlyNotice = context.getString(R.string.balances_archived_readonly_notice)
        val contributeText = context.getString(R.string.balances_add_money)
        val withdrawText = context.getString(R.string.balances_withdraw_cash)

        composeRule.setContent {
            SplitTripTheme {
                BalancesScreen(
                    uiState = BalancesUiState(
                        isLoading = false,
                        groupId = "group-1",
                        isGroupArchived = true,
                        pocketBalance = GroupPocketBalanceUiModel(
                            groupName = "Romania",
                            formattedBalance = "RON120.00",
                            formattedTotalContributed = "RON200.00",
                            formattedTotalSpent = "RON80.00",
                            currency = "RON"
                        )
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(archivedLabel).assertIsDisplayed()
        composeRule.onNodeWithText(readOnlyNotice).assertIsDisplayed()
        composeRule.onNodeWithText(contributeText).assertDoesNotExist()
        composeRule.onNodeWithText(withdrawText).assertDoesNotExist()
    }
}
