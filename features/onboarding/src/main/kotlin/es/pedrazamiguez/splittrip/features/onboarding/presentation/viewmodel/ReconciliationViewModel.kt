package es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import es.pedrazamiguez.splittrip.features.onboarding.R
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.action.ReconciliationUiAction
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.event.ReconciliationUiEvent
import es.pedrazamiguez.splittrip.features.onboarding.presentation.viewmodel.state.ReconciliationUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ReconciliationViewModel(
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase,
    private val authenticationService: AuthenticationService
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReconciliationUiState>(ReconciliationUiState.WaitingForYou)
    val uiState: StateFlow<ReconciliationUiState> = _uiState.asStateFlow()

    private val _actions = Channel<ReconciliationUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    fun onEvent(event: ReconciliationUiEvent) {
        when (event) {
            ReconciliationUiEvent.MigrateData -> migrateData()
        }
    }

    private fun migrateData() {
        val email = authenticationService.currentUserEmail()
        val currentUserId = authenticationService.currentUserId()

        if (email == null || currentUserId == null) {
            viewModelScope.launch {
                _actions.send(
                    ReconciliationUiAction.ShowError(
                        UiText.StringResource(R.string.reconciliation_error_invalid_user)
                    )
                )
            }
            return
        }

        _uiState.value = ReconciliationUiState.Migrating

        viewModelScope.launch {
            reconcileUnregisteredUserUseCase(email, currentUserId)
                .onSuccess {
                    _uiState.value = ReconciliationUiState.Success
                    _actions.send(ReconciliationUiAction.NavigationToNext)
                }
                .onFailure { e ->
                    Timber.e(e, "Reconciliation migration failed manually")
                    _uiState.value = ReconciliationUiState.WaitingForYou
                    _actions.send(
                        ReconciliationUiAction.ShowError(
                            UiText.StringResource(R.string.reconciliation_error_migration_failed)
                        )
                    )
                }
        }
    }
}
