package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.impl.CashWithdrawalValidationServiceImpl
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CashWithdrawalValidationServiceTest {

    private val service = CashWithdrawalValidationServiceImpl()

    @Test
    fun `validateAmountWithdrawn returns Valid for positive amount`() {
        val result = service.validateAmountWithdrawn(10000L)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
    }

    @Test
    fun `validateAmountWithdrawn returns Invalid for zero`() {
        val result = service.validateAmountWithdrawn(0L)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
        val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
        assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE)
    }

    @Test
    fun `validateAmountWithdrawn returns Invalid for negative`() {
        val result = service.validateAmountWithdrawn(-100L)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
    }

    @Test
    fun `validateDeductedBaseAmount returns Valid for positive amount`() {
        val result = service.validateDeductedBaseAmount(27000L)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
    }

    @Test
    fun `validateDeductedBaseAmount returns Invalid for zero`() {
        val result = service.validateDeductedBaseAmount(0L)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
        val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
        assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.DEDUCTED_AMOUNT_MUST_BE_POSITIVE)
    }

    @Test
    fun `validateCurrency returns Valid for non-blank currency`() {
        val result = service.validateCurrency("THB")
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
    }

    @Test
    fun `validateCurrency returns Invalid for blank currency`() {
        val result = service.validateCurrency("")
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
        val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
        assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.CURRENCY_REQUIRED)
    }

    @Test
    fun `validateExchangeRate returns Valid for positive rate`() {
        val result = service.validateExchangeRate(BigDecimal("37.037"))
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
    }

    @Test
    fun `validateExchangeRate returns Invalid for zero rate`() {
        val result = service.validateExchangeRate(BigDecimal.ZERO)
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
        val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
        assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.EXCHANGE_RATE_MUST_BE_POSITIVE)
    }

    @Test
    fun `validateExchangeRate returns Invalid for negative rate`() {
        val result = service.validateExchangeRate(BigDecimal("-1.5"))
        assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
    }

    @Nested
    inner class WithdrawalScopeValidation {

        private val subunit = Subunit(
            id = "subunit-1",
            name = "Couple",
            groupId = "group-1",
            memberIds = listOf("user-1", "user-2")
        )
        private val groupSubunits = listOf(subunit)

        @Test
        fun `GROUP scope with null subunitId returns Valid`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.GROUP,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `GROUP scope with non-null subunitId returns INVALID_SUBUNIT_FOR_SCOPE`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.GROUP,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE)
        }

        @Test
        fun `USER scope with null subunitId returns Valid`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.USER,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `USER scope with non-null subunitId returns INVALID_SUBUNIT_FOR_SCOPE`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.USER,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE)
        }

        @Test
        fun `SUBUNIT scope with valid subunitId and member user returns Valid`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `SUBUNIT scope with null subunitId returns SUBUNIT_REQUIRED`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = null,
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.SUBUNIT_REQUIRED)
        }

        @Test
        fun `SUBUNIT scope with blank subunitId returns SUBUNIT_REQUIRED`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "   ",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.SUBUNIT_REQUIRED)
        }

        @Test
        fun `SUBUNIT scope with unknown subunitId returns SUBUNIT_NOT_FOUND`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "unknown-subunit",
                userId = "user-1",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.SUBUNIT_NOT_FOUND)
        }

        @Test
        fun `SUBUNIT scope with user not in subunit returns USER_NOT_IN_SUBUNIT`() {
            val result = service.validateWithdrawalScope(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "subunit-1",
                userId = "user-999",
                groupSubunits = groupSubunits
            )
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.USER_NOT_IN_SUBUNIT)
        }
    }

    @Nested
    inner class TitleValidation {

        @Test
        fun `validateTitle returns Valid for null title`() {
            val result = service.validateTitle(null)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateTitle returns Valid for blank title`() {
            val result = service.validateTitle("")
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateTitle returns Valid for title within max length`() {
            val result = service.validateTitle("Airport ATM")
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateTitle returns Valid for title at exactly max length`() {
            val title = "a".repeat(CashWithdrawalValidationService.MAX_TITLE_LENGTH)
            val result = service.validateTitle(title)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateTitle returns Invalid for title exceeding max length`() {
            val title = "a".repeat(CashWithdrawalValidationService.MAX_TITLE_LENGTH + 1)
            val result = service.validateTitle(title)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.TITLE_TOO_LONG)
        }
    }

    @Nested
    inner class NotesValidation {

        @Test
        fun `validateNotes returns Valid for null notes`() {
            val result = service.validateNotes(null)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateNotes returns Valid for blank notes`() {
            val result = service.validateNotes("")
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateNotes returns Valid for notes within max length`() {
            val result = service.validateNotes("Bad rate but no other option")
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateNotes returns Valid for notes at exactly max length`() {
            val notes = "n".repeat(CashWithdrawalValidationService.MAX_NOTES_LENGTH)
            val result = service.validateNotes(notes)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Valid)
        }

        @Test
        fun `validateNotes returns Invalid for notes exceeding max length`() {
            val notes = "n".repeat(CashWithdrawalValidationService.MAX_NOTES_LENGTH + 1)
            val result = service.validateNotes(notes)
            assertTrue(result is CashWithdrawalValidationService.ValidationResult.Invalid)
            val invalid = result as CashWithdrawalValidationService.ValidationResult.Invalid
            assertTrue(invalid.error == CashWithdrawalValidationService.ValidationError.NOTES_TOO_LONG)
        }
    }
}
