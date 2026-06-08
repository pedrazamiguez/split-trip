package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService.ValidationResult
import java.math.BigDecimal

class CashWithdrawalValidationServiceImpl : CashWithdrawalValidationService {

    override fun validateAmountWithdrawn(amount: Long): ValidationResult = when {
        amount <= 0 -> ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE)
        else -> ValidationResult.Valid
    }

    override fun validateTitle(title: String?): ValidationResult = when {
        title != null && title.length > CashWithdrawalValidationService.MAX_TITLE_LENGTH ->
            ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.TITLE_TOO_LONG)
        else -> ValidationResult.Valid
    }

    override fun validateNotes(notes: String?): ValidationResult = when {
        notes != null && notes.length > CashWithdrawalValidationService.MAX_NOTES_LENGTH ->
            ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.NOTES_TOO_LONG)
        else -> ValidationResult.Valid
    }

    override fun validateDeductedBaseAmount(amount: Long): ValidationResult = when {
        amount <= 0 -> ValidationResult.Invalid(
            CashWithdrawalValidationService.ValidationError.DEDUCTED_AMOUNT_MUST_BE_POSITIVE
        )
        else -> ValidationResult.Valid
    }

    override fun validateCurrency(currency: String): ValidationResult = when {
        currency.isBlank() -> ValidationResult.Invalid(
            CashWithdrawalValidationService.ValidationError.CURRENCY_REQUIRED
        )
        else -> ValidationResult.Valid
    }

    override fun validateExchangeRate(rate: BigDecimal): ValidationResult = when {
        rate.compareTo(BigDecimal.ZERO) <= 0 ->
            ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.EXCHANGE_RATE_MUST_BE_POSITIVE)
        else -> ValidationResult.Valid
    }

    override fun validateWithdrawalScope(
        withdrawalScope: PayerType,
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult = when (withdrawalScope) {
        PayerType.SUBUNIT -> validateSubunitScope(subunitId, userId, groupSubunits)
        else -> if (subunitId != null) {
            ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE)
        } else {
            ValidationResult.Valid
        }
    }

    private fun validateSubunitScope(
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult {
        if (subunitId.isNullOrBlank()) {
            return ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.SUBUNIT_REQUIRED)
        }
        val subunit = groupSubunits.find { it.id == subunitId }
            ?: return ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.SUBUNIT_NOT_FOUND)
        return if (userId !in subunit.memberIds) {
            ValidationResult.Invalid(CashWithdrawalValidationService.ValidationError.USER_NOT_IN_SUBUNIT)
        } else {
            ValidationResult.Valid
        }
    }
}
