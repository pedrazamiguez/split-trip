package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail

sealed interface ForgotPasswordUiEvent {
    data class EmailChanged(val email: String) : ForgotPasswordUiEvent {
        override fun toString(): String =
            "EmailChanged(email=${if (email.contains('@')) email.maskEmail() else "***"})"
    }
    data object Submit : ForgotPasswordUiEvent
    data object BackClicked : ForgotPasswordUiEvent
}
