package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface CreateEditGroupNavigationEventHandler {
    fun bind(
        stateFlow: MutableStateFlow<CreateEditGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>,
        scope: CoroutineScope
    )
    fun handleNavigation(event: CreateEditGroupUiEvent)
}
