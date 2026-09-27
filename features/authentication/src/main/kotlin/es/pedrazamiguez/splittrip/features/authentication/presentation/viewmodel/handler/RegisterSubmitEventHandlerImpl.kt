package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.EmailCollisionException
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.PasswordValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignUpWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.RegisterUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class RegisterSubmitEventHandlerImpl(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val emailValidationService: EmailValidationService,
    private val passwordValidationService: PasswordValidationService
) : RegisterSubmitEventHandler {

    private lateinit var _uiState: MutableStateFlow<RegisterUiState>
    private lateinit var _actions: Channel<RegisterUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<RegisterUiState>,
        actionsChannel: Channel<RegisterUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsChannel
        this.scope = scope
    }

    override fun handleDismissCollisionDialog() {
        _uiState.update { it.copy(showCollisionDialog = false) }
    }

    override fun handleSubmitSignUp() {
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

        if (!passwordValidationService.isValidPassword(password)) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_password_complexity)) }
            return
        }

        if (password != confirmPassword) {
            _uiState.update { it.copy(error = UiText.StringResource(R.string.register_error_passwords_do_not_match)) }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            signUpWithEmailUseCase(
                email = email,
                displayName = displayName,
                password = password
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    _actions.send(RegisterUiAction.RegisterSuccess)
                }
                .onFailure { e ->
                    Timber.e(e, "Sign-up failed")
                    if (e is EmailCollisionException) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                showCollisionDialog = true,
                                collisionEmail = email,
                                error = UiText.StringResource(R.string.register_error_collision)
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(R.string.register_error_failed),
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }
}
