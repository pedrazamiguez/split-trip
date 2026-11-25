package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencySelectorField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI semantics tests for [CurrencySelectorField] and [StyledOutlinedTextField]
 * read-only clickable overlays.
 */
@RunWith(AndroidJUnit4::class)
class CurrencySelectorFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val eurCurrency = CurrencyUiModel(
        code = "EUR",
        displayText = "EUR (€)",
        decimalDigits = 2,
        defaultName = "Euro",
        localizedName = "Euro",
        symbol = "€"
    )

    private val usdCurrency = CurrencyUiModel(
        code = "USD",
        displayText = "USD ($)",
        decimalDigits = 2,
        defaultName = "US Dollar",
        localizedName = "US Dollar",
        symbol = "$"
    )

    @Test
    fun currencySelector_exposesLabelAndSelectedValue_withDropdownRoleAndOpensSheetOnClick() {
        composeRule.setContent {
            SplitTripTheme {
                CurrencySelectorField(
                    selectedCurrency = eurCurrency,
                    availableCurrencies = persistentListOf(eurCurrency, usdCurrency),
                    onCurrencySelected = {},
                    label = "Currency"
                )
            }
        }

        composeRule.waitForIdle()

        val selectorNode = composeRule.onNodeWithContentDescription("Currency, EUR (€)")
        selectorNode.assertIsDisplayed()
        selectorNode.assertIsEnabled()
        selectorNode.assertHasClickAction()
        selectorNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))

        selectorNode.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("USD ($)").assertIsDisplayed()
    }

    @Test
    fun currencySelector_whenNoCurrencySelected_exposesLabelOnlyWithoutDuplicateNodes() {
        composeRule.setContent {
            SplitTripTheme {
                CurrencySelectorField(
                    selectedCurrency = null,
                    availableCurrencies = persistentListOf(eurCurrency, usdCurrency),
                    onCurrencySelected = {},
                    label = "Currency"
                )
            }
        }

        composeRule.waitForIdle()

        val selectorNode = composeRule.onNodeWithContentDescription("Currency")
        selectorNode.assertIsDisplayed()
        selectorNode.assertHasClickAction()
        selectorNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
    }

    @Test
    fun currencySelector_whenDisabled_exposesDisabledSemanticsAndIgnoresClick() {
        composeRule.setContent {
            SplitTripTheme {
                CurrencySelectorField(
                    selectedCurrency = eurCurrency,
                    availableCurrencies = persistentListOf(eurCurrency, usdCurrency),
                    onCurrencySelected = {},
                    label = "Currency",
                    enabled = false
                )
            }
        }

        composeRule.waitForIdle()

        val selectorNode = composeRule.onNodeWithContentDescription("Currency, EUR (€)")
        selectorNode.assertIsDisplayed()
        selectorNode.assertIsNotEnabled()
        selectorNode.performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("USD ($)").assertDoesNotExist()
    }

    @Test
    fun styledOutlinedTextField_readOnlyWithPlaceholder_exposesLabelAndPlaceholderAndInvokesClick() {
        var clicked = false

        composeRule.setContent {
            SplitTripTheme {
                StyledOutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = "Select date",
                    label = "Due date",
                    readOnly = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.waitForIdle()

        val fieldNode = composeRule.onNodeWithContentDescription("Due date, Select date")
        fieldNode.assertIsDisplayed()
        fieldNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
        fieldNode.performClick()

        assertTrue(clicked)
    }

    @Test
    fun styledOutlinedTextField_textFieldValueOverload_readOnly_exposesLabelAndValue() {
        var clicked = false

        composeRule.setContent {
            SplitTripTheme {
                StyledOutlinedTextField(
                    value = TextFieldValue("USD ($)"),
                    onValueChange = {},
                    label = "Currency",
                    readOnly = true,
                    onClick = { clicked = true }
                )
            }
        }

        composeRule.waitForIdle()

        val fieldNode = composeRule.onNodeWithContentDescription("Currency, USD ($)")
        fieldNode.assertIsDisplayed()
        fieldNode.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList))
        fieldNode.performClick()

        assertTrue(clicked)
    }
}
