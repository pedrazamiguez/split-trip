package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.navigation.Routes
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.IsUserAnonymousUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetBiometricLockEnabledUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.AccountSecurityUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.AccountSecurityUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.AccountSecurityUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.AccountSecurityUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AccountSecurityViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val isUserAnonymousUseCase: IsUserAnonymousUseCase,
    private val getLinkedProvidersUseCase: GetLinkedProvidersUseCase,
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase,
    private val getBiometricCapabilityUseCase: GetBiometricCapabilityUseCase,
    private val getBiometricLockEnabledUseCase: GetBiometricLockEnabledUseCase,
    private val setBiometricLockEnabledUseCase: SetBiometricLockEnabledUseCase,
    private val accountSecurityUiMapper: AccountSecurityUiMapper
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSecurityUiState())
    val uiState: StateFlow<AccountSecurityUiState> = _uiState.asStateFlow()

    private val _actions = Channel<AccountSecurityUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        observeBiometricPreference()
        loadAccountSecurity()
    }

    fun onEvent(event: AccountSecurityUiEvent) {
        when (event) {
            AccountSecurityUiEvent.LoadAccountSecurity -> loadAccountSecurity()
            AccountSecurityUiEvent.RequestPasswordResetConfirmation -> {
                _uiState.update { it.copy(showPasswordResetConfirmDialog = true) }
            }
            AccountSecurityUiEvent.DismissPasswordResetConfirmation -> {
                _uiState.update { it.copy(showPasswordResetConfirmDialog = false) }
            }
            AccountSecurityUiEvent.ConfirmSendPasswordReset -> handleConfirmSendPasswordReset()
            is AccountSecurityUiEvent.ToggleBiometricLock -> handleToggleBiometricLock(event.enabled)
            AccountSecurityUiEvent.BiometricConfirmationSuccess -> handleBiometricConfirmationSuccess()
            AccountSecurityUiEvent.NavigateToAccountStatus -> {
                viewModelScope.launch {
                    _actions.send(AccountSecurityUiAction.NavigateToRoute(Routes.SETTINGS_ACCOUNT_STATUS))
                }
            }
        }
    }

    private fun observeBiometricPreference() {
        viewModelScope.launch {
            try {
                getBiometricLockEnabledUseCase().collect { isEnabled ->
                    _uiState.update { it.copy(biometricLockEnabled = isEnabled) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to observe biometric lock preference")
            }
        }
    }

    private fun loadAccountSecurity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val isAnon = isUserAnonymousUseCase().firstOrNull() ?: false
                val profile = getCurrentUserProfileUseCase()
                val providersResult = getLinkedProvidersUseCase()
                val providers = providersResult.getOrDefault(emptyList())
                val capability = getBiometricCapabilityUseCase()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        email = profile?.email.orEmpty(),
                        isAnonymous = isAnon,
                        linkedProviders = providers.toImmutableList(),
                        biometricCapability = capability
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account security")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleConfirmSendPasswordReset() {
        val email = _uiState.value.email
        if (email.isBlank()) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showPasswordResetConfirmDialog = false,
                    isPasswordResetSending = true
                )
            }
            try {
                val result = sendPasswordResetEmailUseCase(email)
                _uiState.update { it.copy(isPasswordResetSending = false) }
                if (result.isSuccess) {
                    val message = accountSecurityUiMapper.formatPasswordResetSuccessMessage(email)
                    _actions.send(AccountSecurityUiAction.ShowTopPill(message))
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    val message = UiText.StringResource(R.string.account_security_error_prefix, errorMsg)
                    _actions.send(AccountSecurityUiAction.ShowTopPill(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to send password reset email")
                _uiState.update { it.copy(isPasswordResetSending = false) }
                val errorMsg = e.message ?: "Unknown error"
                val message = UiText.StringResource(R.string.account_security_error_prefix, errorMsg)
                _actions.send(AccountSecurityUiAction.ShowTopPill(message))
            }
        }
    }

    private fun handleToggleBiometricLock(enabled: Boolean) {
        if (enabled) {
            if (_uiState.value.isBiometricToggleEnabled) {
                viewModelScope.launch {
                    _actions.send(AccountSecurityUiAction.RequestBiometricConfirmation)
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    setBiometricLockEnabledUseCase(false)
                    _uiState.update { it.copy(biometricLockEnabled = false) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to disable biometric lock preference")
                    val message = UiText.StringResource(
                        R.string.account_security_error_prefix,
                        e.message ?: "Failed to disable biometric lock"
                    )
                    _actions.send(AccountSecurityUiAction.ShowTopPill(message))
                }
            }
        }
    }

    private fun handleBiometricConfirmationSuccess() {
        viewModelScope.launch {
            try {
                setBiometricLockEnabledUseCase(true)
                _uiState.update { it.copy(biometricLockEnabled = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to enable biometric lock preference")
                val message = UiText.StringResource(
                    R.string.account_security_error_prefix,
                    e.message ?: "Failed to enable biometric lock"
                )
                _actions.send(AccountSecurityUiAction.ShowTopPill(message))
            }
        }
    }
}
