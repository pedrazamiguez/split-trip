package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountStatusUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountStatusUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountStatusUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Handles account linking, unlinking, and deletion flow events for the Account Status screen.
 */
class AccountStatusEventHandlerImpl(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val getLinkedProvidersUseCase: GetLinkedProvidersUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailPasswordUseCase: LinkEmailPasswordUseCase,
    private val unlinkProviderUseCase: UnlinkProviderUseCase,
    private val authenticationService: AuthenticationService,
    private val accountStatusUiMapper: AccountStatusUiMapper
) : AccountStatusEventHandler {

    private lateinit var _uiState: MutableStateFlow<AccountStatusUiState>
    private lateinit var _actions: MutableSharedFlow<AccountStatusUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AccountStatusUiState>,
        actionsFlow: MutableSharedFlow<AccountStatusUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun loadAccountStatus() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val isAnon = authenticationService.isAnonymous()
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
                            linkedProviders = linkedProviders,
                            isAnonymous = false
                        )
                    }
                } else if (isAnon) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            email = "",
                            joinDateText = "",
                            linkedProviders = linkedProviders,
                            isAnonymous = true
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                    Timber.w("User profile is null during account status load")
                    _actions.emit(
                        AccountStatusUiAction.ShowError(
                            UiText.StringResource(
                                R.string.account_status_error_prefix,
                                UiText.StringResource(R.string.account_status_error_user_not_found)
                            )
                        )
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account status")
                _uiState.update { it.copy(isLoading = false) }
                _actions.emit(
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

    override fun handleLinkGoogle(idToken: String) {
        scope.launch {
            _uiState.update { it.copy(isLinking = true) }
            linkGoogleAccountUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.emit(
                        AccountStatusUiAction.ShowSuccess(UiText.StringResource(R.string.account_status_link_success))
                    )
                    loadAccountStatus()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to link Google account")
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.emit(
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

    override fun handleShowLinkEmailDialog() {
        _uiState.update {
            it.copy(
                showLinkEmailDialog = true,
                linkEmailInput = "",
                linkPasswordInput = "",
                linkConfirmPasswordInput = "",
                linkPasswordError = null
            )
        }
    }

    override fun handleDismissLinkEmailDialog() {
        _uiState.update { it.copy(showLinkEmailDialog = false) }
    }

    override fun handleLinkEmailChanged(value: String) {
        _uiState.update { it.copy(linkEmailInput = value) }
    }

    override fun handleLinkPasswordChanged(value: String) {
        _uiState.update { it.copy(linkPasswordInput = value) }
    }

    override fun handleLinkConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(linkConfirmPasswordInput = value) }
    }

    override fun handleSubmitLinkEmailPassword() {
        val isAnon = _uiState.value.isAnonymous
        val email = if (isAnon) _uiState.value.linkEmailInput.trim() else _uiState.value.email
        val password = _uiState.value.linkPasswordInput
        val confirmPassword = _uiState.value.linkConfirmPasswordInput

        if (email.isEmpty()) {
            _uiState.update {
                it.copy(
                    linkPasswordError = UiText.StringResource(
                        R.string.account_status_link_email_dialog_error_email_empty
                    )
                )
            }
            return
        }

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

        scope.launch {
            _uiState.update { it.copy(isLinking = true, linkPasswordError = null) }
            linkEmailPasswordUseCase(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false, showLinkEmailDialog = false) }
                    _actions.emit(
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

    override fun handleUnlinkProvider(providerType: AuthProviderType) {
        if (_uiState.value.linkedProviders.size <= 1) {
            scope.launch {
                _actions.emit(
                    AccountStatusUiAction.ShowError(
                        UiText.StringResource(R.string.account_status_unlink_error_last_provider)
                    )
                )
            }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLinking = true) }
            unlinkProviderUseCase(providerType)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.emit(
                        AccountStatusUiAction.ShowSuccess(UiText.StringResource(R.string.account_status_unlink_success))
                    )
                    loadAccountStatus()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to unlink provider")
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.emit(
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

    override fun handleShowDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = true) }
    }

    override fun handleDismissDeleteAccountDialog() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
    }

    override fun handleConfirmDeleteAccount() {
        _uiState.update { it.copy(showDeleteAccountDialog = false) }
        scope.launch {
            _actions.emit(
                AccountStatusUiAction.ShowSuccess(
                    UiText.StringResource(R.string.account_status_delete_request_submitted)
                )
            )
        }
    }
}
