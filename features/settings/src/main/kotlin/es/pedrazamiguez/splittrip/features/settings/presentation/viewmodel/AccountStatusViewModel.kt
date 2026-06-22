package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler.AccountStatusEventHandler
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Account Status screen.
 *
 * Delegates all event processing to [AccountStatusEventHandler] to maintain a clean,
 * single-responsibility controller.
 */
class AccountStatusViewModel(
    private val accountStatusEventHandler: AccountStatusEventHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountStatusUiState())
    val uiState: StateFlow<AccountStatusUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<AccountStatusUiAction>()
    val actions: SharedFlow<AccountStatusUiAction> = _actions.asSharedFlow()

    init {
        accountStatusEventHandler.bind(_uiState, _actions, viewModelScope)
        accountStatusEventHandler.loadAccountStatus()
    }

    fun onEvent(event: AccountStatusUiEvent) {
        when (event) {
            AccountStatusUiEvent.LoadAccountStatus ->
                accountStatusEventHandler.loadAccountStatus()

            is AccountStatusUiEvent.LinkGoogle ->
                accountStatusEventHandler.handleLinkGoogle(event.idToken)

            AccountStatusUiEvent.ShowLinkEmailDialog ->
                accountStatusEventHandler.handleShowLinkEmailDialog()

            AccountStatusUiEvent.DismissLinkEmailDialog ->
                accountStatusEventHandler.handleDismissLinkEmailDialog()

            is AccountStatusUiEvent.LinkEmailChanged ->
                accountStatusEventHandler.handleLinkEmailChanged(event.value)

            is AccountStatusUiEvent.LinkPasswordChanged ->
                accountStatusEventHandler.handleLinkPasswordChanged(event.value)

            is AccountStatusUiEvent.LinkConfirmPasswordChanged ->
                accountStatusEventHandler.handleLinkConfirmPasswordChanged(event.value)

            AccountStatusUiEvent.SubmitLinkEmailPassword ->
                accountStatusEventHandler.handleSubmitLinkEmailPassword()

            is AccountStatusUiEvent.UnlinkProvider ->
                accountStatusEventHandler.handleUnlinkProvider(event.providerType)

            AccountStatusUiEvent.ShowDeleteAccountDialog ->
                accountStatusEventHandler.handleShowDeleteAccountDialog()

            AccountStatusUiEvent.DismissDeleteAccountDialog ->
                accountStatusEventHandler.handleDismissDeleteAccountDialog()

            AccountStatusUiEvent.ConfirmDeleteAccount ->
                accountStatusEventHandler.handleConfirmDeleteAccount()
        }
    }
}
