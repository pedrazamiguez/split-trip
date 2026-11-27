package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action

sealed interface DeveloperServicesUiAction {
    data object NavigateBack : DeveloperServicesUiAction
}
