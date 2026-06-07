package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import java.math.BigDecimal

interface CashWithdrawalValidationService {

    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_NOTES_LENGTH = 500
    }

    fun validateAmountWithdrawn(amount: Long): ValidationResult

    fun validateTitle(title: String?): ValidationResult

    fun validateNotes(notes: String?): ValidationResult

    fun validateDeductedBaseAmount(amount: Long): ValidationResult

    fun validateCurrency(currency: String): ValidationResult

    fun validateExchangeRate(rate: BigDecimal): ValidationResult

    fun validateWithdrawalScope(
        withdrawalScope: PayerType,
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult

    sealed interface ValidationResult {
        data object Valid : ValidationResult
        data class Invalid(val error: ValidationError) : ValidationResult
    }

    enum class ValidationError {
        AMOUNT_MUST_BE_POSITIVE,
        DEDUCTED_AMOUNT_MUST_BE_POSITIVE,
        CURRENCY_REQUIRED,
        EXCHANGE_RATE_MUST_BE_POSITIVE,
        TITLE_TOO_LONG,
        NOTES_TOO_LONG,
        SUBUNIT_REQUIRED,
        SUBUNIT_NOT_FOUND,
        USER_NOT_IN_SUBUNIT,
        INVALID_SUBUNIT_FOR_SCOPE
    }
}
