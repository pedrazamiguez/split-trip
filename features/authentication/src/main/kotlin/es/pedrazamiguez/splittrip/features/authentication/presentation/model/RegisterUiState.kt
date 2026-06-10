package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.maskEmail

data class RegisterUiState(
    val email: String = "",
    val displayName: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null
) {
    override fun toString(): String =
        "RegisterUiState(email=${if (email.contains(
                '@'
            )
        ) {
            email.maskEmail()
        } else {
            "***"
        }}, displayName=$displayName, password=***, confirmPassword=***, isLoading=$isLoading, error=$error)"
}
