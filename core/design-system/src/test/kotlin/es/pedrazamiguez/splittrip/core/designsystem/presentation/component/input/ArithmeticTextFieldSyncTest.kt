package es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input

import es.pedrazamiguez.splittrip.domain.service.calculator.impl.ExpressionCalculatorServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArithmeticTextFieldSyncTest {

    private val evaluator = ExpressionCalculatorServiceImpl()

    @Test
    fun resolveFocusedSyncValue_dotDecimal_returnsCanonicalPlainString() {
        val result = resolveFocusedSyncValue(evaluator.evaluate("12.34"), maxDecimalPlaces = 2)

        assertEquals("12.34", result)
    }

    @Test
    fun resolveFocusedSyncValue_commaDecimal_returnsNormalizedDotString() {
        val result = resolveFocusedSyncValue(evaluator.evaluate("12,34"), maxDecimalPlaces = 2)

        assertEquals("12.34", result)
    }

    @Test
    fun resolveFocusedSyncValue_trailingZeros_stripsTrailingZeros() {
        assertEquals("12.3", resolveFocusedSyncValue(evaluator.evaluate("12.30"), maxDecimalPlaces = 2))
        assertEquals("12", resolveFocusedSyncValue(evaluator.evaluate("12.00"), maxDecimalPlaces = 2))
        assertEquals("0", resolveFocusedSyncValue(evaluator.evaluate("0.00"), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_exceedingDecimalPlaces_roundsHalfUp() {
        assertEquals("12.35", resolveFocusedSyncValue(evaluator.evaluate("12.345"), maxDecimalPlaces = 2))
        assertEquals("13", resolveFocusedSyncValue(evaluator.evaluate("12.6"), maxDecimalPlaces = 0))
    }

    @Test
    fun resolveFocusedSyncValue_arithmeticExpression_returnsEvaluatedResult() {
        assertEquals("12", resolveFocusedSyncValue(evaluator.evaluate("10+2"), maxDecimalPlaces = 2))
        assertEquals("32.3", resolveFocusedSyncValue(evaluator.evaluate("16.15×2"), maxDecimalPlaces = 2))
        assertEquals("12", resolveFocusedSyncValue(evaluator.evaluate("10,5+1,5"), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_trailingDecimalSeparator_returnsIntegerValue() {
        assertEquals("12", resolveFocusedSyncValue(evaluator.evaluate("12."), maxDecimalPlaces = 2))
        assertEquals("12", resolveFocusedSyncValue(evaluator.evaluate("12,"), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_zeroAndNegativeValues_returnsCanonicalString() {
        assertEquals("0", resolveFocusedSyncValue(evaluator.evaluate("0"), maxDecimalPlaces = 2))
        assertEquals("0", resolveFocusedSyncValue(evaluator.evaluate("5-5"), maxDecimalPlaces = 2))
        assertEquals("-5", resolveFocusedSyncValue(evaluator.evaluate("-5"), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_emptyOrBlankExpression_returnsEmptyString() {
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate(""), maxDecimalPlaces = 2))
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate("   "), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_malformedExpression_returnsEmptyString() {
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate("10+"), maxDecimalPlaces = 2))
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate("abc"), maxDecimalPlaces = 2))
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate("."), maxDecimalPlaces = 2))
    }

    @Test
    fun resolveFocusedSyncValue_divisionByZero_returnsEmptyString() {
        assertEquals("", resolveFocusedSyncValue(evaluator.evaluate("10÷0"), maxDecimalPlaces = 2))
    }

    @Test
    fun formatEvaluatedValue_largeRoundNumber_avoidsScientificNotation() {
        val result = formatEvaluatedValue(BigDecimal("10000.00"), maxDecimalPlaces = 2)

        assertEquals("10000", result)
    }
}
