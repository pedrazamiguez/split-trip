package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType

sealed interface ProfileUiEvent {
    data object LoadProfile : ProfileUiEvent
    data class LinkGoogleAccount(val idToken: String) : ProfileUiEvent
    data object ShowLinkEmailDialog : ProfileUiEvent
    data object DismissLinkEmailDialog : ProfileUiEvent
    data class LinkPasswordChanged(val value: String) : ProfileUiEvent
    data class LinkConfirmPasswordChanged(val value: String) : ProfileUiEvent
    data object SubmitLinkEmailPassword : ProfileUiEvent
    data class UnlinkProvider(val providerType: AuthProviderType) : ProfileUiEvent
}
