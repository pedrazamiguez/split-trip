package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.service.UserValidationService

class UserValidationServiceImpl : UserValidationService {
    override fun validateDisplayName(displayName: String): ValidationResult = when {
        displayName.isBlank() -> ValidationResult.Invalid("Display name cannot be empty")
        displayName.length > MAX_DISPLAY_NAME_LENGTH -> ValidationResult.Invalid(
            "Display name cannot exceed $MAX_DISPLAY_NAME_LENGTH characters"
        )
        else -> ValidationResult.Valid
    }

    override fun validateBio(bio: String?): ValidationResult = when {
        bio != null && bio.length > MAX_BIO_LENGTH -> ValidationResult.Invalid(
            "Bio cannot exceed $MAX_BIO_LENGTH characters"
        )
        else -> ValidationResult.Valid
    }

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 50
        const val MAX_BIO_LENGTH = 150
    }
}
