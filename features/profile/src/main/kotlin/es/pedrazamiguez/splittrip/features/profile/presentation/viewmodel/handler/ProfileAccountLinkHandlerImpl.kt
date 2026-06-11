package es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AuthProviderType
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.auth.UnlinkProviderUseCase
import es.pedrazamiguez.splittrip.features.profile.R
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.action.ProfileUiAction
import es.pedrazamiguez.splittrip.features.profile.presentation.viewmodel.state.ProfileUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class ProfileAccountLinkHandlerImpl(
    private val linkGoogleAccountUseCase: LinkGoogleAccountUseCase,
    private val linkEmailPasswordUseCase: LinkEmailPasswordUseCase,
    private val unlinkProviderUseCase: UnlinkProviderUseCase
) : ProfileAccountLinkHandler {

    private lateinit var _uiState: MutableStateFlow<ProfileUiState>
    private lateinit var _actions: Channel<ProfileUiAction>
    private lateinit var scope: CoroutineScope
    private lateinit var loadProfile: () -> Unit

    override fun bind(
        stateFlow: MutableStateFlow<ProfileUiState>,
        actionsChannel: Channel<ProfileUiAction>,
        scope: CoroutineScope,
        loadProfile: () -> Unit
    ) {
        _uiState = stateFlow
        _actions = actionsChannel
        this.scope = scope
        this.loadProfile = loadProfile
    }

    override fun handleLinkGoogleAccount(idToken: String) {
        scope.launch {
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
                    _actions.send(ProfileUiAction.ShowError(UiText.StringResource(R.string.profile_link_error_failed)))
                }
        }
    }

    override fun handleShowLinkEmailDialog() {
        _uiState.update {
            it.copy(
                showLinkEmailDialog = true,
                linkPasswordInput = "",
                linkConfirmPasswordInput = "",
                linkPasswordError = null
            )
        }
    }

    override fun handleDismissLinkEmailDialog() {
        _uiState.update { it.copy(showLinkEmailDialog = false) }
    }

    override fun handleLinkPasswordChanged(value: String) {
        _uiState.update { it.copy(linkPasswordInput = value) }
    }

    override fun handleLinkConfirmPasswordChanged(value: String) {
        _uiState.update { it.copy(linkConfirmPasswordInput = value) }
    }

    override fun handleSubmitLinkEmailPassword() {
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

        scope.launch {
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
                            linkPasswordError = UiText.StringResource(R.string.profile_link_error_failed)
                        )
                    }
                }
        }
    }

    override fun handleUnlinkProvider(providerType: AuthProviderType) {
        if (_uiState.value.linkedProviders.size <= 1) {
            scope.launch {
                _actions.send(
                    ProfileUiAction.ShowError(UiText.StringResource(R.string.profile_unlink_error_last_provider))
                )
            }
            return
        }

        scope.launch {
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
                    _actions.send(
                        ProfileUiAction.ShowError(UiText.StringResource(R.string.profile_unlink_error_failed))
                    )
                }
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
