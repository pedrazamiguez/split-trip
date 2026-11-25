package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetReceiptExtractionCapabilityUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObservePlanAllowancesUseCase
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.PlanAllowancesUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.PlanAllowancesUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.PlanAllowancesUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.PlanAllowancesUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanAllowancesViewModel(
    observePlanAllowancesUseCase: ObservePlanAllowancesUseCase,
    private val getReceiptExtractionCapabilityUseCase: GetReceiptExtractionCapabilityUseCase,
    private val planAllowancesUiMapper: PlanAllowancesUiMapper
) : ViewModel() {

    val uiState: StateFlow<PlanAllowancesUiState> = observePlanAllowancesUseCase()
        .map { allowances ->
            planAllowancesUiMapper.map(
                allowances = allowances,
                extractionCapability = getReceiptExtractionCapabilityUseCase()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = PlanAllowancesUiState(isLoading = true)
        )

    private val _actions = Channel<PlanAllowancesUiAction>(Channel.BUFFERED)
    val actions: Flow<PlanAllowancesUiAction> = _actions.receiveAsFlow()

    fun onEvent(event: PlanAllowancesUiEvent) {
        when (event) {
            PlanAllowancesUiEvent.RequestUpgrade -> {
                viewModelScope.launch {
                    _actions.send(PlanAllowancesUiAction.NavigateToUpgrade)
                }
            }
        }
    }
}
