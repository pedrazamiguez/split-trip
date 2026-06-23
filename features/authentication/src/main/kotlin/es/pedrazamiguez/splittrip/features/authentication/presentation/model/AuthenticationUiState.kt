package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

data class AuthenticationUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val isGuestLoading: Boolean = false,
    val error: UiText? = null,
    val showCollisionDialog: Boolean = false,
    val collisionEmail: String = "",
    val collisionPassword: String = "",
    val pendingGoogleIdToken: String? = null,
    val isMerging: Boolean = false,
    val mergeError: UiText? = null
)
