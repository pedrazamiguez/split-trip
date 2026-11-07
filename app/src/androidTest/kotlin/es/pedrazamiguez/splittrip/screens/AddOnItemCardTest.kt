package es.pedrazamiguez.splittrip.screens

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon.AddOnItemCard
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.helpers.ScreenshotRule
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI smoke and interaction tests for [AddOnItemCard].
 */
@RunWith(AndroidJUnit4::class)
class AddOnItemCardTest {

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @get:Rule(order = 2)
    val screenshotRule = ScreenshotRule()

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val testCurrency = CurrencyUiModel(
        code = "EUR",
        displayText = "EUR (€)",
        decimalDigits = 2,
        defaultName = "Euro",
        localizedName = "Euro"
    )

    private val testPaymentMethod = PaymentMethodUiModel(
        id = "CREDIT_CARD",
        displayText = "Credit Card"
    )

    @Test
    fun rendersCollapsedAddOnItemCard_displaysSummaryAndChevronDown() {
        var toggleClicked = false
        val expandCd = context.getString(R.string.add_expense_add_on_item_expand)
        val removeCd = context.getString(R.string.add_expense_add_on_remove)
        val tipTypeLabel = context.getString(R.string.add_expense_add_on_type_tip)
        val onTopModeLabel = context.getString(R.string.add_expense_add_on_mode_on_top)

        val collapsedAddOn = AddOnUiModel(
            id = "addon-1",
            type = AddOnType.TIP,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountInput = "5.00",
            formattedAmount = "5.00 €",
            currency = testCurrency,
            paymentMethod = testPaymentMethod,
            isExpanded = false
        )

        composeRule.setContent {
            SplitTripTheme {
                AddOnItemCard(
                    addOn = collapsedAddOn,
                    availableCurrencies = persistentListOf(testCurrency),
                    paymentMethods = persistentListOf(testPaymentMethod),
                    showCurrencySelector = false,
                    onEvent = {},
                    onRemove = {},
                    onToggleExpand = { toggleClicked = true },
                    groupCurrency = testCurrency
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText(tipTypeLabel).assertIsDisplayed()
        composeRule.onNodeWithText(onTopModeLabel).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(expandCd).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(removeCd).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(expandCd).performClick()
        composeRule.waitForIdle()
        assertTrue(toggleClicked)
    }

    @Test
    fun rendersExpandedAddOnItemCard_displaysEditorFieldsAndChevronUp() {
        var collapseClicked = false
        val collapseCd = context.getString(R.string.add_expense_add_on_item_collapse)
        val removeCd = context.getString(R.string.add_expense_add_on_remove)
        val typeTitle = context.getString(R.string.add_expense_add_on_type_title)
        val modeTitle = context.getString(R.string.add_expense_add_on_mode_title)
        val valueTypeTitle = context.getString(R.string.add_expense_add_on_value_type_title)
        val amountHint = context.getString(R.string.add_expense_add_on_amount_hint)
        val descriptionHint = context.getString(R.string.add_expense_add_on_description_hint)

        val expandedAddOn = AddOnUiModel(
            id = "addon-1",
            type = AddOnType.TIP,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountInput = "5.00",
            formattedAmount = "5.00 €",
            currency = testCurrency,
            paymentMethod = testPaymentMethod,
            isExpanded = true
        )

        composeRule.setContent {
            SplitTripTheme {
                AddOnItemCard(
                    addOn = expandedAddOn,
                    availableCurrencies = persistentListOf(testCurrency),
                    paymentMethods = persistentListOf(testPaymentMethod),
                    showCurrencySelector = false,
                    onEvent = {},
                    onRemove = {},
                    onToggleExpand = { collapseClicked = true },
                    groupCurrency = testCurrency
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(collapseCd).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(removeCd).assertIsDisplayed()
        composeRule.onNodeWithText(typeTitle).assertIsDisplayed()
        composeRule.onNodeWithText(modeTitle).assertIsDisplayed()
        composeRule.onNodeWithText(valueTypeTitle).assertIsDisplayed()
        composeRule.onNodeWithText(amountHint).assertIsDisplayed()
        composeRule.onNodeWithText(descriptionHint).assertIsDisplayed()

        composeRule.onNodeWithContentDescription(collapseCd).performClick()
        composeRule.waitForIdle()
        assertTrue(collapseClicked)
    }
}
