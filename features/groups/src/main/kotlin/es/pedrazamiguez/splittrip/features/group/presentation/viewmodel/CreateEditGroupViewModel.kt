package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
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
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
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

@Suppress("LongParameterList")
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

        viewModelScope.launch {
            featureGateService.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).collect { isEnabled ->
                _uiState.update { it.copy(isCoverUploadEnabled = isEnabled) }
            }
        }
    }

    fun init(groupId: String?) {
        if (isInitialized) return
        isInitialized = true

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            loadCurrencies(groupId)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: CreateEditGroupUiEvent, onSuccess: () -> Unit) {
        Timber.tag(LogTag.MVI).d("Event: ${formatEventForLogging(event)}")
        when (event) {
            is CreateEditGroupUiEvent.NameChanged -> _uiState.update { state ->
                state.copy(groupName = event.name, isNameValid = event.name.isNotBlank())
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
            is CreateEditGroupUiEvent.JumpToStep -> navigationEventHandler.handleNavigation(event)
            is CreateEditGroupUiEvent.GroupImagePicked -> imageEventHandler.handleGroupImagePicked(event.uri)
            is CreateEditGroupUiEvent.GroupImageRemoved -> imageEventHandler.handleGroupImageRemoved()
            is CreateEditGroupUiEvent.ShowImageSourceSheet -> imageEventHandler.handleShowImageSourceSheet(
                event.show
            )
        }
    }

    private fun handleCurrencySelected(code: String) {
        _uiState.update { state ->
            val selected = state.availableCurrencies.find { it.code == code } ?: return
            val updatedExtras = state.extraCurrencies
                .filter { it.code != code }
                .toImmutableList()
            state.copy(selectedCurrency = selected, extraCurrencies = updatedExtras)
        }
    }

    private fun handleExtraCurrencyToggled(code: String) {
        _uiState.update { state ->
            val currentExtras = state.extraCurrencies
            val updatedExtras = if (currentExtras.any { it.code == code }) {
                currentExtras.filter { it.code != code }
            } else {
                val item = state.availableCurrencies.find { it.code == code } ?: return
                currentExtras + item
            }.toImmutableList()
            state.copy(extraCurrencies = updatedExtras)
        }
    }

    private fun handleMemberSelected(event: CreateEditGroupUiEvent.MemberSelected) {
        _uiState.update { state ->
            if (state.selectedMembers.any { it.userId == event.user.userId }) {
                state
            } else {
                state.copy(
                    selectedMembers = (state.selectedMembers + event.user).toImmutableList(),
                    memberSearchResults = persistentListOf()
                )
            }
        }
    }

    private fun handleMemberRemoved(event: CreateEditGroupUiEvent.MemberRemoved) {
        _uiState.update { state ->
            state.copy(
                selectedMembers = state.selectedMembers
                    .filter { it.userId != event.user.userId }
                    .toImmutableList()
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
                val results = if (users.isEmpty()) {
                    val normalizedEmail = User.normalizeEmail(query)
                    val pendingUserId = User.generatePendingUserId(normalizedEmail)
                    if (pendingUserId !in selectedIds) {
                        listOf(
                            User(
                                userId = pendingUserId,
                                email = normalizedEmail,
                                isPending = true
                            )
                        )
                    } else {
                        emptyList()
                    }
                } else {
                    users.filter { u -> u.userId !in selectedIds }
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
                    )
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

        val selectedCurrencyModel = mappedCurrencies.find { it.code == group.currency }
        val extraCurrencyModels = mappedCurrencies.filter { it.code in group.extraCurrencies }

        val memberProfiles = getMemberProfilesUseCase(group.members)
        val memberUsers = group.members.map { memberId ->
            memberProfiles[memberId] ?: User(userId = memberId, email = "")
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
                selectedMembers = memberUsers.toImmutableList(),
                imageUrl = group.mainImagePath,
                localGroupImagePath = group.mainImagePath,
                isLoadingCurrencies = false
            )
        }
    }

    private fun handleMemberScanned(userId: String, email: String) {
        val alreadySelected = _uiState.value.selectedMembers.any { it.userId == userId }
        if (alreadySelected) return

        val partialUser = User(
            userId = userId,
            email = email,
            displayName = null,
            profileImagePath = null,
            bio = null
        )
        _uiState.update { state ->
            state.copy(
                selectedMembers = (state.selectedMembers + partialUser).toImmutableList()
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

    override fun onCleared() {
        super.onCleared()
        imageEventHandler.cleanTempImages()
    }

    companion object {
        private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
        private const val MEMBER_SEARCH_MIN_QUERY_LENGTH = 3
    }
}

private fun formatEventForLogging(event: CreateEditGroupUiEvent): String {
    return when (event) {
        is CreateEditGroupUiEvent.MemberSearchQueryChanged ->
            "MemberSearchQueryChanged(queryLength=${event.query.length})"
        is CreateEditGroupUiEvent.MemberSelected ->
            "MemberSelected(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateEditGroupUiEvent.MemberRemoved ->
            "MemberRemoved(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateEditGroupUiEvent.MemberScanned ->
            "MemberScanned(userId=${event.userId}, email=${event.email.maskEmail()})"
        is CreateEditGroupUiEvent.UnregisteredMemberDisplayNameChanged ->
            "UnregisteredMemberDisplayNameChanged(userId=${event.userId}, nameLength=${event.displayName.length})"
        else -> event::class.java.simpleName
    }
}
