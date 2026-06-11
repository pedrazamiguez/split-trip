package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the Profile screen.
 *
 * Loads the current authenticated user's profile data and maps it
 * through [ProfileUiMapper] for display.
 */
class ProfileViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val profileUiMapper: ProfileUiMapper,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailPasswordUseCase: LinkEmailPasswordUseCase,
    private val unlinkProviderUseCase: UnlinkProviderUseCase,
    private val getLinkedProvidersUseCase: GetLinkedProvidersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _actions = Channel<ProfileUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.LoadProfile -> loadProfile()
            is ProfileUiEvent.LinkGoogleAccount -> linkGoogleAccount(event.idToken)
            ProfileUiEvent.ShowLinkEmailDialog -> _uiState.update {
                it.copy(
                    showLinkEmailDialog = true,
                    linkPasswordInput = "",
                    linkConfirmPasswordInput = "",
                    linkPasswordError = null
                )
            }
            ProfileUiEvent.DismissLinkEmailDialog -> _uiState.update { it.copy(showLinkEmailDialog = false) }
            is ProfileUiEvent.LinkPasswordChanged -> _uiState.update { it.copy(linkPasswordInput = event.value) }
            is ProfileUiEvent.LinkConfirmPasswordChanged -> _uiState.update {
                it.copy(linkConfirmPasswordInput = event.value)
            }
            ProfileUiEvent.SubmitLinkEmailPassword -> submitLinkEmailPassword()
            is ProfileUiEvent.UnlinkProvider -> unlinkProvider(event.providerType)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val user = getCurrentUserProfileUseCase()
                val linkedProvidersResult = getLinkedProvidersUseCase()
                val linkedProviders = linkedProvidersResult.getOrDefault(emptyList())
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profileUiMapper.toProfileUiModel(user),
                            linkedProviders = linkedProviders
                        )
                    }
                } else {
                    val errorText = UiText.StringResource(R.string.profile_error_loading)
                    _uiState.update {
                        it.copy(isLoading = false, hasError = true)
                    }
                    _actions.send(ProfileUiAction.ShowError(errorText))
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load profile")
                val errorText = UiText.StringResource(R.string.profile_error_loading)
                _uiState.update {
                    it.copy(isLoading = false, hasError = true)
                }
                _actions.send(ProfileUiAction.ShowError(errorText))
            }
        }
    }

    private fun linkGoogleAccount(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true) }
            linkGoogleAccountUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(ProfileUiAction.ShowSuccess(UiText.StringResource(R.string.profile_link_success)))
                    loadProfile()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to link Google account")
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(ProfileUiAction.ShowError(UiText.DynamicString(e.message ?: "Linking failed")))
                }
        }
    }

    private fun submitLinkEmailPassword() {
        val password = _uiState.value.linkPasswordInput
        val confirmPassword = _uiState.value.linkConfirmPasswordInput
        val email = _uiState.value.profile?.email ?: return

        if (password.length < MIN_PASSWORD_LENGTH) {
            _uiState.update {
                it.copy(linkPasswordError = UiText.StringResource(R.string.profile_link_error_password_length))
            }
            return
        }

        if (password != confirmPassword) {
            _uiState.update {
                it.copy(linkPasswordError = UiText.StringResource(R.string.profile_link_error_passwords_match))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true, linkPasswordError = null) }
            linkEmailPasswordUseCase(email, password)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false, showLinkEmailDialog = false) }
                    _actions.send(ProfileUiAction.ShowSuccess(UiText.StringResource(R.string.profile_link_success)))
                    loadProfile()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to link Email/Password")
                    _uiState.update {
                        it.copy(
                            isLinking = false,
                            linkPasswordError = UiText.DynamicString(e.message ?: "Linking failed")
                        )
                    }
                }
        }
    }

    private fun unlinkProvider(providerType: AuthProviderType) {
        if (_uiState.value.linkedProviders.size <= 1) {
            viewModelScope.launch {
                _actions.send(
                    ProfileUiAction.ShowError(UiText.StringResource(R.string.profile_unlink_error_last_provider))
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLinking = true) }
            unlinkProviderUseCase(providerType)
                .onSuccess {
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(ProfileUiAction.ShowSuccess(UiText.StringResource(R.string.profile_unlink_success)))
                    loadProfile()
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to unlink provider")
                    _uiState.update { it.copy(isLinking = false) }
                    _actions.send(ProfileUiAction.ShowError(UiText.DynamicString(e.message ?: "Unlinking failed")))
                }
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
