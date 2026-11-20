package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackFeatureGateBlocked
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackLimitGateBlocked
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
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
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.model.LoadedGroupCurrencies
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateEditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupImageEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupMemberEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupNavigationEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateEditGroupSubmitEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.buildUpdatedExtraCurrencies
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.formatEventForLogging
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.loadEditGuard
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.toggleExtraCurrencySelection
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.withInitialCreateState
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.support.withInitialEditState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    private val createEditGroupMemberEventHandler: CreateEditGroupMemberEventHandler,
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper,
    private val featureGateService: FeatureGateService,
    private val appConfigService: AppConfigService,
    private val authenticationService: AuthenticationService,
    private val getUserGroupsFlowUseCase: GetUserGroupsFlowUseCase,
    private val getGroupCurrenciesInUseUseCase: GetGroupCurrenciesInUseUseCase,
    private val getNonRemovableGroupMembersUseCase: GetNonRemovableGroupMembersUseCase,
    private val telemetryTracker: TelemetryTracker,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEditGroupUiState())
    val uiState: StateFlow<CreateEditGroupUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateEditGroupUiAction>()
    val actions: SharedFlow<CreateEditGroupUiAction> = _actions.asSharedFlow()

    private var isInitialized = false

    init {
        navigationEventHandler.bind(_uiState, _actions, viewModelScope)
        imageEventHandler.bind(_uiState, _actions, viewModelScope)
        submitEventHandler.bind(_uiState, _actions, viewModelScope)
        createEditGroupMemberEventHandler.bind(_uiState, _actions, viewModelScope)

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
                telemetryTracker.trackLimitGateBlocked(
                    limit = GatedLimit.MAX_OWNED_GROUPS_COUNT,
                    entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_CREATE_INIT,
                    upgradePrompted = true
                )
                _uiState.update {
                    it.copy(
                        isCreationBlocked = true,
                        proPreviewPrompt = ProFeaturePreviewPrompt(
                            previewType = ProFeaturePreviewType.UNLIMITED_GROUPS,
                            entrySource = MonetizationTelemetry.EntrySource.GROUP_CREATION
                        )
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
            is CreateEditGroupUiEvent.MemberSearchQueryChanged -> {
                createEditGroupMemberEventHandler.handleSearchQueryChanged(event.query)
            }
            is CreateEditGroupUiEvent.MemberSelected -> {
                createEditGroupMemberEventHandler.handleMemberSelected(event.user)
            }
            is CreateEditGroupUiEvent.MemberRemoved -> {
                createEditGroupMemberEventHandler.handleMemberRemoved(event.user)
            }
            is CreateEditGroupUiEvent.MemberScanned -> {
                createEditGroupMemberEventHandler.handleMemberScanned(event.userId, event.email)
            }
            is CreateEditGroupUiEvent.UnregisteredMemberDisplayNameChanged -> {
                createEditGroupMemberEventHandler.handleUnregisteredMemberDisplayNameChanged(
                    event.userId,
                    event.displayName
                )
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
                val source = _uiState.value.proPreviewPrompt?.entrySource
                    ?: MonetizationTelemetry.EntrySource.GROUP_CREATION
                _uiState.update { it.copy(proPreviewPrompt = null) }
                viewModelScope.launch {
                    _actions.emit(CreateEditGroupUiAction.NavigateToSubscriptions(source))
                }
            }
            is CreateEditGroupUiEvent.DismissProPreview -> {
                _uiState.update { it.copy(proPreviewPrompt = null) }
                if (_uiState.value.isCreationBlocked) {
                    viewModelScope.launch {
                        _actions.emit(CreateEditGroupUiAction.NavigateBack)
                    }
                }
            }
            is CreateEditGroupUiEvent.LockedCoverPhotoClicked -> {
                telemetryTracker.trackFeatureGateBlocked(
                    feature = GatedFeature.GROUP_COVER_UPLOAD,
                    entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_COVER_LOCKED_PREVIEW,
                    upgradePrompted = true
                )
                _uiState.update {
                    it.copy(
                        proPreviewPrompt = ProFeaturePreviewPrompt(
                            previewType = ProFeaturePreviewType.GROUP_COVER_PHOTO,
                            entrySource = MonetizationTelemetry.EntrySource.GROUP_COVER_UPLOAD
                        )
                    )
                }
            }
            is CreateEditGroupUiEvent.MemberCapacityUpsellClicked -> {
                createEditGroupMemberEventHandler.handleMemberCapacityUpsellClicked()
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
            val updatedExtras = toggleExtraCurrencySelection(code, current.availableCurrencies, current.extraCurrencies)
            current.copy(extraCurrencies = updatedExtras, error = null)
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

                LoadedGroupCurrencies(mappedCurrencies, defaultCurrency, group)
            }
        }.onSuccess { (mappedCurrencies, defaultCurrency, group) ->
            if (group != null) {
                applyEditState(group, mappedCurrencies)
            } else {
                _uiState.update { it.withInitialCreateState(mappedCurrencies, defaultCurrency) }
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
            it.withInitialEditState(
                group = group,
                mappedCurrencies = mappedCurrencies,
                memberUsers = memberUsers,
                currenciesInUse = currenciesInUse,
                nonRemovableMembers = nonRemovableMembers
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        imageEventHandler.cleanTempImages()
    }
}
