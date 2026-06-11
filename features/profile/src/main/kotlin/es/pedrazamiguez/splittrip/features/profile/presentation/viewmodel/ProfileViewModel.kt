package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.auth.GetLinkedProvidersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.mapper.ProfileUiMapper
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.event.ProfileUiEvent
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler.ProfileAccountLinkHandler
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import kotlinx.collections.immutable.toImmutableList
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
 * through [ProfileUiMapper] for display. Delegated account linking
 * responsibilities to [ProfileAccountLinkHandler].
 */
class ProfileViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val profileUiMapper: ProfileUiMapper,
    private val getLinkedProvidersUseCase: GetLinkedProvidersUseCase,
    private val profileAccountLinkHandler: ProfileAccountLinkHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _actions = Channel<ProfileUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        profileAccountLinkHandler.bind(
            stateFlow = _uiState,
            actionsChannel = _actions,
            scope = viewModelScope,
            loadProfile = { loadProfile() }
        )
        loadProfile()
    }

    fun onEvent(event: ProfileUiEvent) {
        when (event) {
            ProfileUiEvent.LoadProfile -> loadProfile()
            is ProfileUiEvent.LinkGoogleAccount -> profileAccountLinkHandler.handleLinkGoogleAccount(event.idToken)
            ProfileUiEvent.ShowLinkEmailDialog -> profileAccountLinkHandler.handleShowLinkEmailDialog()
            ProfileUiEvent.DismissLinkEmailDialog -> profileAccountLinkHandler.handleDismissLinkEmailDialog()
            is ProfileUiEvent.LinkPasswordChanged -> profileAccountLinkHandler.handleLinkPasswordChanged(event.value)
            is ProfileUiEvent.LinkConfirmPasswordChanged -> profileAccountLinkHandler.handleLinkConfirmPasswordChanged(
                event.value
            )
            ProfileUiEvent.SubmitLinkEmailPassword -> profileAccountLinkHandler.handleSubmitLinkEmailPassword()
            is ProfileUiEvent.UnlinkProvider -> profileAccountLinkHandler.handleUnlinkProvider(event.providerType)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val user = getCurrentUserProfileUseCase()
                val linkedProvidersResult = getLinkedProvidersUseCase()
                val linkedProviders = linkedProvidersResult.getOrElse { e ->
                    Timber.e(e, "Failed to load linked providers, keeping current values")
                    _uiState.value.linkedProviders
                }.toImmutableList()
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
}
