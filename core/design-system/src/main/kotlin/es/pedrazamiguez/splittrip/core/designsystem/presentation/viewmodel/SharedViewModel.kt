package es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SharedViewModel(
    private val getSelectedGroupIdUseCase: GetSelectedGroupIdUseCase,
    private val getSelectedGroupNameUseCase: GetSelectedGroupNameUseCase,
    private val getSelectedGroupCurrencyUseCase: GetSelectedGroupCurrencyUseCase,
    private val setSelectedGroupUseCase: SetSelectedGroupUseCase,
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase
) : ViewModel() {

    val selectedGroup: StateFlow<Group?> = observeSelectedGroupUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val selectedGroupId: StateFlow<String?> = getSelectedGroupIdUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val selectedGroupName: StateFlow<String?> = getSelectedGroupNameUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val selectedGroupCurrency: StateFlow<String?> = getSelectedGroupCurrencyUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    fun selectGroup(groupId: String?, groupName: String?, currency: String? = null) {
        viewModelScope.launch {
            setSelectedGroupUseCase(groupId, groupName, currency)
        }
    }
}
