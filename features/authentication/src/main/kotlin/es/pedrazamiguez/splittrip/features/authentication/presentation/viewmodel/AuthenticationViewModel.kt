package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInAnonymouslyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler.AuthenticationCollisionEventHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInAnonymouslyUseCase: SignInAnonymouslyUseCase,
    private val authenticationCollisionEventHandler: AuthenticationCollisionEventHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        authenticationCollisionEventHandler.bind(_uiState, viewModelScope)
    }

    fun onEvent(event: AuthenticationUiEvent, onLoginSuccess: () -> Unit) {
        Timber.tag(LogTag.MVI).d("Event: $event")

        when (event) {
            is AuthenticationUiEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email) }
            }

            is AuthenticationUiEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.value) }
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
                _uiState.update {
                    it.copy(
                        error = UiText.StringResource(R.string.login_google_error),
                        isGoogleLoading = false
                    )
                }
            }

            is AuthenticationUiEvent.CollisionPasswordChanged -> {
                authenticationCollisionEventHandler.handleCollisionPasswordChanged(event.value)
            }

            AuthenticationUiEvent.SubmitCollisionMerge -> {
                authenticationCollisionEventHandler.handleSubmitCollisionMerge(onLoginSuccess)
            }

            AuthenticationUiEvent.DismissCollisionDialog -> {
                authenticationCollisionEventHandler.handleDismissCollisionDialog()
            }

            AuthenticationUiEvent.ContinueAsGuest -> {
                loginAnonymously(onLoginSuccess)
            }
        }
    }

    private fun login(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            signInWithEmailUseCase(
                _uiState.value.email,
                _uiState.value.password
            )
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                    onLoginSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.DynamicString(e.message ?: ""),
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun loginWithGoogle(idToken: String, onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGoogleLoading = true,
                    error = null
                )
            }

            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _uiState.update { it.copy(isGoogleLoading = false) }
                    onLoginSuccess()
                }
                .onFailure { e ->
                    if (e is GoogleCollisionWithEmailPasswordException) {
                        _uiState.update {
                            it.copy(
                                showCollisionDialog = true,
                                collisionEmail = e.email,
                                pendingGoogleIdToken = e.idToken,
                                isGoogleLoading = false,
                                collisionPassword = "",
                                mergeError = null
                            )
                        }
                    } else {
                        Timber.e(e, "Google sign-in failed")
                        _uiState.update {
                            it.copy(
                                error = UiText.StringResource(R.string.login_google_error),
                                isGoogleLoading = false
                            )
                        }
                    }
                }
        }
    }

    private fun loginAnonymously(onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGuestLoading = true,
                    error = null
                )
            }
            signInAnonymouslyUseCase()
                .onSuccess {
                    _uiState.update { it.copy(isGuestLoading = false) }
                    onLoginSuccess()
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            error = UiText.DynamicString(e.message ?: "Guest login failed"),
                            isGuestLoading = false
                        )
                    }
                }
        }
    }
}
