package es.pedrazamiguez.splittrip.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CURRENCY_PICKER_EMPTY_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CURRENCY_PICKER_ITEM_TAG_PREFIX
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CURRENCY_PICKER_SEARCH_TAG
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyPickerBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [CurrencyPickerBottomSheet].
 */
@RunWith(AndroidJUnit4::class)
class CurrencyPickerBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val sampleCurrencies = listOf(
        CurrencyUiModel(
            code = "EUR",
            displayText = "EUR (€)",
            decimalDigits = 2,
            defaultName = "Euro",
            localizedName = "Euro"
        ),
        CurrencyUiModel(
            code = "USD",
            displayText = "USD ($)",
            decimalDigits = 2,
            defaultName = "US Dollar",
            localizedName = "US Dollar"
        ),
        CurrencyUiModel(
            code = "GBP",
            displayText = "GBP (£)",
            decimalDigits = 2,
            defaultName = "British Pound",
            localizedName = "British Pound"
        )
    ).toImmutableList()

    private val tenCurrencies = listOf(
        CurrencyUiModel("EUR", "EUR (€)", 2, "Euro", "Euro"),
        CurrencyUiModel("USD", "USD ($)", 2, "US Dollar", "US Dollar"),
        CurrencyUiModel("GBP", "GBP (£)", 2, "British Pound", "British Pound"),
        CurrencyUiModel("JPY", "JPY (¥)", 0, "Japanese Yen", "Japanese Yen"),
        CurrencyUiModel("CAD", "CAD ($)", 2, "Canadian Dollar", "Canadian Dollar"),
        CurrencyUiModel("AUD", "AUD ($)", 2, "Australian Dollar", "Australian Dollar"),
        CurrencyUiModel("CHF", "CHF", 2, "Swiss Franc", "Swiss Franc"),
        CurrencyUiModel("CNY", "CNY (¥)", 2, "Chinese Yuan", "Chinese Yuan"),
        CurrencyUiModel("SEK", "SEK (kr)", 2, "Swedish Krona", "Swedish Krona"),
        CurrencyUiModel("NZD", "NZD ($)", 2, "New Zealand Dollar", "New Zealand Dollar")
    ).toImmutableList()

    @Test
    fun rendersFormattedCurrencyItems_andHighlightsSelected() {
        val selectedDescription = targetContext.getString(DesignSystemR.string.content_description_selected)

        composeRule.setContent {
            SplitTripTheme {
                CurrencyPickerBottomSheet(
                    selectedCurrency = sampleCurrencies.first(),
                    availableCurrencies = sampleCurrencies,
                    onCurrencySelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Euro").assertIsDisplayed()
        composeRule.onNodeWithText("EUR (€)").assertIsDisplayed()
        composeRule.onNodeWithText("US Dollar").assertIsDisplayed()
        composeRule.onNodeWithText("USD ($)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(selectedDescription).assertIsDisplayed()
    }

    @Test
    fun tappingCurrencyItem_invokesCallbackWithCurrencyCode_andDismisses() {
        var selectedCode: String? = null
        var dismissed = false

        composeRule.setContent {
            SplitTripTheme {
                CurrencyPickerBottomSheet(
                    selectedCurrency = sampleCurrencies.first(),
                    availableCurrencies = sampleCurrencies,
                    onCurrencySelected = { selectedCode = it },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CURRENCY_PICKER_ITEM_TAG_PREFIX + "USD").performClick()
        composeRule.waitForIdle()

        assertEquals("USD", selectedCode)
        assertTrue(dismissed)
    }

    @Test
    fun whenCurrenciesLessThanOrEqualToSix_searchBarIsNotDisplayed() {
        val fiveCurrencies = sampleCurrencies.take(2).toImmutableList()

        composeRule.setContent {
            SplitTripTheme {
                CurrencyPickerBottomSheet(
                    selectedCurrency = null,
                    availableCurrencies = fiveCurrencies,
                    onCurrencySelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CURRENCY_PICKER_SEARCH_TAG).assertDoesNotExist()
    }

    @Test
    fun whenCurrenciesGreaterThanSix_searchBarIsDisplayed_andFiltersCorrectly() {
        composeRule.setContent {
            SplitTripTheme {
                CurrencyPickerBottomSheet(
                    selectedCurrency = null,
                    availableCurrencies = tenCurrencies,
                    onCurrencySelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CURRENCY_PICKER_SEARCH_TAG).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(CURRENCY_PICKER_SEARCH_TAG)))
            .performTextInput("yen")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Japanese Yen").assertIsDisplayed()
        composeRule.onNodeWithText("JPY (¥)").assertIsDisplayed()
        composeRule.onNodeWithTag(CURRENCY_PICKER_ITEM_TAG_PREFIX + "EUR").assertDoesNotExist()
    }

    @Test
    fun whenSearchMatchesNothing_emptyStateIsDisplayed() {
        composeRule.setContent {
            SplitTripTheme {
                CurrencyPickerBottomSheet(
                    selectedCurrency = null,
                    availableCurrencies = tenCurrencies,
                    onCurrencySelected = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag(CURRENCY_PICKER_SEARCH_TAG)))
            .performTextInput("ZZZZ")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(CURRENCY_PICKER_EMPTY_TAG).assertIsDisplayed()
    }
}
