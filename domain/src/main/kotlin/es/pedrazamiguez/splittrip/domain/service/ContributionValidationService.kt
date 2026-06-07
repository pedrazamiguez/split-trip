package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Subunit

interface ContributionValidationService {

    fun validateAmount(amount: Long): ValidationResult

    fun validate(contribution: Contribution): ValidationResult

    fun validateContributionScope(
        contributionScope: PayerType,
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult

    fun validateSubunit(
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
        SUBUNIT_NOT_FOUND,
        SUBUNIT_REQUIRED,
        INVALID_SUBUNIT_FOR_SCOPE,
        USER_NOT_IN_SUBUNIT
    }
}
