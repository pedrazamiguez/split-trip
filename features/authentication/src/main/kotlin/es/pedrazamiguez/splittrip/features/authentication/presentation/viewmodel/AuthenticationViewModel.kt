package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.exception.GoogleCollisionWithEmailPasswordException
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithGoogleUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationViewModel(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthenticationUiState())
    val uiState = _uiState.asStateFlow()

    fun onEvent(event: AuthenticationUiEvent, onLoginSuccess: () -> Unit) {
        Timber.tag(LogTag.MVI).d("Event: $event")

        when (event) {
            is AuthenticationUiEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.email)
            }

            is AuthenticationUiEvent.PasswordChanged -> {
                _uiState.value = _uiState.value.copy(password = event.value)
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

            is AuthenticationUiEvent.CollisionPasswordChanged -> {
                _uiState.value = _uiState.value.copy(collisionPassword = event.value)
            }

            AuthenticationUiEvent.SubmitCollisionMerge -> {
                submitCollisionMerge(onLoginSuccess)
            }

            AuthenticationUiEvent.DismissCollisionDialog -> {
                _uiState.value = _uiState.value.copy(showCollisionDialog = false, pendingGoogleIdToken = null)
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
                    if (e is GoogleCollisionWithEmailPasswordException) {
                        _uiState.value = _uiState.value.copy(
                            showCollisionDialog = true,
                            collisionEmail = e.email,
                            pendingGoogleIdToken = e.idToken,
                            isGoogleLoading = false,
                            collisionPassword = "",
                            mergeError = null
                        )
                    } else {
                        Timber.e(e, "Google sign-in failed")
                        _uiState.value = _uiState.value.copy(
                            error = UiText.DynamicString(e.message ?: ""),
                            isGoogleLoading = false
                        )
                    }
                }
        }
    }

    private fun submitCollisionMerge(onLoginSuccess: () -> Unit) {
        val email = _uiState.value.collisionEmail
        val password = _uiState.value.collisionPassword
        val idToken = _uiState.value.pendingGoogleIdToken ?: return

        if (password.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                mergeError = UiText.StringResource(R.string.login_error_empty_password)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isMerging = true,
                mergeError = null
            )

            signInWithEmailUseCase(email, password)
                .onSuccess {
                    linkGoogleAccountUseCase(idToken)
                        .onSuccess {
                            _uiState.value = _uiState.value.copy(
                                isMerging = false,
                                showCollisionDialog = false,
                                pendingGoogleIdToken = null
                            )
                            onLoginSuccess()
                        }
                        .onFailure { linkError ->
                            Timber.e(linkError, "Failed to link Google account after merge sign-in")
                            _uiState.value = _uiState.value.copy(
                                isMerging = false,
                                mergeError = UiText.StringResource(R.string.login_error_merge_link_failed)
                            )
                        }
                }
                .onFailure { authError ->
                    Timber.e(authError, "Merge sign-in failed")
                    _uiState.value = _uiState.value.copy(
                        isMerging = false,
                        mergeError = UiText.StringResource(R.string.login_error_invalid_credentials)
                    )
                }
        }
    }
}
