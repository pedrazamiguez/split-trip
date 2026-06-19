package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.constant.AppConstants
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.sanitizer.maskEmail
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetUserDefaultCurrencyUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.CreateGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupImageEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupNavigationEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler.CreateGroupSubmitEventHandler
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateGroupUiState
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

class CreateGroupViewModel(
    private val createGroupNavigationEventHandler: CreateGroupNavigationEventHandler,
    private val createGroupImageEventHandler: CreateGroupImageEventHandler,
    private val createGroupSubmitEventHandler: CreateGroupSubmitEventHandler,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val getUserDefaultCurrencyUseCase: GetUserDefaultCurrencyUseCase,
    private val searchUsersByEmailUseCase: SearchUsersByEmailUseCase,
    private val emailValidationService: EmailValidationService,
    private val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    private val groupUiMapper: GroupUiMapper,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<CreateGroupUiAction>()
    val actions: SharedFlow<CreateGroupUiAction> = _actions.asSharedFlow()

    private var memberSearchJob: Job? = null

    init {
        createGroupNavigationEventHandler.bind(_uiState, _actions, viewModelScope)
        createGroupImageEventHandler.bind(_uiState, _actions, viewModelScope)
        createGroupSubmitEventHandler.bind(_uiState, _actions, viewModelScope)

        loadCurrencies()
        createGroupImageEventHandler.cleanTempImages()
    }

    @Suppress("CyclomaticComplexMethod")
    fun onEvent(event: CreateGroupUiEvent, onCreateGroupSuccess: () -> Unit) {
        Timber.tag(LogTag.MVI).d("Event: ${formatEventForLogging(event)}")
        when (event) {
            is CreateGroupUiEvent.NameChanged -> _uiState.update { state ->
                state.copy(groupName = event.name, isNameValid = event.name.isNotBlank())
            }
            is CreateGroupUiEvent.DescriptionChanged -> _uiState.update { state ->
                state.copy(groupDescription = event.description)
            }
            is CreateGroupUiEvent.CurrencySelected -> handleCurrencySelected(event.code)
            is CreateGroupUiEvent.ExtraCurrencyToggled -> handleExtraCurrencyToggled(event.code)
            is CreateGroupUiEvent.MemberSearchQueryChanged -> searchMembers(event.query)
            is CreateGroupUiEvent.MemberSelected -> handleMemberSelected(event)
            is CreateGroupUiEvent.MemberRemoved -> handleMemberRemoved(event)
            is CreateGroupUiEvent.MemberScanned -> handleMemberScanned(event.userId, event.email)
            is CreateGroupUiEvent.SubmitCreateGroup -> createGroupSubmitEventHandler.handleSubmit(onCreateGroupSuccess)
            is CreateGroupUiEvent.NextStep,
            is CreateGroupUiEvent.PreviousStep,
            is CreateGroupUiEvent.JumpToStep -> createGroupNavigationEventHandler.handleNavigation(event)
            is CreateGroupUiEvent.GroupImagePicked -> createGroupImageEventHandler.handleGroupImagePicked(event.uri)
            is CreateGroupUiEvent.GroupImageRemoved -> createGroupImageEventHandler.handleGroupImageRemoved()
            is CreateGroupUiEvent.ShowImageSourceSheet -> createGroupImageEventHandler.handleShowImageSourceSheet(
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

    private fun handleMemberSelected(event: CreateGroupUiEvent.MemberSelected) {
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

    private fun handleMemberRemoved(event: CreateGroupUiEvent.MemberRemoved) {
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
                _uiState.update {
                    it.copy(
                        memberSearchResults = users.filter { u -> u.userId !in selectedIds }.toImmutableList(),
                        isSearchingMembers = false
                    )
                }
            }.onFailure { e ->
                Timber.e(e, "Failed to search users by email")
                _uiState.update { it.copy(memberSearchResults = persistentListOf(), isSearchingMembers = false) }
            }
        }
    }

    private fun loadCurrencies() {
        if (_uiState.value.availableCurrencies.isNotEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCurrencies = true) }

            runCatching {
                withContext(defaultDispatcher) {
                    val userDefaultCurrency =
                        getUserDefaultCurrencyUseCase().firstOrNull()
                            ?: AppConstants.DEFAULT_CURRENCY_CODE

                    val sortedCurrencies = getSupportedCurrenciesUseCase().getOrThrow()
                    val mappedCurrencies = groupUiMapper.toCurrencyUiModels(sortedCurrencies)
                    val defaultCurrency = mappedCurrencies.find { it.code == userDefaultCurrency }
                        ?: mappedCurrencies.firstOrNull()

                    Triple(mappedCurrencies, defaultCurrency, userDefaultCurrency)
                }
            }.onSuccess { (mappedCurrencies, defaultCurrency, _) ->
                _uiState.update {
                    it.copy(
                        availableCurrencies = mappedCurrencies,
                        selectedCurrency = it.selectedCurrency ?: defaultCurrency,
                        isLoadingCurrencies = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(isLoadingCurrencies = false) }
                _actions.emit(
                    CreateGroupUiAction.ShowError(UiText.StringResource(R.string.group_error_load_currencies))
                )
                Timber.e(e, "Failed to load currencies")
            }
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
        createGroupImageEventHandler.cleanTempImages()
    }

    companion object {
        private const val MEMBER_SEARCH_DEBOUNCE_MS = 300L
        private const val MEMBER_SEARCH_MIN_QUERY_LENGTH = 3
    }
}

private fun formatEventForLogging(event: CreateGroupUiEvent): String {
    return when (event) {
        is CreateGroupUiEvent.MemberSearchQueryChanged ->
            "MemberSearchQueryChanged(queryLength=${event.query.length})"
        is CreateGroupUiEvent.MemberSelected ->
            "MemberSelected(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateGroupUiEvent.MemberRemoved ->
            "MemberRemoved(userId=${event.user.userId}, email=${event.user.email.maskEmail()})"
        is CreateGroupUiEvent.MemberScanned ->
            "MemberScanned(userId=${event.userId}, email=${event.email.maskEmail()})"
        else -> event::class.java.simpleName
    }
}
