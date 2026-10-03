package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.ValidationResult

interface UserValidationService {

    companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 50
        const val MAX_BIO_LENGTH = 150
    }

    fun validateDisplayName(displayName: String): ValidationResult
    fun validateBio(bio: String?): ValidationResult
}
