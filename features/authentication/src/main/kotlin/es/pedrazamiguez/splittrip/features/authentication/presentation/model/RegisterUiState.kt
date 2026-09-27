package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail
import es.pedrazamiguez.splittrip.domain.model.PasswordRequirementStatus

data class RegisterUiState(
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val showCollisionDialog: Boolean = false,
    val collisionEmail: String = "",
    val passwordRequirementStatus: PasswordRequirementStatus = PasswordRequirementStatus()
) {
    override fun toString(): String {
        val maskedEmail = email.maskEmail()
        val emailString = if (maskedEmail == email) "***" else maskedEmail
        return "RegisterUiState(email=$emailString, displayName=***, password=***, " +
            "confirmPassword=***, isLoading=$isLoading, error=$error, " +
            "passwordRequirementStatus=$passwordRequirementStatus)"
    }
}
