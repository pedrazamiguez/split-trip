package es.pedrazamiguez.splittrip.features.authentication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase
import es.pedrazamiguez.splittrip.features.authentication.R
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiAction
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiEvent
import es.pedrazamiguez.splittrip.features.authentication.presentation.model.ForgotPasswordUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ForgotPasswordViewModel(
    private val sendPasswordResetEmailUseCase: SendPasswordResetEmailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private val _uiAction = Channel<ForgotPasswordUiAction>()
    val uiAction = _uiAction.receiveAsFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        Timber.tag(LogTag.MVI).d("Event: $event")
        when (event) {
            is ForgotPasswordUiEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(
                    email = event.email,
                    emailError = null,
                    generalError = null
                )
            }
            ForgotPasswordUiEvent.Submit -> {
                submitResetRequest()
            }
            ForgotPasswordUiEvent.BackClicked -> {
                viewModelScope.launch {
                    _uiAction.send(ForgotPasswordUiAction.NavigateBack)
                }
            }
        }
    }

    private fun submitResetRequest() {
        val email = _uiState.value.email.trim()
        if (email.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                emailError = UiText.StringResource(R.string.register_error_invalid_email)
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                emailError = null,
                generalError = null
            )

            sendPasswordResetEmailUseCase(email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = UiText.DynamicString(e.message ?: "")
                    )
                }
        }
    }
}
