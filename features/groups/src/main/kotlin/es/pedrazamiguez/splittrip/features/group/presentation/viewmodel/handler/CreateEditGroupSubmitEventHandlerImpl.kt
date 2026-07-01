package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.exception.CannotRemoveMemberException
import es.pedrazamiguez.splittrip.domain.exception.GroupArchivedException
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.group.AddGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateEditGroupSubmitEventHandlerImpl(
    private val createGroupUseCase: CreateGroupUseCase,
    private val updateGroupUseCase: UpdateGroupUseCase,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val featureGateService: FeatureGateService,
    private val telemetryTracker: TelemetryTracker,
    private val appConfigService: AppConfigService,
    private val addGroupMembersUseCase: AddGroupMembersUseCase,
    private val removeGroupMemberUseCase: RemoveGroupMemberUseCase
) : CreateEditGroupSubmitEventHandler {
    private lateinit var _uiState: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var _actions: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var scope: CoroutineScope
    private var initialGroup: Group? = null

    override fun bind(
        stateFlow: MutableStateFlow<CreateEditGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun setInitialGroup(group: Group) {
        this.initialGroup = group
    }

    override fun handleSubmit(onSuccess: () -> Unit) {
        if (_uiState.value.groupName.isBlank()) {
            _uiState.update {
                it.copy(isNameValid = false, error = UiText.StringResource(R.string.group_error_name_empty))
            }
            return
        }

        if (_uiState.value.isEditMode) {
            updateGroup(onSuccess)
        } else {
            checkLimitsAndCreateGroup(onSuccess)
        }
    }

    private fun updateGroup(onSuccess: () -> Unit) {
        val group = initialGroup ?: return
        val state = _uiState.value
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val currentMemberIds = state.selectedMembers.map { it.userId }
            val membersToAdd = state.selectedMembers.filter { it.userId !in group.members }
            val membersToRemove = group.members.filter { it !in currentMemberIds }

            val updatedGroup = group.copy(
                name = state.groupName.trim(),
                description = state.groupDescription.trim(),
                currency = state.selectedCurrency?.code ?: "EUR",
                extraCurrencies = state.extraCurrencies.map { it.code },
                mainImagePath = state.localGroupImagePath
            )

            updateGroupUseCase(updatedGroup)
                .onSuccess {
                    val hasError = syncMemberChanges(group, membersToAdd, membersToRemove)
                    emitGroupUpdateResult(hasError)
                    onSuccess()
                }
                .onFailure { e ->
                    emitGroupUpdateFailure(e)
                }
        }
    }

    private suspend fun syncMemberChanges(
        group: Group,
        membersToAdd: List<es.pedrazamiguez.splittrip.domain.model.User>,
        membersToRemove: List<String>
    ): Boolean {
        var hasError = false
        if (membersToAdd.isNotEmpty()) {
            addGroupMembersUseCase(group.id, membersToAdd)
                .onFailure { e ->
                    Timber.e(e, "Failed to add members to group ${group.id}")
                    hasError = true
                }
        }
        if (membersToRemove.isNotEmpty()) {
            for (userId in membersToRemove) {
                removeGroupMemberUseCase(group.id, userId)
                    .onFailure { e ->
                        Timber.e(e, "Failed to remove member $userId from group ${group.id}")
                        hasError = true
                        val message = when {
                            e is CannotRemoveMemberException && e.message?.contains("non_zero_balance") == true ->
                                UiText.StringResource(R.string.group_remove_member_error_balance)
                            e is CannotRemoveMemberException && e.message?.contains("is_creator") == true ->
                                UiText.StringResource(R.string.group_remove_member_error_admin)
                            else -> UiText.StringResource(R.string.group_error_remove_member_failed)
                        }
                        _actions.emit(CreateEditGroupUiAction.ShowError(message))
                    }
            }
        }
        return hasError
    }

    private suspend fun emitGroupUpdateResult(hasError: Boolean) {
        _uiState.update { it.copy(isLoading = false) }
        val message = if (hasError) {
            UiText.StringResource(R.string.group_edit_success_saved_with_errors)
        } else {
            UiText.StringResource(R.string.group_edit_success_saved)
        }
        _actions.emit(CreateEditGroupUiAction.ShowSuccess(message))
    }

    private suspend fun emitGroupUpdateFailure(e: Throwable) {
        Timber.e(e, "Failed to save group details")
        val errorMessage = when (e) {
            is GroupArchivedException -> UiText.StringResource(DesignSystemR.string.group_error_archived)
            else -> UiText.StringResource(R.string.group_error_creation_failed)
        }
        _uiState.update {
            it.copy(
                isLoading = false,
                error = errorMessage
            )
        }
        _actions.emit(
            CreateEditGroupUiAction.ShowError(errorMessage)
        )
    }

    private fun checkLimitsAndCreateGroup(onSuccess: () -> Unit) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentGroups = getUserGroupsFlowUseCase().firstOrNull() ?: emptyList()
            featureGateService.checkLimit(GatedLimit.MAX_GROUPS_COUNT, currentGroups.size)
                .collect { limitResult ->
                    when (limitResult) {
                        is LimitResult.Allowed -> checkMemberLimitAndCreateGroup(onSuccess)
                        is LimitResult.Blocked -> handleGroupsLimitBlocked()
                    }
                }
        }
    }

    private suspend fun checkMemberLimitAndCreateGroup(onSuccess: () -> Unit) {
        val membersCount = _uiState.value.selectedMembers.size
        featureGateService.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, membersCount)
            .collect { memberLimitResult ->
                when (memberLimitResult) {
                    is LimitResult.Allowed -> createGroup(onSuccess)
                    is LimitResult.Blocked -> handleMembersLimitBlocked(memberLimitResult)
                }
            }
    }

    private suspend fun handleGroupsLimitBlocked() {
        val errorRes = UiText.StringResource(R.string.group_error_limit_groups_exceeded)
        _uiState.update { it.copy(isLoading = false, error = errorRes) }
        _actions.emit(CreateEditGroupUiAction.ShowError(errorRes))
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
        _actions.emit(CreateEditGroupUiAction.ShowError(errorRes))
    }

    private fun createGroup(onSuccess: () -> Unit) {
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
                    CreateEditGroupUiAction.ShowSuccess(
                        UiText.StringResource(R.string.group_created_success, groupName)
                    )
                )
                onSuccess()
            }.onFailure { e ->
                Timber.e(e, "Failed to create group")
                _uiState.update {
                    it.copy(isLoading = false, error = UiText.StringResource(R.string.group_error_creation_failed))
                }
                _actions.emit(
                    CreateEditGroupUiAction.ShowError(UiText.StringResource(R.string.group_error_creation_failed))
                )
            }
        }
    }
}
