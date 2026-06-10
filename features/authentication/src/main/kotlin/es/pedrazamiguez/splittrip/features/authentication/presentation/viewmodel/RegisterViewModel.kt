package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class RegisterViewModel(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val emailValidationService: EmailValidationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val _actions = Channel<RegisterUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    fun onEvent(event: RegisterUiEvent, onRegisterSuccess: () -> Unit) {
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
                submitSignUp(onRegisterSuccess)
            }
        }
    }

    private fun submitSignUp(onRegisterSuccess: () -> Unit) {
        val email = _uiState.value.email.trim()
        val displayName = _uiState.value.displayName.trim()
        val password = _uiState.value.password
        val confirmPassword = _uiState.value.confirmPassword

        if (displayName.isEmpty()) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_empty_display_name)) }
            return
        }

        if (!emailValidationService.isValidEmail(email)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_invalid_email)) }
            return
        }

        if (password.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_password_too_short)) }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_passwords_do_not_match)) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpWithEmailUseCase(
                email = email,
                displayName = displayName,
                password = password
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onRegisterSuccess()
                }
                .onFailure { e ->
                    Timber.e(e, "Sign-up failed")
                    _uiState.update {
                        it.copy(
                            error = UiText.DynamicString(e.message ?: "Sign-up failed"),
                            isLoading = false
                        )
                    }
                }
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
