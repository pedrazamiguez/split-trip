package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType

sealed interface AccountStatusUiEvent {
    data object LoadAccountStatus : AccountStatusUiEvent
    data class LinkGoogle(val idToken: String) : AccountStatusUiEvent
    data object ShowLinkEmailDialog : AccountStatusUiEvent
    data object DismissLinkEmailDialog : AccountStatusUiEvent
    data class LinkPasswordChanged(val value: String) : AccountStatusUiEvent
    data class LinkConfirmPasswordChanged(val value: String) : AccountStatusUiEvent
    data object SubmitLinkEmailPassword : AccountStatusUiEvent
    data class UnlinkProvider(val providerType: AuthProviderType) : AccountStatusUiEvent
    data object ShowDeleteAccountDialog : AccountStatusUiEvent
    data object DismissDeleteAccountDialog : AccountStatusUiEvent
    data object ConfirmDeleteAccount : AccountStatusUiEvent
}
