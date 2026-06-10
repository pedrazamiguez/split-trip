package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.logging.maskEmail

sealed interface RegisterUiEvent {
    data class EmailChanged(val email: String) : RegisterUiEvent {
        override fun toString(): String =
            "EmailChanged(email=${if (email.contains('@')) email.maskEmail() else "***"})"
    }
    data class DisplayNameChanged(val name: String) : RegisterUiEvent
    data class PasswordChanged(val value: String) : RegisterUiEvent {
        override fun toString(): String = "PasswordChanged(input=***)"
    }
    data class ConfirmPasswordChanged(val value: String) : RegisterUiEvent {
        override fun toString(): String = "ConfirmPasswordChanged(input=***)"
    }
    data object SubmitSignUp : RegisterUiEvent
}
