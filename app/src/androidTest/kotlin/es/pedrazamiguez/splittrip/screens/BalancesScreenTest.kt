package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.BalancesScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.screen.CategorySpendingScreen
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.BalancesUiState
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.CategorySpendingUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests for empty states on [BalancesScreen] and [CategorySpendingScreen].
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
    fun rendersCategorySpendingScreen_emptyState_displaysCategorySpendingEmptyCopy() {
        val emptyTitle = context.getString(R.string.balances_category_spending_empty_title)
        val emptyDescription = context.getString(R.string.balances_category_spending_empty_description)

        composeRule.setContent {
            SplitTripTheme {
                CategorySpendingScreen(
                    uiState = CategorySpendingUiState(
                        isLoading = false,
                        items = persistentListOf()
                    )
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(emptyTitle).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(emptyDescription).performScrollTo().assertIsDisplayed()
    }
}
