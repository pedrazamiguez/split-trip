package es.pedrazamiguez.splittrip.domain.model

data class PasswordRequirementStatus(
    val isMinLengthValid: Boolean = false,
    val hasUpperCase: Boolean = false,
    val hasLowerCase: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false
) {
    val isValid: Boolean
        get() = isMinLengthValid && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
}
