package es.pedrazamiguez.splittrip.core.designsystem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetSelectedGroupNameUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetSelectedGroupUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModel(
    private val getSelectedGroupIdUseCase: GetSelectedGroupIdUseCase,
    private val getSelectedGroupNameUseCase: GetSelectedGroupNameUseCase,
    private val getSelectedGroupCurrencyUseCase: GetSelectedGroupCurrencyUseCase,
    private val setSelectedGroupUseCase: SetSelectedGroupUseCase,
    private val observeSelectedGroupUseCase: ObserveSelectedGroupUseCase,
    private val observeGroupUseCase: ObserveGroupUseCase
) : ViewModel() {

    val selectedGroup: StateFlow<Group?> = observeSelectedGroupUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val isInitialLoadComplete: StateFlow<Boolean> = observeSelectedGroupUseCase()
        .map { true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
                replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
            ),
            initialValue = false
        )

    val selectedGroupId: StateFlow<String?> = getSelectedGroupIdUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val selectedGroupName: StateFlow<String?> = combine(
        selectedGroup,
        getSelectedGroupNameUseCase()
    ) { group, prefName ->
        group?.name ?: prefName
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    val selectedGroupCurrency: StateFlow<String?> = combine(
        selectedGroup,
        getSelectedGroupCurrencyUseCase()
    ) { group, prefCurrency ->
        group?.currency ?: prefCurrency
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = AppConstants.FLOW_RETENTION_TIME,
            replayExpirationMillis = AppConstants.FLOW_REPLAY_EXPIRATION
        ),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            observeSelectedGroupUseCase().collect { group ->
                if (group == null && selectedGroupId.value != null) {
                    selectGroup(null, null, null)
                }
            }
        }

        viewModelScope.launch {
            getSelectedGroupIdUseCase()
                .flatMapLatest { storedId ->
                    if (storedId == null) {
                        flowOf(false)
                    } else {
                        observeGroupUseCase(storedId).map { resolvedGroup ->
                            resolvedGroup == null
                        }
                    }
                }
                .collect { isStale ->
                    if (isStale) {
                        // Stored ID no longer resolves to a known group — clear it
                        setSelectedGroupUseCase(null, null, null)
                    }
                }
        }
    }

    fun selectGroup(groupId: String?, groupName: String?, currency: String? = null) {
        viewModelScope.launch {
            setSelectedGroupUseCase(groupId, groupName, currency)
        }
    }
}
