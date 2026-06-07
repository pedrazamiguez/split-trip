package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import es.pedrazamiguez.splittrip.logging.LogTag
import es.pedrazamiguez.splittrip.logging.maskEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AuthenticationUiEvent, onLoginSuccess: () -> Unit) {
        val eventLog = when (event) {
            is AuthenticationUiEvent.EmailChanged -> "EmailChanged(email=${event.email.maskEmail()})"
            is AuthenticationUiEvent.PasswordChanged -> "PasswordChanged(password=***)"
            is AuthenticationUiEvent.GoogleSignInResult -> "GoogleSignInResult(idToken=***)"
            else -> event.toString()
        }
        Timber.tag(LogTag.MVI).d("Event: $eventLog")

        when (event) {
            is AuthenticationUiEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }

            is AuthenticationUiEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.password)
            }

            AuthenticationUiEvent.SubmitLogin -> {
                login(onLoginSuccess)
            }

            is AuthenticationUiEvent.GoogleSignInResult -> {
                loginWithGoogle(
                    idToken = event.idToken,
                    onLoginSuccess = onLoginSuccess
                )
            }

            AuthenticationUiEvent.GoogleSignInFailed -> {
                _uiState.value = _uiState.value.copy(
                    error = UiText.StringResource(R.string.login_google_error),
                    isGoogleLoading = false
                )
            }
        }
    }

    private fun login(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            signInWithEmailUseCase(
                _uiState.value.email,
                _uiState.value.password
            )
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onLoginSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        error = UiText.DynamicString(e.message ?: ""),
                        isLoading = false
                    )
                }
        }
    }

    private fun loginWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGoogleLoading = true,
                error = null
            )

            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isGoogleLoading = false)
                    onLoginSuccess()
                }
                .onFailure { e ->
                    Timber.e(e, "Google sign-in failed")
                    _uiState.value = _uiState.value.copy(
                        error = UiText.DynamicString(e.message ?: ""),
                        isGoogleLoading = false
                    )
                }
        }
    }
}
