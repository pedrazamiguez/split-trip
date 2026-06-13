package es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel

import androidx.lifecycle.ViewModel
import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.action.ActivityLoggingUiAction
import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.event.ActivityLoggingUiEvent
import es.pedrazamiguez.splittrip.features.activitylog.presentation.viewmodel.state.ActivityLoggingUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ActivityLoggingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityLoggingUiState())
    val uiState: StateFlow<ActivityLoggingUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<ActivityLoggingUiAction>()
    val actions: SharedFlow<ActivityLoggingUiAction> = _actions.asSharedFlow()

    fun onEvent(event: ActivityLoggingUiEvent) {
        when (event) {
            ActivityLoggingUiEvent.Refresh -> { /* Stub */ }
        }
    }
}
