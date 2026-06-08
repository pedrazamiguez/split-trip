package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService.ValidationResult

class ContributionValidationServiceImpl : ContributionValidationService {

    override fun validateAmount(amount: Long): ValidationResult = when {
        amount <= 0 -> ValidationResult.Invalid(ContributionValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE)
        else -> ValidationResult.Valid
    }

    override fun validate(contribution: Contribution): ValidationResult = validateAmount(contribution.amount)

    override fun validateContributionScope(
        contributionScope: PayerType,
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult = when (contributionScope) {
        PayerType.SUBUNIT -> validateSubunitScope(subunitId, userId, groupSubunits)
        else -> if (subunitId != null) {
            ValidationResult.Invalid(ContributionValidationService.ValidationError.INVALID_SUBUNIT_FOR_SCOPE)
        } else {
            ValidationResult.Valid
        }
    }

    override fun validateSubunit(
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult {
        if (subunitId == null) return ValidationResult.Valid
        val subunit = groupSubunits.find { it.id == subunitId }
            ?: return ValidationResult.Invalid(ContributionValidationService.ValidationError.SUBUNIT_NOT_FOUND)
        if (userId !in subunit.memberIds) {
            return ValidationResult.Invalid(ContributionValidationService.ValidationError.USER_NOT_IN_SUBUNIT)
        }
        return ValidationResult.Valid
    }

    private fun validateSubunitScope(
        subunitId: String?,
        userId: String,
        groupSubunits: List<Subunit>
    ): ValidationResult {
        if (subunitId.isNullOrBlank()) {
            return ValidationResult.Invalid(ContributionValidationService.ValidationError.SUBUNIT_REQUIRED)
        }
        val subunit = groupSubunits.find { it.id == subunitId }
            ?: return ValidationResult.Invalid(ContributionValidationService.ValidationError.SUBUNIT_NOT_FOUND)
        return if (userId !in subunit.memberIds) {
            ValidationResult.Invalid(ContributionValidationService.ValidationError.USER_NOT_IN_SUBUNIT)
        } else {
            ValidationResult.Valid
        }
    }
}
