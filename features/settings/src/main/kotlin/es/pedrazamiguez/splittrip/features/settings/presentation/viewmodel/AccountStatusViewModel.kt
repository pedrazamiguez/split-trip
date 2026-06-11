package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountStatusUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AccountStatusViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val getLinkedProvidersUseCase: GetLinkedProvidersUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailPasswordUseCase: LinkEmailPasswordUseCase,
    private val unlinkProviderUseCase: UnlinkProviderUseCase,
    private val accountStatusUiMapper: AccountStatusUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountStatusUiState())
    val uiState: StateFlow<AccountStatusUiState> = _uiState.asStateFlow()

    private val _actions = Channel<AccountStatusUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        loadAccountStatus()
    }

    fun onEvent(event: AccountStatusUiEvent) {
        when (event) {
            AccountStatusUiEvent.LoadAccountStatus -> loadAccountStatus()
            is AccountStatusUiEvent.LinkGoogle -> handleLinkGoogle(event.idToken)
            AccountStatusUiEvent.ShowLinkEmailDialog -> handleShowLinkEmailDialog()
            AccountStatusUiEvent.DismissLinkEmailDialog -> handleDismissLinkEmailDialog()
            is AccountStatusUiEvent.LinkPasswordChanged -> handleLinkPasswordChanged(event.value)
            is AccountStatusUiEvent.LinkConfirmPasswordChanged -> handleLinkConfirmPasswordChanged(event.value)
            AccountStatusUiEvent.SubmitLinkEmailPassword -> handleSubmitLinkEmailPassword()
            is AccountStatusUiEvent.UnlinkProvider -> handleUnlinkProvider(event.providerType)
            AccountStatusUiEvent.ShowDeleteAccountDialog -> handleShowDeleteAccountDialog()
            AccountStatusUiEvent.DismissDeleteAccountDialog -> handleDismissDeleteAccountDialog()
            AccountStatusUiEvent.ConfirmDeleteAccount -> handleConfirmDeleteAccount()
        }
    }

    private fun loadAccountStatus() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val user = getCurrentUserProfileUseCase()
                val linkedProvidersResult = getLinkedProvidersUseCase()
                val linkedProviders = linkedProvidersResult.getOrElse { e ->
                    Timber.e(e, "Failed to load linked providers")
                    _uiState.value.linkedProviders
                }.toImmutableList()

                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = user.email,
                            joinDateText = accountStatusUiMapper.formatJoinDate(user.createdAt),
                            linkedProviders = linkedProviders
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    println("VM: sending ShowError - User not found")
                    _actions.send(
                        AccountStatusUiAction.ShowError(
                            UiText.StringResource(R.string.account_status_error_prefix, "User not found")
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account status")
                _uiState.update { it.copy(isLoading = false) }
                println("VM: sending ShowError - Exception: ${e.message}")
                _actions.send(
                    AccountStatusUiAction.ShowError(
                        UiText.StringResource(
                            R.string.account_status_error_prefix,
                            e.localizedMessage ?: "Unknown error"
                        )
                    )
                )
            }
        }
    }

    private fun handleLinkGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true) }
            linkGoogleAccountUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(
                        AccountStatusUiAction.ShowSuccess(UiText.StringResource(R.string.account_status_link_success))
                    )
                    loadAccountStatus()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to link Google account")
                    _uiState.update { it.copy(isLinking = false) }
                    println("VM: Link Google failed, sending error: ${e.message}")
                    _actions.send(
                        AccountStatusUiAction.ShowError(
                            UiText.StringResource(
                                R.string.account_status_error_prefix,
                                e.localizedMessage ?: "Failed to link Google"
                            )
                        )
                    )
                }
        }
    }

    private fun handleShowLinkEmailDialog() {
        _uiState.update {
            it.copy(
                showLinkEmailDialog = true,
                linkPasswordInput = "",
                linkConfirmPasswordInput = "",
                linkPasswordError = null
            )
        }
    }

    private fun handleDismissLinkEmailDialog() {
        _uiState.update { it.copy(showLinkEmailDialog = false) }
    }

    private fun handleLinkPasswordChanged(value: String) {
        _uiState.update { it.copy(linkPasswordInput = value) }
    }

    private fun handleLinkConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(linkConfirmPasswordInput = value) }
    }

    private fun handleSubmitLinkEmailPassword() {
        val password = _uiState.value.linkPasswordInput
        val confirmPassword = _uiState.value.linkConfirmPasswordInput
        val email = _uiState.value.email

        if (password.isEmpty() || confirmPassword.isEmpty()) {
            _uiState.update {
                it.copy(
                    linkPasswordError = UiText.StringResource(R.string.account_status_link_email_dialog_error_empty)
                )
            }
            return
        }

        if (password != confirmPassword) {
            _uiState.update {
                it.copy(
                    linkPasswordError = UiText.StringResource(R.string.account_status_link_email_dialog_error_mismatch)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true, linkPasswordError = null) }
            linkEmailPasswordUseCase(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false, showLinkEmailDialog = false) }
                    _actions.send(
                        AccountStatusUiAction.ShowSuccess(UiText.StringResource(R.string.account_status_link_success))
                    )
                    loadAccountStatus()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to link Email/Password")
                    _uiState.update {
                        it.copy(
                            isLinking = false,
                            linkPasswordError = UiText.StringResource(
                                R.string.account_status_error_prefix,
                                e.localizedMessage ?: "Failed to link email/password"
                            )
                        )
                    }
                }
        }
    }

    private fun handleUnlinkProvider(providerType: AuthProviderType) {
        if (_uiState.value.linkedProviders.size <= 1) {
            viewModelScope.launch {
                println("VM: Unlinking last provider, sending error")
                _actions.send(
                    AccountStatusUiAction.ShowError(
                        UiText.StringResource(R.string.account_status_unlink_error_last_provider)
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true) }
            unlinkProviderUseCase(providerType)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(
                        AccountStatusUiAction.ShowSuccess(UiText.StringResource(R.string.account_status_unlink_success))
                    )
                    loadAccountStatus()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to unlink provider")
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(
                        AccountStatusUiAction.ShowError(
                            UiText.StringResource(
                                R.string.account_status_error_prefix,
                                e.localizedMessage ?: "Failed to unlink"
                            )
                        )
                    )
                }
        }
    }

    private fun handleShowDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    private fun handleDismissDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
    }

    private fun handleConfirmDeleteAccount() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
        viewModelScope.launch {
            println("VM: Confirm delete account, sending success")
            _actions.send(
                AccountStatusUiAction.ShowSuccess(
                    UiText.DynamicString("Account deletion is out of scope / tracked in #1132")
                )
            )
        }
    }
}
