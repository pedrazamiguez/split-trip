package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail

sealed interface AuthenticationUiEvent {
    data class EmailChanged(val email: String) : AuthenticationUiEvent {
        override fun toString(): String =
            "EmailChanged(email=${if (email.contains('@')) email.maskEmail() else "***"})"
    }
    data class PasswordChanged(val value: String) : AuthenticationUiEvent {
        override fun toString(): String = "PasswordChanged(input=***)"
    }
    data object SubmitLogin : AuthenticationUiEvent
    data class GoogleSignInResult(val idToken: String) : AuthenticationUiEvent {
        override fun toString(): String = "GoogleSignInResult(idToken=***)"
    }

    data object GoogleSignInFailed : AuthenticationUiEvent

    data class CollisionPasswordChanged(val value: String) : AuthenticationUiEvent {
        override fun toString(): String = "CollisionPasswordChanged(input=***)"
    }
    data object SubmitCollisionMerge : AuthenticationUiEvent
    data object DismissCollisionDialog : AuthenticationUiEvent
    data object ContinueAsGuest : AuthenticationUiEvent
}
