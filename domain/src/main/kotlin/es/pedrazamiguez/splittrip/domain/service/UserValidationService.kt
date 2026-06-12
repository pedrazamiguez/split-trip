package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.ValidationResult

interface UserValidationService {
    fun validateDisplayName(displayName: String): ValidationResult
    fun validateBio(bio: String?): ValidationResult
}
