package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseValidationServiceTest {

    private lateinit var service: ExpenseValidationService

    @BeforeEach
    fun setUp() {
        val calculatorService = ExpenseCalculatorService()
        val splitCalculatorFactory = ExpenseSplitCalculatorFactory(calculatorService)
        service = ExpenseValidationService(splitCalculatorFactory)
    }

    @Nested
    inner class ValidateTitle {

        @Test
        fun `valid title returns Valid`() {
            val result = service.validateTitle("Dinner")
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `empty title returns Invalid`() {
            val result = service.validateTitle("")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("Title cannot be empty", (result as ValidationResult.Invalid).message)
        }

        @Test
        fun `blank title returns Invalid`() {
            val result = service.validateTitle("   ")
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("Title cannot be empty", (result as ValidationResult.Invalid).message)
        }

        @Test
        fun `title with only whitespace returns Invalid`() {
            val result = service.validateTitle("\t\n  ")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `title with content returns Valid`() {
            val result = service.validateTitle("Hotel booking")
            assertEquals(ValidationResult.Valid, result)
        }
    }

    @Nested
    inner class ValidateAmount {

        @Test
        fun `valid amount returns Valid`() {
            val result = service.validateAmount("10.50")
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `valid integer amount returns Valid`() {
            val result = service.validateAmount("100")
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `empty amount returns Invalid`() {
            val result = service.validateAmount("")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `blank amount returns Invalid`() {
            val result = service.validateAmount("   ")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `zero amount returns Invalid`() {
            val result = service.validateAmount("0")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `negative amount returns Invalid`() {
            val result = service.validateAmount("-5.00")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `invalid characters returns Invalid`() {
            val result = service.validateAmount("abc")
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `comma decimal format returns Valid`() {
            val result = service.validateAmount("12,50")
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `amount with thousand separators returns Valid`() {
            val result = service.validateAmount("1,250.00")
            assertEquals(ValidationResult.Valid, result)
        }
    }

    @Nested
    inner class ValidateUserCount {

        @Test
        fun `positive count returns Valid`() {
            val result = service.validateUserCount(3)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `count of one returns Valid`() {
            val result = service.validateUserCount(1)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `zero count returns Invalid`() {
            val result = service.validateUserCount(0)
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("User count must be greater than zero", (result as ValidationResult.Invalid).message)
        }

        @Test
        fun `negative count returns Invalid`() {
            val result = service.validateUserCount(-1)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    inner class ValidateSplits {

        @Test
        fun `valid EQUAL split returns Valid`() {
            val result = service.validateSplits(
                splitType = SplitType.EQUAL,
                splits = emptyList(),
                totalAmountCents = 1000L,
                participantIds = listOf("user1", "user2")
            )
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `EQUAL split with empty participants returns Invalid`() {
            val result = service.validateSplits(
                splitType = SplitType.EQUAL,
                splits = emptyList(),
                totalAmountCents = 1000L,
                participantIds = emptyList()
            )
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `valid EXACT split returns Valid`() {
            val splits = listOf(
                ExpenseSplit(userId = "user1", amountCents = 600L),
                ExpenseSplit(userId = "user2", amountCents = 400L)
            )
            val result = service.validateSplits(
                splitType = SplitType.EXACT,
                splits = splits,
                totalAmountCents = 1000L,
                participantIds = listOf("user1", "user2")
            )
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `EXACT split with wrong sum returns Invalid`() {
            val splits = listOf(
                ExpenseSplit(userId = "user1", amountCents = 600L),
                ExpenseSplit(userId = "user2", amountCents = 300L)
            )
            val result = service.validateSplits(
                splitType = SplitType.EXACT,
                splits = splits,
                totalAmountCents = 1000L,
                participantIds = listOf("user1", "user2")
            )
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `valid PERCENT split returns Valid`() {
            val splits = listOf(
                ExpenseSplit(userId = "user1", amountCents = 0, percentage = BigDecimal("60")),
                ExpenseSplit(userId = "user2", amountCents = 0, percentage = BigDecimal("40"))
            )
            val result = service.validateSplits(
                splitType = SplitType.PERCENT,
                splits = splits,
                totalAmountCents = 1000L,
                participantIds = listOf("user1", "user2")
            )
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `PERCENT split with wrong sum returns Invalid`() {
            val splits = listOf(
                ExpenseSplit(userId = "user1", amountCents = 0, percentage = BigDecimal("60")),
                ExpenseSplit(userId = "user2", amountCents = 0, percentage = BigDecimal("30"))
            )
            val result = service.validateSplits(
                splitType = SplitType.PERCENT,
                splits = splits,
                totalAmountCents = 1000L,
                participantIds = listOf("user1", "user2")
            )
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `PERCENT split with empty participants returns Invalid`() {
            val result = service.validateSplits(
                splitType = SplitType.PERCENT,
                splits = emptyList(),
                totalAmountCents = 1000L,
                participantIds = emptyList()
            )
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    inner class ValidateAddOn {

        private val validAddOn = AddOn(
            id = "addon-1",
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 250,
            currency = "EUR",
            groupAmountCents = 250
        )

        @Test
        fun `valid add-on returns Valid`() {
            val result = service.validateAddOn(validAddOn, sourceAmountCents = 10000)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `zero amount returns Invalid`() {
            val addOn = validAddOn.copy(amountCents = 0)
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `negative amount returns Invalid`() {
            val addOn = validAddOn.copy(amountCents = -100)
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `blank currency returns Invalid`() {
            val addOn = validAddOn.copy(currency = "")
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `INCLUDED mode with amount equal to source returns Invalid`() {
            val addOn = validAddOn.copy(
                mode = AddOnMode.INCLUDED,
                amountCents = 10000
            )
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `INCLUDED mode with amount exceeding source returns Invalid`() {
            val addOn = validAddOn.copy(
                mode = AddOnMode.INCLUDED,
                amountCents = 15000
            )
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }

        @Test
        fun `INCLUDED mode with amount less than source returns Valid`() {
            val addOn = validAddOn.copy(
                mode = AddOnMode.INCLUDED,
                amountCents = 5000
            )
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `ON_TOP mode allows amount equal to source`() {
            val addOn = validAddOn.copy(
                mode = AddOnMode.ON_TOP,
                amountCents = 10000
            )
            val result = service.validateAddOn(addOn, sourceAmountCents = 10000)
            assertEquals(ValidationResult.Valid, result)
        }
    }

    @Nested
    inner class ValidateAddOns {

        @Test
        fun `empty list returns Valid`() {
            val result = service.validateAddOns(emptyList(), sourceAmountCents = 10000)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `all valid add-ons returns Valid`() {
            val addOns = listOf(
                AddOn(
                    id = "a1",
                    amountCents = 250,
                    currency = "EUR",
                    groupAmountCents = 250
                ),
                AddOn(
                    id = "a2",
                    type = AddOnType.TIP,
                    amountCents = 500,
                    currency = "EUR",
                    groupAmountCents = 500
                )
            )
            val result = service.validateAddOns(addOns, sourceAmountCents = 10000)
            assertEquals(ValidationResult.Valid, result)
        }

        @Test
        fun `returns first invalid result`() {
            val addOns = listOf(
                AddOn(
                    id = "a1",
                    amountCents = 250,
                    currency = "EUR",
                    groupAmountCents = 250
                ),
                AddOn(
                    id = "a2",
                    amountCents = 0,
                    currency = "EUR",
                    groupAmountCents = 0
                )
            )
            val result = service.validateAddOns(addOns, sourceAmountCents = 10000)
            assertTrue(result is ValidationResult.Invalid)
        }
    }

    @Nested
    inner class ValidateExpenseDate {

        @Test
        fun `current or past date returns Valid`() {
            val now = System.currentTimeMillis()
            val past = now - 100000L
            assertEquals(ValidationResult.Valid, service.validateExpenseDate(now))
            assertEquals(ValidationResult.Valid, service.validateExpenseDate(past))
        }

        @Test
        fun `future date returns Invalid`() {
            val future = System.currentTimeMillis() + 100000L
            val result = service.validateExpenseDate(future)
            assertTrue(result is ValidationResult.Invalid)
            assertEquals("Expense date and time cannot be in the future", (result as ValidationResult.Invalid).message)
        }
    }
}
