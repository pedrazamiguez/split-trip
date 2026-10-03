package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.UserValidationService

class UserValidationServiceImpl : UserValidationService {
    override fun validateDisplayName(displayName: String): ValidationResult {
        val trimmed = displayName.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Invalid("Display name cannot be empty")
            trimmed.length > UserValidationService.MAX_DISPLAY_NAME_LENGTH -> ValidationResult.Invalid(
                "Display name cannot exceed ${UserValidationService.MAX_DISPLAY_NAME_LENGTH} characters"
            )
            else -> ValidationResult.Valid
        }
    }

    override fun validateBio(bio: String?): ValidationResult = when {
        bio != null && bio.length > UserValidationService.MAX_BIO_LENGTH -> ValidationResult.Invalid(
            "Bio cannot exceed ${UserValidationService.MAX_BIO_LENGTH} characters"
        )
        else -> ValidationResult.Valid
    }
}
