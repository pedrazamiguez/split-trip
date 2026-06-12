package es.pedrazamiguez.splittrip.features.authentication.presentation.model

sealed interface ForgotPasswordUiAction {
    data object NavigateBack : ForgotPasswordUiAction
}
