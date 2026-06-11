package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event

sealed interface ProfileUiEvent {
    data object LoadProfile : ProfileUiEvent
}
