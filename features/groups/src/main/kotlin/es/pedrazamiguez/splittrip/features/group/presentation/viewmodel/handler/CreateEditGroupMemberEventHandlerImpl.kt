package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackLimitGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.buildPendingUserSearchResults
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.mergeScannedMemberProfile
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.resolveIsCurrentUser
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.resolveMemberRemovalBlockReason
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.toUiText
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.updateMemberDisplayName
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateEditGroupMemberEventHandlerImpl(
    private val searchUsersByEmailUseCase: SearchUsersByEmailUseCase,
    private val emailValidationService: EmailValidationService,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val authenticationService: AuthenticationService,
    private val telemetryTracker: TelemetryTracker
) : CreateEditGroupMemberEventHandler {

    private lateinit var _uiState: MutableStateFlow<CreateEditGroupUiState>
    private lateinit var _actions: MutableSharedFlow<CreateEditGroupUiAction>
    private lateinit var scope: CoroutineScope
    private var memberSearchJob: Job? = null

    override fun bind(
        stateFlow: MutableStateFlow<CreateEditGroupUiState>,
        actionsFlow: MutableSharedFlow<CreateEditGroupUiAction>,
        scope: CoroutineScope
    ) {
        _uiState = stateFlow
        _actions = actionsFlow
        this.scope = scope
    }

    override fun handleSearchQueryChanged(query: String) {
        memberSearchJob?.cancel()

        if (query.length < MEMBER_SEARCH_MIN_QUERY_LENGTH || !emailValidationService.isValidEmail(query)) {
            _uiState.update { it.copy(memberSearchResults = persistentListOf(), isSearchingMembers = false) }
            return
        }

        memberSearchJob = scope.launch {
            delay(MEMBER_SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchingMembers = true) }

            searchUsersByEmailUseCase(query).onSuccess { users ->
                val selectedIds = _uiState.value.selectedMembers.map { it.userId }.toSet()
                val filteredUsers = users.filter { user ->
                    user.userId !in selectedIds && !isCurrentUser(user.userId, user.email)
                }
                val results = if (filteredUsers.isEmpty()) {
                    val currentUserEmail = _uiState.value.currentUserEmail ?: authenticationService.currentUserEmail()
                    buildPendingUserSearchResults(query, currentUserEmail, selectedIds)
                } else {
                    filteredUsers
                }
                _uiState.update {
                    it.copy(
                        memberSearchResults = results.toImmutableList(),
                        isSearchingMembers = false
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to search users by email")
                _uiState.update { it.copy(memberSearchResults = persistentListOf(), isSearchingMembers = false) }
            }
        }
    }

    override fun handleMemberSelected(user: User) {
        if (isCurrentUser(user.userId, user.email)) return

        val state = _uiState.value
        if (state.selectedMembers.any { it.userId == user.userId }) {
            return
        }
        if (state.selectedMembers.size >= state.maxMembersLimit) {
            handleMemberLimitExceeded(
                entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SELECT,
                clearSearchResults = true
            )
            return
        }
        _uiState.update {
            it.copy(
                selectedMembers = (it.selectedMembers + user).toImmutableList(),
                memberSearchResults = persistentListOf()
            )
        }
    }

    override fun handleMemberRemoved(user: User) {
        val state = _uiState.value
        val blockReason = resolveMemberRemovalBlockReason(
            isEditMode = state.isEditMode,
            selectedMembersCount = state.selectedMembers.size,
            nonRemovableMembers = state.nonRemovableMembers,
            userId = user.userId
        )
        if (blockReason != null) {
            scope.launch {
                _actions.emit(CreateEditGroupUiAction.ShowError(blockReason.toUiText()))
            }
            return
        }
        _uiState.update { current ->
            current.copy(
                selectedMembers = current.selectedMembers
                    .filter { it.userId != user.userId }
                    .toImmutableList(),
                error = null
            )
        }
    }

    override fun handleMemberScanned(userId: String, email: String) {
        if (isCurrentUser(userId, email)) {
            scope.launch {
                _actions.emit(
                    CreateEditGroupUiAction.ShowError(
                        UiText.StringResource(R.string.group_error_cannot_add_self)
                    )
                )
            }
            return
        }

        val state = _uiState.value
        if (state.selectedMembers.any { it.userId == userId }) return

        if (state.selectedMembers.size >= state.maxMembersLimit) {
            handleMemberLimitExceeded(
                entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_SCAN,
                clearSearchResults = false
            )
            return
        }

        val partialUser = User(
            userId = userId,
            email = email,
            displayName = null,
            profileImagePath = null,
            bio = null
        )
        _uiState.update {
            it.copy(
                selectedMembers = (it.selectedMembers + partialUser).toImmutableList()
            )
        }

        scope.launch {
            runCatching {
                getMemberProfilesUseCase(listOf(userId))
            }.onSuccess { profiles ->
                val fullUser = profiles[userId] ?: return@onSuccess
                _uiState.update { current ->
                    current.copy(selectedMembers = mergeScannedMemberProfile(current.selectedMembers, userId, fullUser))
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to load profile for scanned user $userId")
            }
        }
    }

    override fun handleUnregisteredMemberDisplayNameChanged(userId: String, displayName: String) {
        _uiState.update { state ->
            state.copy(
                selectedMembers = updateMemberDisplayName(
                    state.selectedMembers,
                    userId,
                    displayName
                )
            )
        }
    }

    override fun handleMemberCapacityUpsellClicked() {
        telemetryTracker.trackLimitGateBlocked(
            limit = GatedLimit.MAX_MEMBERS_PER_GROUP,
            entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_MEMBER_UPSELL,
            upgradePrompted = true
        )
        _uiState.update {
            it.copy(
                proPreviewPrompt = ProFeaturePreviewPrompt(
                    previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
                    entrySource = MonetizationTelemetry.EntrySource.GROUP_MEMBER_LIMIT
                )
            )
        }
    }

    private fun isCurrentUser(userId: String, email: String): Boolean = resolveIsCurrentUser(
        userId = userId,
        email = email,
        currentUserId = authenticationService.currentUserId(),
        currentUserEmail = _uiState.value.currentUserEmail ?: authenticationService.currentUserEmail()
    )

    private fun handleMemberLimitExceeded(
        entryPoint: String,
        clearSearchResults: Boolean
    ) {
        val state = _uiState.value
        val upgradePrompted = !state.isGroupPro
        telemetryTracker.trackLimitGateBlocked(
            limit = GatedLimit.MAX_MEMBERS_PER_GROUP,
            entryPoint = entryPoint,
            upgradePrompted = upgradePrompted
        )
        if (upgradePrompted) {
            _uiState.update {
                it.copy(
                    proPreviewPrompt = ProFeaturePreviewPrompt(
                        previewType = ProFeaturePreviewType.EXPANDED_GROUP_MEMBERS,
                        entrySource = MonetizationTelemetry.EntrySource.GROUP_MEMBER_LIMIT
                    ),
                    memberSearchResults = if (clearSearchResults) persistentListOf() else it.memberSearchResults
                )
            }
        } else {
            if (clearSearchResults) {
                _uiState.update { it.copy(memberSearchResults = persistentListOf()) }
            }
            scope.launch {
                _actions.emit(
                    CreateEditGroupUiAction.ShowError(
                        UiText.StringResource(R.string.group_error_limit_members_exceeded)
                    )
                )
            }
        }
    }

    companion object {
        private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
        private const val MEMBER_SEARCH_MIN_QUERY_LENGTH = 3
    }
}
