package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateGroupSubmitEventHandlerImpl(
    private val createGroupUseCase: CreateGroupUseCase,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val featureGateService: FeatureGateService,
    private val telemetryTracker: TelemetryTracker,
    private val appConfigService: AppConfigService
) : CreateGroupSubmitEventHandler {
    private lateinit var _uiState: MutableStateFlow<CreateGroupUiState>
    private lateinit var _actions: MutableSharedFlow<CreateGroupUiAction>
    private lateinit var scope: CoroutineScope

    override fun bind(
        stateFlow: MutableStateFlow<CreateGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateGroupUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun handleSubmit(onCreateGroupSuccess: () -> Unit) {
        if (_uiState.value.groupName.isBlank()) {
            _uiState.update {
                it.copy(isNameValid = false, error = UiText.StringResource(R.string.group_error_name_empty))
            }
            return
        }

        checkLimitsAndCreateGroup(onCreateGroupSuccess)
    }

    private fun checkLimitsAndCreateGroup(onCreateGroupSuccess: () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentGroups = getUserGroupsFlowUseCase().firstOrNull() ?: emptyList()
            featureGateService.checkLimit(GatedLimit.MAX_GROUPS_COUNT, currentGroups.size)
                .collect { limitResult ->
                    when (limitResult) {
                        is LimitResult.Allowed -> checkMemberLimitAndCreateGroup(onCreateGroupSuccess)
                        is LimitResult.Blocked -> handleGroupsLimitBlocked()
                    }
                }
        }
    }

    private suspend fun checkMemberLimitAndCreateGroup(onCreateGroupSuccess: () -> Unit) {
        val membersCount = _uiState.value.selectedMembers.size
        featureGateService.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, membersCount)
            .collect { memberLimitResult ->
                when (memberLimitResult) {
                    is LimitResult.Allowed -> createGroup(onCreateGroupSuccess)
                    is LimitResult.Blocked -> handleMembersLimitBlocked(memberLimitResult)
                }
            }
    }

    private suspend fun handleGroupsLimitBlocked() {
        val errorRes = UiText.StringResource(R.string.group_error_limit_groups_exceeded)
        _uiState.update { it.copy(isLoading = false, error = errorRes) }
        _actions.emit(CreateGroupUiAction.ShowError(errorRes))
    }

    private suspend fun handleMembersLimitBlocked(memberLimitResult: LimitResult.Blocked) {
        val errorRes = if (memberLimitResult.upgradeRequired) {
            UiText.StringResource(R.string.group_error_limit_members_exceeded)
        } else {
            UiText.StringResource(
                R.string.group_error_limit_members_registered_exceeded,
                appConfigService.maxMembersPerGroup.value
            )
        }
        _uiState.update { it.copy(isLoading = false, error = errorRes) }
        _actions.emit(CreateGroupUiAction.ShowError(errorRes))
    }

    private fun createGroup(onCreateGroupSuccess: () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val state = _uiState.value
            val groupName = state.groupName

            createGroupUseCase(
                Group(
                    name = groupName,
                    description = state.groupDescription,
                    currency = state.selectedCurrency?.code ?: appConfigService.defaultCurrencyCode.value,
                    extraCurrencies = state.extraCurrencies.map { it.code },
                    members = state.selectedMembers.map { it.userId },
                    mainImagePath = state.localGroupImagePath
                ),
                state.selectedMembers
            ).onSuccess {
                _uiState.update { it.copy(isLoading = false) }
                telemetryTracker.trackEvent(
                    "group_created",
                    mapOf("currency" to (state.selectedCurrency?.code ?: ""))
                )
                _actions.emit(
                    CreateGroupUiAction.ShowSuccess(UiText.StringResource(R.string.group_created_success, groupName))
                )
                onCreateGroupSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to create group")
                _uiState.update {
                    it.copy(isLoading = false, error = UiText.StringResource(R.string.group_error_creation_failed))
                }
                _actions.emit(
                    CreateGroupUiAction.ShowError(UiText.StringResource(R.string.group_error_creation_failed))
                )
            }
        }
    }
}
