package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Interface for the event handler managing account linking and unlinking in the Profile screen.
 */
interface ProfileAccountLinkHandler {

    /**
     * Binds this handler to the ViewModel's state, action channels, scope, and reload callback.
     */
    fun bind(
        stateFlow: MutableStateFlow<ProfileUiState>,
        actionsChannel: Channel<ProfileUiAction>,
        scope: CoroutineScope,
        loadProfile: () -> Unit
    )

    fun handleLinkGoogleAccount(idToken: String)
    fun handleShowLinkEmailDialog()
    fun handleDismissLinkEmailDialog()
    fun handleLinkPasswordChanged(value: String)
    fun handleLinkConfirmPasswordChanged(value: String)
    fun handleSubmitLinkEmailPassword()
    fun handleUnlinkProvider(providerType: AuthProviderType)
}
