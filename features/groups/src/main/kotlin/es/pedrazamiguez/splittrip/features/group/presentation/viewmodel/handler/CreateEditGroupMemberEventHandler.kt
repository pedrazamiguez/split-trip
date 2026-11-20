package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

interface CreateEditGroupMemberEventHandler {
    fun bind(
        stateFlow: MutableStateFlow<CreateEditGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>,
        scope: CoroutineScope
    )
    fun handleSearchQueryChanged(query: String)
    fun handleMemberSelected(user: User)
    fun handleMemberRemoved(user: User)
    fun handleMemberScanned(userId: String, email: String)
    fun handleUnregisteredMemberDisplayNameChanged(userId: String, displayName: String)
    fun handleMemberCapacityUpsellClicked()
}
