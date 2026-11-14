package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.domain.service.featuregate.LimitResult
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupCurrenciesInUseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetNonRemovableGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupImageEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupNavigationEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupSubmitEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.buildUpdatedExtraCurrencies
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.formatEventForLogging
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.loadEditGuard
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.toUiText
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("LongParameterList", "TooManyFunctions")
class CreateEditGroupViewModel(
    private val navigationEventHandler: CreateEditGroupNavigationEventHandler,
    private val imageEventHandler: CreateEditGroupImageEventHandler,
    private val submitEventHandler: CreateEditGroupSubmitEventHandler,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase,
    private val searchUsersByEmailUseCase: SearchUsersByEmailUseCase,
    private val emailValidationService: EmailValidationService,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper,
    private val featureGateService: FeatureGateService,
    private val appConfigService: AppConfigService,
    private val authenticationService: AuthenticationService,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val getGroupCurrenciesInUseUseCase: GetGroupCurrenciesInUseUseCase,
    private val getNonRemovableGroupMembersUseCase: GetNonRemovableGroupMembersUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditGroupUiState())
    val uiState: StateFlow<CreateEditGroupUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateEditGroupUiAction>()
    val actions: SharedFlow<CreateEditGroupUiAction> = _actions.asSharedFlow()

    private var memberSearchJob: Job? = null
    private var isInitialized = false

    init {
        navigationEventHandler.bind(_uiState, _actions, viewModelScope)
        imageEventHandler.bind(_uiState, _actions, viewModelScope)
        submitEventHandler.bind(_uiState, _actions, viewModelScope)

        imageEventHandler.cleanTempImages()
    }

    fun init(groupId: String?) {
        if (isInitialized) return
        isInitialized = true

        observeCoverUploadGate(groupId)

        if (groupId == null) {
            checkGroupCreationLimit()
            observeActingUserMemberLimit()
        } else {
            observeExistingGroupMemberLimit(groupId)
        }

        loadInitialUserData(groupId)
    }

    private fun observeCoverUploadGate(groupId: String?) {
        viewModelScope.launch {
            featureGateService.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD, groupId).collect { isEnabled ->
                _uiState.update { it.copy(isCoverUploadEnabled = isEnabled) }
            }
        }
    }

    private fun checkGroupCreationLimit() {
        viewModelScope.launch {
            val currentGroups = getUserGroupsFlowUseCase().firstOrNull() ?: emptyList()
            val currentUserId = authenticationService.currentUserId()
            val ownedGroupsCount = currentGroups.count { it.createdBy == currentUserId }
            val limitResult = featureGateService.checkLimit(
                limit = GatedLimit.MAX_OWNED_GROUPS_COUNT,
                currentCount = ownedGroupsCount
            ).firstOrNull()
            if (limitResult is LimitResult.Blocked && limitResult.upgradeRequired) {
                _uiState.update {
                    it.copy(
                        isCreationBlocked = true,
                        showUpgradeDialog = true,
                        upgradeDialogTitle = UiText.StringResource(DesignSystemR.string.upgrade_dialog_title),
                        upgradeDialogMessage = UiText.StringResource(R.string.group_error_limit_groups_exceeded)
                    )
                }
            }
        }
    }

    private fun observeActingUserMemberLimit() {
        viewModelScope.launch {
            featureGateService.isActingUserPro().collect { isPro ->
                _uiState.update {
                    it.copy(
                        isGroupPro = isPro,
                        maxMembersLimit = if (isPro) {
                            appConfigService.maxMembersPerGroupPro.value
                        } else {
                            appConfigService.maxMembersPerGroupFree.value
                        }
                    )
                }
            }
        }
    }

    private fun observeExistingGroupMemberLimit(groupId: String) {
        viewModelScope.launch {
            val maxFree = appConfigService.maxMembersPerGroupFree.value
            val maxPro = appConfigService.maxMembersPerGroupPro.value
            featureGateService.checkLimit(GatedLimit.MAX_MEMBERS_PER_GROUP, maxFree, groupId).collect { result ->
                val isPro = result is LimitResult.Allowed
                _uiState.update {
                    it.copy(
                        isGroupPro = isPro,
                        maxMembersLimit = if (isPro) maxPro else maxFree
                    )
                }
            }
        }
    }

    private fun loadInitialUserData(groupId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val email = authenticationService.currentUserEmail()
            if (email != null) {
                _uiState.update { it.copy(currentUserEmail = email) }
            }
            loadCurrencies(groupId)
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun onEvent(event: CreateEditGroupUiEvent, onSuccess: () -> Unit) {
        Timber.tag(LogTag.MVI).d("Event: ${formatEventForLogging(event)}")
        when (event) {
            is CreateEditGroupUiEvent.NameChanged -> _uiState.update { state ->
                state.copy(
                    groupName = event.name,
                    isNameValid = event.name.isNotBlank()
                )
            }
            is CreateEditGroupUiEvent.DescriptionChanged -> _uiState.update { state ->
                state.copy(groupDescription = event.description)
            }
            is CreateEditGroupUiEvent.CurrencySelected -> handleCurrencySelected(event.code)
            is CreateEditGroupUiEvent.ExtraCurrencyToggled -> handleExtraCurrencyToggled(event.code)
            is CreateEditGroupUiEvent.MemberSearchQueryChanged -> searchMembers(event.query)
            is CreateEditGroupUiEvent.MemberSelected -> handleMemberSelected(event)
            is CreateEditGroupUiEvent.MemberRemoved -> handleMemberRemoved(event)
            is CreateEditGroupUiEvent.MemberScanned -> handleMemberScanned(event.userId, event.email)
            is CreateEditGroupUiEvent.UnregisteredMemberDisplayNameChanged -> {
                _uiState.update { state ->
                    val updated = state.selectedMembers.map { user ->
                        if (user.userId == event.userId) {
                            user.copy(displayName = event.displayName.trim().takeIf { it.isNotBlank() })
                        } else {
                            user
                        }
                    }.toImmutableList()
                    state.copy(selectedMembers = updated)
                }
            }
            is CreateEditGroupUiEvent.Submit -> submitEventHandler.handleSubmit(onSuccess)
            is CreateEditGroupUiEvent.NextStep,
            is CreateEditGroupUiEvent.PreviousStep,
            is CreateEditGroupUiEvent.CloseWizard,
            is CreateEditGroupUiEvent.JumpToStep -> navigationEventHandler.handleNavigation(event)
            is CreateEditGroupUiEvent.GroupImagePicked -> {
                imageEventHandler.handleGroupImagePicked(event.uri)
            }
            is CreateEditGroupUiEvent.GroupImageRemoved -> {
                imageEventHandler.handleGroupImageRemoved()
            }
            is CreateEditGroupUiEvent.ShowImageSourceSheet -> imageEventHandler.handleShowImageSourceSheet(
                event.show
            )
            is CreateEditGroupUiEvent.UpgradeClicked -> {
                _uiState.update { it.copy(showUpgradeDialog = false) }
                viewModelScope.launch {
                    _actions.emit(CreateEditGroupUiAction.NavigateToSubscriptions)
                }
            }
            is CreateEditGroupUiEvent.DismissUpgradeDialog -> {
                _uiState.update { it.copy(showUpgradeDialog = false) }
                if (_uiState.value.isCreationBlocked) {
                    viewModelScope.launch {
                        _actions.emit(CreateEditGroupUiAction.NavigateBack)
                    }
                }
            }
            is CreateEditGroupUiEvent.LockedCoverPhotoClicked -> {
                viewModelScope.launch {
                    _actions.emit(
                        CreateEditGroupUiAction.ShowError(
                            UiText.StringResource(R.string.group_error_limit_cover_upload_disabled)
                        )
                    )
                    _actions.emit(CreateEditGroupUiAction.NavigateToSubscriptions)
                }
            }
            is CreateEditGroupUiEvent.MemberCapacityUpsellClicked -> {
                viewModelScope.launch {
                    _actions.emit(CreateEditGroupUiAction.NavigateToSubscriptions)
                }
            }
        }
    }

    private fun handleCurrencySelected(code: String) {
        _uiState.update { state ->
            val selected = state.availableCurrencies.find { it.code == code } ?: return@update state
            val updatedExtras = buildUpdatedExtraCurrencies(
                previousCurrency = state.selectedCurrency,
                newCode = code,
                currentExtras = state.extraCurrencies,
                currenciesInUse = state.currenciesInUse
            )
            state.copy(
                selectedCurrency = selected,
                extraCurrencies = updatedExtras,
                error = null
            )
        }
    }

    private fun handleExtraCurrencyToggled(code: String) {
        val state = _uiState.value
        val isDeselecting = state.extraCurrencies.any { it.code == code }
        if (isDeselecting && code in state.currenciesInUse) {
            viewModelScope.launch {
                _actions.emit(
                    CreateEditGroupUiAction.ShowError(
                        UiText.StringResource(R.string.group_remove_currency_error_in_use, code)
                    )
                )
            }
            return
        }

        _uiState.update { current ->
            val currentExtras = current.extraCurrencies
            val updatedExtras = if (currentExtras.any { it.code == code }) {
                currentExtras.filter { it.code != code }
            } else {
                val item = current.availableCurrencies.find { it.code == code } ?: return@update current
                currentExtras + item
            }.toImmutableList()
            current.copy(extraCurrencies = updatedExtras, error = null)
        }
    }

    private fun handleMemberSelected(event: CreateEditGroupUiEvent.MemberSelected) {
        if (isCurrentUser(event.user.userId, event.user.email)) return

        val state = _uiState.value
        if (state.selectedMembers.any { it.userId == event.user.userId }) {
            return
        }
        if (state.selectedMembers.size >= state.maxMembersLimit) {
            val errorRes = UiText.StringResource(R.string.group_error_limit_members_exceeded)
            if (!state.isGroupPro) {
                _uiState.update {
                    it.copy(
                        showUpgradeDialog = true,
                        upgradeDialogTitle = UiText.StringResource(DesignSystemR.string.upgrade_dialog_title),
                        upgradeDialogMessage = errorRes,
                        memberSearchResults = persistentListOf()
                    )
                }
            }
            viewModelScope.launch {
                _actions.emit(CreateEditGroupUiAction.ShowError(errorRes))
            }
            return
        }
        _uiState.update {
            it.copy(
                selectedMembers = (it.selectedMembers + event.user).toImmutableList(),
                memberSearchResults = persistentListOf()
            )
        }
    }

    private fun handleMemberRemoved(event: CreateEditGroupUiEvent.MemberRemoved) {
        val blockReason = _uiState.value.nonRemovableMembers[event.user.userId]
        if (blockReason != null) {
            viewModelScope.launch {
                _actions.emit(CreateEditGroupUiAction.ShowError(blockReason.toUiText()))
            }
            return
        }
        _uiState.update { state ->
            state.copy(
                selectedMembers = state.selectedMembers
                    .filter { it.userId != event.user.userId }
                    .toImmutableList(),
                error = null
            )
        }
    }

    private fun searchMembers(query: String) {
        memberSearchJob?.cancel()

        if (query.length < MEMBER_SEARCH_MIN_QUERY_LENGTH || !emailValidationService.isValidEmail(query)) {
            _uiState.update { it.copy(memberSearchResults = persistentListOf(), isSearchingMembers = false) }
            return
        }

        memberSearchJob = viewModelScope.launch {
            delay(MEMBER_SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearchingMembers = true) }

            searchUsersByEmailUseCase(query).onSuccess { users ->
                val selectedIds = _uiState.value.selectedMembers.map { it.userId }.toSet()
                val filteredUsers = users.filter { u ->
                    u.userId !in selectedIds && !isCurrentUser(u.userId, u.email)
                }
                val results = if (filteredUsers.isEmpty()) {
                    getPendingUserResults(query, selectedIds)
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

    private suspend fun loadCurrencies(groupId: String?) {
        _uiState.update { it.copy(isLoadingCurrencies = true) }

        runCatching {
            withContext(defaultDispatcher) {
                val userDefaultCurrency = getUserDefaultCurrencyUseCase().firstOrNull()
                    ?: appConfigService.defaultCurrencyCode.value

                val sortedCurrencies = getSupportedCurrenciesUseCase().getOrThrow()
                val mappedCurrencies = groupUiMapper.toCurrencyUiModels(sortedCurrencies)
                val defaultCurrency = mappedCurrencies.find { it.code == userDefaultCurrency }
                    ?: mappedCurrencies.firstOrNull()

                val group = groupId?.let { getGroupByIdUseCase(it) }

                Triple(mappedCurrencies, defaultCurrency, group)
            }
        }.onSuccess { (mappedCurrencies, defaultCurrency, group) ->
            if (group != null) {
                applyEditState(group, mappedCurrencies)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isEditMode = false,
                        availableCurrencies = mappedCurrencies,
                        selectedCurrency = it.selectedCurrency ?: defaultCurrency,
                        isLoadingCurrencies = false
                    ).let { updatedState ->
                        updatedState.copy(initialFormSnapshot = updatedState.toFormSnapshot())
                    }
                }
                if (groupId != null) {
                    _actions.emit(
                        CreateEditGroupUiAction.ShowError(
                            UiText.StringResource(R.string.group_detail_error_loading)
                        )
                    )
                    _actions.emit(CreateEditGroupUiAction.NavigateBack)
                }
            }
        }.onFailure { e ->
            _uiState.update { it.copy(isLoadingCurrencies = false, isLoading = false) }
            _actions.emit(
                CreateEditGroupUiAction.ShowError(UiText.StringResource(R.string.group_error_load_currencies))
            )
            Timber.e(e, "Failed to load currencies or group")
        }
    }

    private suspend fun applyEditState(
        group: Group,
        mappedCurrencies: ImmutableList<CurrencyUiModel>
    ) {
        submitEventHandler.setInitialGroup(group)
        imageEventHandler.setInitialGroup(group)

        val selectedCurrencyModel = mappedCurrencies.find { it.code == group.currency }
        val extraCurrencyModels = mappedCurrencies.filter { it.code in group.extraCurrencies }

        val memberProfiles = getMemberProfilesUseCase(group.members)
        val memberUsers = group.members.map { memberId ->
            memberProfiles[memberId] ?: User(userId = memberId, email = "")
        }

        val currenciesInUse = loadEditGuard(
            fallback = persistentSetOf(),
            errorMessage = "Failed to load currencies in use for group ${group.id}"
        ) {
            getGroupCurrenciesInUseUseCase(group.id).toImmutableSet()
        }

        val nonRemovableMembers = loadEditGuard(
            fallback = persistentMapOf(),
            errorMessage = "Failed to load non-removable members for group ${group.id}"
        ) {
            getNonRemovableGroupMembersUseCase(group.id).toImmutableMap()
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                isEditMode = true,
                groupId = group.id,
                groupName = group.name,
                groupDescription = group.description,
                availableCurrencies = mappedCurrencies,
                selectedCurrency = selectedCurrencyModel,
                extraCurrencies = extraCurrencyModels.toImmutableList(),
                currenciesInUse = currenciesInUse,
                selectedMembers = memberUsers.toImmutableList(),
                nonRemovableMembers = nonRemovableMembers,
                imageUrl = group.mainImagePath,
                localGroupImagePath = group.mainImagePath,
                isLoadingCurrencies = false
            ).let { updatedState ->
                updatedState.copy(initialFormSnapshot = updatedState.toFormSnapshot())
            }
        }
    }

    private fun handleMemberScanned(userId: String, email: String) {
        if (isCurrentUser(userId, email)) {
            viewModelScope.launch {
                _actions.emit(
                    CreateEditGroupUiAction.ShowError(
                        UiText.StringResource(R.string.group_error_cannot_add_self)
                    )
                )
            }
            return
        }

        val state = _uiState.value
        val alreadySelected = state.selectedMembers.any { it.userId == userId }
        if (alreadySelected) return

        if (state.selectedMembers.size >= state.maxMembersLimit) {
            val errorRes = UiText.StringResource(R.string.group_error_limit_members_exceeded)
            if (!state.isGroupPro) {
                _uiState.update {
                    it.copy(
                        showUpgradeDialog = true,
                        upgradeDialogTitle = UiText.StringResource(DesignSystemR.string.upgrade_dialog_title),
                        upgradeDialogMessage = errorRes
                    )
                }
            }
            viewModelScope.launch {
                _actions.emit(CreateEditGroupUiAction.ShowError(errorRes))
            }
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

        viewModelScope.launch {
            runCatching {
                getMemberProfilesUseCase(listOf(userId))
            }.onSuccess { profiles ->
                val fullUser = profiles[userId] ?: return@onSuccess
                _uiState.update { state ->
                    val updated = state.selectedMembers.map {
                        if (it.userId == userId) fullUser else it
                    }.toImmutableList()
                    state.copy(selectedMembers = updated)
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to load profile for scanned user $userId")
            }
        }
    }

    private fun isCurrentUser(userId: String, email: String): Boolean {
        val currentUserId = authenticationService.currentUserId()
        val currentUserEmail = _uiState.value.currentUserEmail ?: authenticationService.currentUserEmail()
        return (currentUserId != null && userId == currentUserId) ||
            (currentUserEmail != null && User.areEmailsEquivalent(email, currentUserEmail))
    }

    private fun getPendingUserResults(query: String, selectedIds: Set<String>): List<User> {
        val normalizedEmail = User.normalizeEmail(query)
        val currentUserEmail = _uiState.value.currentUserEmail ?: authenticationService.currentUserEmail()

        if (currentUserEmail != null && User.areEmailsEquivalent(query, currentUserEmail)) return emptyList()

        val pendingUserId = User.generatePendingUserId(normalizedEmail)
        return if (pendingUserId !in selectedIds) {
            listOf(User(userId = pendingUserId, email = normalizedEmail, isPending = true))
        } else {
            emptyList()
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageEventHandler.cleanTempImages()
    }

    companion object {
        private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
        private const val MEMBER_SEARCH_MIN_QUERY_LENGTH = 3
    }
}
