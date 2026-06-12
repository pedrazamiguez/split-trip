package es.pedrazamiguez.splittrip.features.authentication.presentation.model

import es.pedrazamiguez.splittrip.core.common.presentation.UiText

data class ForgotPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val emailError: UiText? = null,
    val generalError: UiText? = null,
    val isSuccess: Boolean = false
)
