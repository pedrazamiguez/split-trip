package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.AuthenticationUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class AuthenticationCollisionHandlerImpl(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase
) : AuthenticationCollisionHandler {

    private lateinit var _uiState: MutableStateFlow<AuthenticationUiState>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<AuthenticationUiState>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        this.scope = scope
    }

    override fun handleCollisionPasswordChanged(value: String) {
        _uiState.update { it.copy(collisionPassword = value) }
    }

    override fun handleDismissCollisionDialog() {
        _uiState.update { it.copy(showCollisionDialog = false, pendingGoogleIdToken = null) }
    }

    override fun handleSubmitCollisionMerge(onLoginSuccess: () -> Unit) {
        val email = _uiState.value.collisionEmail
        val password = _uiState.value.collisionPassword
        val idToken = _uiState.value.pendingGoogleIdToken ?: return

        if (password.isEmpty()) {
            _uiState.update {
                it.copy(mergeError = UiText.StringResource(R.string.login_error_empty_password))
            }
            return
        }

        scope.launch {
            _uiState.update {
                it.copy(
                    isMerging = true,
                    mergeError = null
                )
            }

            signInWithEmailUseCase(email, password)
                .onSuccess {
                    linkGoogleAccountUseCase(idToken)
                        .onSuccess {
                            _uiState.update {
                                it.copy(
                                    isMerging = false,
                                    showCollisionDialog = false,
                                    pendingGoogleIdToken = null
                                )
                            }
                            onLoginSuccess()
                        }
                        .onFailure { linkError ->
                            Timber.e(linkError, "Failed to link Google account after merge sign-in")
                            _uiState.update {
                                it.copy(
                                    isMerging = false,
                                    mergeError = UiText.StringResource(R.string.login_error_merge_link_failed)
                                )
                            }
                        }
                }
                .onFailure { authError ->
                    Timber.e(authError, "Merge sign-in failed")
                    _uiState.update {
                        it.copy(
                            isMerging = false,
                            mergeError = UiText.StringResource(R.string.login_error_invalid_credentials)
                        )
                    }
                }
        }
    }
}
