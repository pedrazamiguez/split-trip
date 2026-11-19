package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.network.NetworkMonitor
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.features.settlement.R
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.action.DisputeSettlementUiAction
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.event.DisputeSettlementUiEvent
import es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state.DisputeSettlementUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class DisputeSettlementViewModel(
    private val disputeSettlementUseCase: DisputeSettlementUseCase,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private var groupId: String = ""
    private var settlementId: String = ""

    private val _localState = MutableStateFlow(DisputeSettlementUiState())
    private val _actions = Channel<DisputeSettlementUiAction>(Channel.BUFFERED)
    val actions = _actions.receiveAsFlow()

    val uiState: StateFlow<DisputeSettlementUiState> = combine(
        _localState,
        networkMonitor.isOnline
    ) { localState, isOnline ->
        localState.copy(isOffline = !isOnline)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = DisputeSettlementUiState()
    )

    fun setContext(groupId: String, settlementId: String) {
        this.groupId = groupId
        this.settlementId = settlementId
    }

    fun onEvent(event: DisputeSettlementUiEvent) {
        when (event) {
            is DisputeSettlementUiEvent.ReasonChanged -> {
                _localState.update { it.copy(reason = event.reason) }
            }
            DisputeSettlementUiEvent.SubmitClicked -> handleSubmit()
        }
    }

    private fun handleSubmit() {
        if (uiState.value.isSubmitting) return
        if (uiState.value.isOffline) {
            viewModelScope.launch {
                _actions.send(
                    DisputeSettlementUiAction.ShowError(
                        UiText.StringResource(R.string.your_balance_offline_warning)
                    )
                )
            }
            return
        }

        val trimmedReason = _localState.value.reason.trim()
        val settlementIds = settlementId.split(",").filter { it.isNotBlank() }
        if (groupId.isBlank() || settlementIds.isEmpty() || trimmedReason.isBlank()) return

        _localState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            var hasError = false
            for (id in settlementIds) {
                disputeSettlementUseCase(
                    groupId = groupId,
                    settlementId = id,
                    reason = trimmedReason
                ).fold(
                    onSuccess = {},
                    onFailure = { e ->
                        Timber.w(e, "Failed to request review for settlement $id")
                        hasError = true
                    }
                )
            }

            _localState.update { it.copy(isSubmitting = false) }
            if (hasError) {
                _actions.send(
                    DisputeSettlementUiAction.ShowError(
                        UiText.StringResource(R.string.your_balance_dispute_error)
                    )
                )
            } else {
                _actions.send(
                    DisputeSettlementUiAction.ShowSuccess(
                        UiText.StringResource(R.string.your_balance_dispute_success)
                    )
                )
                _actions.send(DisputeSettlementUiAction.NavigateBack)
            }
        }
    }
}
