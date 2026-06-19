package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface CreateGroupImageEventHandler {

    fun bind(
        stateFlow: MutableStateFlow<CreateGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateGroupUiAction>,
        scope: CoroutineScope
    )

    fun handleGroupImagePicked(uri: String)

    fun handleGroupImageRemoved()

    fun handleShowImageSourceSheet(show: Boolean)

    fun cleanTempImages()
}
