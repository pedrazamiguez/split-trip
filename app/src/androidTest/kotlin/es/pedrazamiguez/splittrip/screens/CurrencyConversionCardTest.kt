package es.pedrazamiguez.splittrip.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.ArithmeticKeyboardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.LocalArithmeticKeyboardState
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionCalculatorService
import es.pedrazamiguez.splittrip.domain.service.calculator.impl.ExpressionCalculatorServiceImpl
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinApplication
import org.koin.dsl.module

/**
 * UI tests for [CurrencyConversionCard].
 */
@RunWith(AndroidJUnit4::class)
class CurrencyConversionCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testModule = module {
        single<ExpressionCalculatorService> { ExpressionCalculatorServiceImpl() }
    }

    @Test
    fun focusingGroupAmountField_placesCursorAtEnd_andOperatorAppendsAtEnd() {
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())
        var groupAmount by mutableStateOf("16.15")

        composeRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                    SplitTripTheme {
                        CurrencyConversionCard(
                            state = CurrencyConversionCardState(
                                title = "Currency Conversion",
                                exchangeRateValue = "38.50",
                                exchangeRateLabel = "1 EUR = X THB",
                                groupAmountValue = groupAmount,
                                groupAmountLabel = "Cost in EUR",
                                isLoadingRate = false,
                                isExchangeRateLocked = false
                            ),
                            onExchangeRateChanged = {},
                            onGroupAmountChanged = { groupAmount = it }
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("16.15").performClick()
        composeRule.waitForIdle()

        keyboardState.value.onOperatorClick("×")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("16.15×").performTextInput("2")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("16.15×2").assertIsDisplayed()
    }

    @Test
    fun focusingExchangeRateField_placesCursorAtEnd() {
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())
        var rateValue by mutableStateOf("38.50")

        composeRule.setContent {
            KoinApplication(application = { modules(testModule) }) {
                CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                    SplitTripTheme {
                        CurrencyConversionCard(
                            state = CurrencyConversionCardState(
                                title = "Currency Conversion",
                                exchangeRateValue = rateValue,
                                exchangeRateLabel = "1 EUR = X THB",
                                groupAmountValue = "100",
                                groupAmountLabel = "Cost in EUR",
                                isLoadingRate = false,
                                isExchangeRateLocked = false
                            ),
                            onExchangeRateChanged = { rateValue = it },
                            onGroupAmountChanged = {}
                        )
                    }
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("38.50").performClick()
        composeRule.waitForIdle()

        keyboardState.value.onOperatorClick("+")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("38.50+").performTextInput("1")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("38.50+1").assertIsDisplayed()
    }
}
