package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.logging.maskEmail

sealed interface RegisterUiEvent {
    data class EmailChanged(val email: String) : RegisterUiEvent {
        override fun toString(): String {
            val masked = email.maskEmail()
            return "EmailChanged(email=${if (masked == email) "***" else masked})"
        }
    }
    data class DisplayNameChanged(val name: String) : RegisterUiEvent {
        override fun toString(): String = "DisplayNameChanged(name=***)"
    }
    data class PasswordChanged(val value: String) : RegisterUiEvent {
        override fun toString(): String = "PasswordChanged(input=***)"
    }
    data class ConfirmPasswordChanged(val value: String) : RegisterUiEvent {
        override fun toString(): String = "ConfirmPasswordChanged(input=***)"
    }
    data object SubmitSignUp : RegisterUiEvent
    data object DismissCollisionDialog : RegisterUiEvent
}
