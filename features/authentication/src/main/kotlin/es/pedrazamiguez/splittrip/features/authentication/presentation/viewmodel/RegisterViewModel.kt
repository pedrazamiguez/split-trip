package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.RegisterSubmitEventHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class RegisterViewModel(
    private val registerSubmitEventHandler: RegisterSubmitEventHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _actions = Channel<RegisterUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    init {
        registerSubmitEventHandler.bind(_uiState, _actions, viewModelScope)
    }

    fun onEvent(event: RegisterUiEvent) {
        Timber.tag(LogTag.MVI).d("Event: $event")

        when (event) {
            is RegisterUiEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email, error = null) }
            }

            is RegisterUiEvent.DisplayNameChanged -> {
                _uiState.update { it.copy(displayName = event.name, error = null) }
            }

            is RegisterUiEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.value, error = null) }
            }

            is RegisterUiEvent.ConfirmPasswordChanged -> {
                _uiState.update { it.copy(confirmPassword = event.value, error = null) }
            }

            RegisterUiEvent.SubmitSignUp -> {
                registerSubmitEventHandler.handleSubmitSignUp()
            }

            RegisterUiEvent.DismissCollisionDialog -> {
                registerSubmitEventHandler.handleDismissCollisionDialog()
            }
        }
    }
}
