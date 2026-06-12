package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Contract for the event handler that manages account-status related operations.
 *
 * Handles account loading, linking providers (Google, Email/Password), unlinking,
 * and account deletion flow events.
 */
interface AccountStatusEventHandler {

    /**
     * Binds this handler to the ViewModel's state and action flows.
     */
    fun bind(
        stateFlow: MutableStateFlow<AccountStatusUiState>,
        actionsFlow: MutableSharedFlow<AccountStatusUiAction>,
        scope: CoroutineScope
    )

    fun loadAccountStatus()
    fun handleLinkGoogle(idToken: String)
    fun handleShowLinkEmailDialog()
    fun handleDismissLinkEmailDialog()
    fun handleLinkPasswordChanged(value: String)
    fun handleLinkConfirmPasswordChanged(value: String)
    fun handleSubmitLinkEmailPassword()
    fun handleUnlinkProvider(providerType: AuthProviderType)
    fun handleShowDeleteAccountDialog()
    fun handleDismissDeleteAccountDialog()
    fun handleConfirmDeleteAccount()
}
