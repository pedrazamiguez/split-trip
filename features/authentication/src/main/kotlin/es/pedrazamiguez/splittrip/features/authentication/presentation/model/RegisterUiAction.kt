package es.pedrazamiguez.splittrip.features.authentication.presentation.model

sealed interface RegisterUiAction {
    data object NavigateBack : RegisterUiAction
    data object RegisterSuccess : RegisterUiAction
}
