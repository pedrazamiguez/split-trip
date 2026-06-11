package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
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
    private val profileUiMapper: ProfileUiMapper
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
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            try {
                val user = getCurrentUserProfileUseCase()
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            profile = profileUiMapper.toProfileUiModel(user)
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
