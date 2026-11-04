package es.pedrazamiguez.splittrip.screens

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import es.pedrazamiguez.splittrip.core.designsystem.foundation.SplitTripTheme
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.ArithmeticKeyboardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.ArithmeticTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.LocalArithmeticKeyboardState
import es.pedrazamiguez.splittrip.domain.service.calculator.impl.ExpressionCalculatorServiceImpl
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [ArithmeticTextField].
 */
@RunWith(AndroidJUnit4::class)
class ArithmeticTextFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val evaluator = ExpressionCalculatorServiceImpl()

    @Test
    fun commitResult_emitsRawPlainString_withoutGroupingSeparators_whenArithmeticEvaluated() {
        var committedValue = ""
        var textValue by mutableStateOf("")

        composeRule.setContent {
            SplitTripTheme {
                ArithmeticTextField(
                    value = textValue,
                    onValueChange = {
                        committedValue = it
                        textValue = it
                    },
                    evaluator = evaluator,
                    modifier = Modifier.testTag("arithmetic_field")
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.onNodeWithTag("arithmetic_field").performTextInput("10000+450")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performImeAction()
        composeRule.waitForIdle()

        assertEquals("10450", committedValue)
    }

    @Test
    fun rendersResolvedDisplayValue_whenNotFocused() {
        composeRule.setContent {
            val config = LocalConfiguration.current
            config.setLocale(Locale.US)
            CompositionLocalProvider(LocalConfiguration provides config) {
                SplitTripTheme {
                    ArithmeticTextField(
                        value = "10450",
                        onValueChange = {},
                        evaluator = evaluator
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("10,450").assertIsDisplayed()
    }

    @Test
    fun rendersCustomDisplayValue_whenProvidedAndNotFocused() {
        composeRule.setContent {
            SplitTripTheme {
                ArithmeticTextField(
                    value = "10450",
                    displayValue = "Custom 10.450 €",
                    onValueChange = {},
                    evaluator = evaluator
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithText("Custom 10.450 €").assertIsDisplayed()
    }

    @Test
    fun rendersRawExpressionBuffer_whenFocused() {
        var textValue by mutableStateOf("10450")

        composeRule.setContent {
            SplitTripTheme {
                ArithmeticTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    evaluator = evaluator,
                    modifier = Modifier.testTag("arithmetic_field")
                )
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("10450").assertIsDisplayed()
    }

    @Test
    fun operatorInsertion_placesCursorAfterOperator_andSubsequentDigitsAppendAfter() {
        var committedValue = ""
        var textValue by mutableStateOf("16.15")
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())

        composeRule.setContent {
            CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                SplitTripTheme {
                    ArithmeticTextField(
                        value = textValue,
                        onValueChange = {
                            committedValue = it
                            textValue = it
                        },
                        evaluator = evaluator,
                        modifier = Modifier.testTag("arithmetic_field")
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.waitForIdle()

        keyboardState.value.onOperatorClick("×")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performTextInput("2")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performImeAction()
        composeRule.waitForIdle()

        assertEquals("32.3", committedValue)
    }

    @Test
    fun operatorInsertionInMiddle_insertsAtSelectionAndAdvancesCursor() {
        var textValue by mutableStateOf("1615")
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())

        composeRule.setContent {
            CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                SplitTripTheme {
                    ArithmeticTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        evaluator = evaluator,
                        modifier = Modifier.testTag("arithmetic_field")
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performTextInputSelection(TextRange(2))
        composeRule.waitForIdle()

        keyboardState.value.onOperatorClick("+")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performTextInput("4")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("16+415").assertIsDisplayed()
    }

    @Test
    fun focusingPrepopulatedField_placesCursorAtEnd() {
        var textValue by mutableStateOf("100")
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())

        composeRule.setContent {
            CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                SplitTripTheme {
                    ArithmeticTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        evaluator = evaluator,
                        modifier = Modifier.testTag("arithmetic_field")
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.waitForIdle()

        keyboardState.value.onOperatorClick("+")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performTextInput("25")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("100+25").assertIsDisplayed()
    }

    @Test
    fun clearingExpression_resetsCursorToZero() {
        var textValue by mutableStateOf("999")
        val keyboardState = mutableStateOf(ArithmeticKeyboardState())

        composeRule.setContent {
            CompositionLocalProvider(LocalArithmeticKeyboardState provides keyboardState) {
                SplitTripTheme {
                    ArithmeticTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        evaluator = evaluator,
                        modifier = Modifier.testTag("arithmetic_field")
                    )
                }
            }
        }

        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performClick()
        composeRule.waitForIdle()

        keyboardState.value.onClear()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("arithmetic_field").performTextInput("42")
        composeRule.waitForIdle()

        composeRule.onNodeWithText("42").assertIsDisplayed()
    }
}
