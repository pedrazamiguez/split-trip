package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedFeature
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetSupportedCurrenciesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.mapper.GroupUiMapper
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.EditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.event.EditGroupUiEvent
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.EditGroupUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class EditGroupViewModel(
    private val getGroupByIdUseCase: GetGroupByIdUseCase,
    private val updateGroupUseCase: UpdateGroupUseCase,
    private val getSupportedCurrenciesUseCase: GetSupportedCurrenciesUseCase,
    private val groupImageStorageService: GroupImageStorageService,
    private val groupUiMapper: GroupUiMapper,
    private val featureGateService: FeatureGateService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditGroupUiState())
    val uiState: StateFlow<EditGroupUiState> = _uiState.asStateFlow()

    private val _actions = MutableSharedFlow<EditGroupUiAction>()
    val actions: SharedFlow<EditGroupUiAction> = _actions.asSharedFlow()

    private var initialGroup: Group? = null

    init {
        cleanTempGroupImages()
        viewModelScope.launch {
            featureGateService.isFeatureEnabled(GatedFeature.GROUP_COVER_UPLOAD).collect { isEnabled ->
                _uiState.update { it.copy(isCoverUploadEnabled = isEnabled) }
            }
        }
    }

    fun initGroup(groupId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Load supported currencies
            _uiState.update { it.copy(isLoadingCurrencies = true) }
            val currenciesResult = getSupportedCurrenciesUseCase().getOrNull() ?: emptyList()
            val availableCurrencyModels = groupUiMapper.toCurrencyUiModels(currenciesResult)
            _uiState.update {
                it.copy(
                    availableCurrencies = availableCurrencyModels,
                    isLoadingCurrencies = false
                )
            }

            val group = getGroupByIdUseCase(groupId)
            if (group != null) {
                initialGroup = group
                val selectedCurrencyModel = availableCurrencyModels.find { it.code == group.currency }
                val extraCurrencyModels = availableCurrencyModels.filter { it.code in group.extraCurrencies }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        groupName = group.name,
                        groupDescription = group.description,
                        selectedCurrency = selectedCurrencyModel,
                        extraCurrencies = extraCurrencyModels.toImmutableList(),
                        imageUrl = group.mainImagePath,
                        localGroupImagePath = group.mainImagePath
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _actions.emit(
                    EditGroupUiAction.ShowNotification(UiText.StringResource(R.string.group_detail_error_loading))
                )
                _actions.emit(EditGroupUiAction.NavigateBack)
            }
        }
    }

    fun onEvent(event: EditGroupUiEvent) {
        Timber.tag(LogTag.MVI).d("Event: $event")
        when (event) {
            is EditGroupUiEvent.NameChanged -> _uiState.update { state ->
                state.copy(groupName = event.name, isNameValid = event.name.isNotBlank())
            }
            is EditGroupUiEvent.DescriptionChanged -> _uiState.update { state ->
                state.copy(groupDescription = event.description)
            }
            is EditGroupUiEvent.CurrencySelected -> handleCurrencySelected(event.code)
            is EditGroupUiEvent.ExtraCurrencyToggled -> handleExtraCurrencyToggled(event.code)
            is EditGroupUiEvent.GroupImagePicked -> handleGroupImagePicked(event.uri)
            is EditGroupUiEvent.GroupImageRemoved -> handleGroupImageRemoved()
            is EditGroupUiEvent.ShowImageSourceSheet -> _uiState.update {
                it.copy(showImageSourceSheet = event.show)
            }
            is EditGroupUiEvent.SaveClicked -> handleSave()
        }
    }

    private fun handleCurrencySelected(code: String) {
        val selected = _uiState.value.availableCurrencies.find { it.code == code } ?: return
        _uiState.update { state ->
            // If the new main currency is currently an extra currency, remove it from extra currencies
            val updatedExtra = state.extraCurrencies.filter { it.code != code }.toImmutableList()
            state.copy(selectedCurrency = selected, extraCurrencies = updatedExtra)
        }
    }

    private fun handleExtraCurrencyToggled(code: String) {
        val currency = _uiState.value.availableCurrencies.find { it.code == code } ?: return
        _uiState.update { state ->
            val updated = if (state.extraCurrencies.any { it.code == code }) {
                state.extraCurrencies.filter { it.code != code }
            } else {
                state.extraCurrencies + currency
            }
            state.copy(extraCurrencies = updated.toImmutableList())
        }
    }

    private fun handleGroupImagePicked(uri: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching {
                groupImageStorageService.saveTempGroupImage(uri)
            }.onSuccess { tempUri ->
                _uiState.update { it.copy(localGroupImagePath = tempUri, imageUrl = null, isLoading = false) }
            }.onFailure { e ->
                Timber.e(e, "Failed to process group image")
                _uiState.update { it.copy(isLoading = false) }
                _actions.emit(
                    EditGroupUiAction.ShowNotification(
                        UiText.StringResource(R.string.group_error_image_processing_failed)
                    )
                )
            }
        }
    }

    private fun handleGroupImageRemoved() {
        _uiState.update { it.copy(localGroupImagePath = null, imageUrl = null) }
    }

    private fun handleSave() {
        val state = _uiState.value
        if (state.groupName.isBlank()) {
            _uiState.update { it.copy(isNameValid = false) }
            return
        }

        val group = initialGroup ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val updatedGroup = group.copy(
                name = state.groupName.trim(),
                description = state.groupDescription.trim(),
                currency = state.selectedCurrency?.code ?: "EUR",
                extraCurrencies = state.extraCurrencies.map { it.code },
                mainImagePath = state.localGroupImagePath
            )

            updateGroupUseCase(updatedGroup)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false) }
                    _actions.emit(
                        EditGroupUiAction.ShowNotification(UiText.StringResource(R.string.group_edit_success_saved))
                    )
                    _actions.emit(EditGroupUiAction.NavigateBack)
                }
                .onFailure { e ->
                    Timber.e(e, "Failed to save group details")
                    _uiState.update { it.copy(isSaving = false) }
                    _actions.emit(
                        EditGroupUiAction.ShowNotification(UiText.StringResource(R.string.group_error_creation_failed))
                    )
                }
        }
    }

    private fun cleanTempGroupImages() {
        viewModelScope.launch {
            try {
                groupImageStorageService.cleanTempGroupImages()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clean temp group images")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanTempGroupImages()
    }
}
