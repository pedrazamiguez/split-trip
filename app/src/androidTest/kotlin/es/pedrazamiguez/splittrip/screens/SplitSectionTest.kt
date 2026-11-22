package es.pedrazamiguez.splittrip.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnySibling
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.split.SplitSection
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI and transition state tests for [SplitSection] subunit mode switching.
 */
@RunWith(AndroidJUnit4::class)
class SplitSectionTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val euro = CurrencyUiModel("EUR", "EUR - Euro", 2)

    private val splitTypes = persistentListOf(
        SplitTypeUiModel("EQUAL", "Equal"),
        SplitTypeUiModel("EXACT", "Exact"),
        SplitTypeUiModel("PERCENT", "Percent")
    )

    private val flatSplits = persistentListOf(
        SplitUiModel(
            userId = "user-1",
            displayName = "Alice",
            amountCents = 5000L,
            formattedAmount = "50.00 €"
        ),
        SplitUiModel(
            userId = "user-2",
            displayName = "Bob",
            amountCents = 5000L,
            formattedAmount = "50.00 €"
        )
    )

    private val entitySplits = persistentListOf(
        SplitUiModel(
            userId = "subunit-1",
            displayName = "The Smiths",
            amountCents = 10000L,
            formattedAmount = "100.00 €",
            isEntityRow = true,
            entitySplitType = splitTypes.first(),
            entityMembers = flatSplits
        )
    )

    @Test
    fun splitSection_togglingSubunitSwitch_transitionsBetweenFlatAndEntityEditors() {
        val toggleLabel = context.getString(R.string.add_expense_split_subunit_mode)
        val subunitSwitchMatcher = isToggleable() and hasAnySibling(hasText(toggleLabel))

        composeRule.setContent {
            SplitTripTheme {
                var isSubunitMode by remember { mutableStateOf(false) }
                SplitSection(
                    uiState = AddExpenseUiState(
                        sourceAmount = "100.00",
                        selectedCurrency = euro,
                        groupCurrency = euro,
                        availableSplitTypes = splitTypes,
                        selectedSplitType = splitTypes.first(),
                        hasSubunits = true,
                        isSubunitMode = isSubunitMode,
                        splits = flatSplits,
                        entitySplits = entitySplits
                    ),
                    onEvent = { event ->
                        if (event is AddExpenseUiEvent.SubunitModeToggled) {
                            isSubunitMode = !isSubunitMode
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNode(subunitSwitchMatcher).assertIsOff()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("The Smiths").assertDoesNotExist()

        composeRule.onNode(subunitSwitchMatcher).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(subunitSwitchMatcher).assertIsOn()
        composeRule.onNodeWithText("The Smiths").assertIsDisplayed()
        composeRule.onNodeWithText("Alice").assertDoesNotExist()

        composeRule.onNode(subunitSwitchMatcher).performClick()
        composeRule.waitForIdle()

        composeRule.onNode(subunitSwitchMatcher).assertIsOff()
        composeRule.onNodeWithText("Alice").assertIsDisplayed()
        composeRule.onNodeWithText("The Smiths").assertDoesNotExist()
    }

    @Test
    fun splitSection_splitError_appearsAndClearsSmoothlyOnToggle() {
        val toggleLabel = context.getString(R.string.add_expense_split_subunit_mode)
        val subunitSwitchMatcher = isToggleable() and hasAnySibling(hasText(toggleLabel))
        val errorMessage = "Split amounts must match total"

        composeRule.setContent {
            SplitTripTheme {
                var isSubunitMode by remember { mutableStateOf(false) }
                var splitError by remember { mutableStateOf<UiText?>(UiText.DynamicString(errorMessage)) }
                SplitSection(
                    uiState = AddExpenseUiState(
                        sourceAmount = "100.00",
                        selectedCurrency = euro,
                        groupCurrency = euro,
                        availableSplitTypes = splitTypes,
                        selectedSplitType = splitTypes.first(),
                        hasSubunits = true,
                        isSubunitMode = isSubunitMode,
                        splits = flatSplits,
                        entitySplits = entitySplits,
                        splitError = splitError
                    ),
                    onEvent = { event ->
                        if (event is AddExpenseUiEvent.SubunitModeToggled) {
                            isSubunitMode = !isSubunitMode
                            splitError = null
                        }
                    }
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()

        composeRule.onNode(subunitSwitchMatcher).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(errorMessage).assertDoesNotExist()
        composeRule.onNodeWithText("The Smiths").assertIsDisplayed()
    }
}
