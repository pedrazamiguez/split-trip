package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus
import es.pedrazamiguez.splittrip.domain.service.PasswordValidationService

class PasswordValidationServiceImpl : PasswordValidationService {

    override fun validate(password: String): PasswordRequirementStatus = PasswordRequirementStatus(
        isMinLengthValid = password.length >= MIN_PASSWORD_LENGTH,
        hasUpperCase = password.any { it.isUpperCase() },
        hasLowerCase = password.any { it.isLowerCase() },
        hasDigit = password.any { it.isDigit() },
        hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    )

    override fun isValidPassword(password: String): Boolean = validate(password).isValid

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
