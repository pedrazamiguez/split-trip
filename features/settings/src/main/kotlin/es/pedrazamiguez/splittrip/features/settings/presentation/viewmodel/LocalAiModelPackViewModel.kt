package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.exception.LocalAiModelNotEntitledException
import es.pedrazamiguez.splittrip.domain.usecase.setting.CancelLocalAiModelPackDownloadUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DeleteLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.DownloadLocalAiModelPackUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLocalAiModelPackWifiOnlyUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.mapper.LocalAiModelPackUiMapper
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.LocalAiModelPackUiAction
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.event.LocalAiModelPackUiEvent
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.state.LocalAiModelPackUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
class LocalAiModelPackViewModel(
    private val observeLocalAiModelPackStateUseCase: ObserveLocalAiModelPackStateUseCase,
    private val downloadLocalAiModelPackUseCase: DownloadLocalAiModelPackUseCase,
    private val cancelLocalAiModelPackDownloadUseCase: CancelLocalAiModelPackDownloadUseCase,
    private val deleteLocalAiModelPackUseCase: DeleteLocalAiModelPackUseCase,
    private val setLocalAiModelPackWifiOnlyUseCase: SetLocalAiModelPackWifiOnlyUseCase,
    private val localAiModelPackUiMapper: LocalAiModelPackUiMapper
) : ViewModel() {

    private val showDeleteConfirmation = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(LocalAiModelPackUiState(isLoading = true))
    val uiState: StateFlow<LocalAiModelPackUiState> = _uiState.asStateFlow()

    private val _actions = Channel<LocalAiModelPackUiAction>(Channel.BUFFERED)
    val actions: Flow<LocalAiModelPackUiAction> = _actions.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                observeLocalAiModelPackStateUseCase(),
                showDeleteConfirmation
            ) { state, showDialog ->
                localAiModelPackUiMapper.map(
                    state = state,
                    showDeleteConfirmation = showDialog
                )
            }.collect { mappedState ->
                _uiState.value = mappedState
            }
        }
    }

    fun onEvent(event: LocalAiModelPackUiEvent) {
        when (event) {
            LocalAiModelPackUiEvent.RequestDownload,
            LocalAiModelPackUiEvent.RetryDownload -> handleDownloadRequest()

            LocalAiModelPackUiEvent.CancelDownload -> handleCancelDownload()

            LocalAiModelPackUiEvent.RequestDelete -> showDeleteConfirmation.value = true

            LocalAiModelPackUiEvent.DismissDeleteDialog -> showDeleteConfirmation.value = false

            LocalAiModelPackUiEvent.ConfirmDelete -> handleConfirmDelete()

            is LocalAiModelPackUiEvent.ToggleWifiOnly -> handleToggleWifiOnly(event.enabled)

            LocalAiModelPackUiEvent.RequestUpgrade -> emitAction(LocalAiModelPackUiAction.RequestUpgrade)
        }
    }

    private fun handleDownloadRequest() {
        viewModelScope.launch {
            downloadLocalAiModelPackUseCase()
                .onSuccess {
                    _actions.send(
                        LocalAiModelPackUiAction.ShowTopPill(
                            UiText.StringResource(R.string.local_ai_pack_pill_download_started)
                        )
                    )
                }
                .onFailure { error ->
                    if (error is LocalAiModelNotEntitledException) {
                        _actions.send(LocalAiModelPackUiAction.RequestUpgrade)
                    } else {
                        _actions.send(
                            LocalAiModelPackUiAction.ShowTopPill(
                                UiText.StringResource(R.string.local_ai_pack_pill_download_failed)
                            )
                        )
                    }
                }
        }
    }

    private fun handleCancelDownload() {
        viewModelScope.launch {
            cancelLocalAiModelPackDownloadUseCase()
                .onSuccess {
                    _actions.send(
                        LocalAiModelPackUiAction.ShowTopPill(
                            UiText.StringResource(R.string.local_ai_pack_pill_download_cancelled)
                        )
                    )
                }
                .onFailure {
                    _actions.send(
                        LocalAiModelPackUiAction.ShowTopPill(
                            UiText.StringResource(R.string.local_ai_pack_pill_action_failed)
                        )
                    )
                }
        }
    }

    private fun handleConfirmDelete() {
        showDeleteConfirmation.value = false
        viewModelScope.launch {
            deleteLocalAiModelPackUseCase()
                .onSuccess {
                    _actions.send(
                        LocalAiModelPackUiAction.ShowTopPill(
                            UiText.StringResource(R.string.local_ai_pack_pill_deleted)
                        )
                    )
                }
                .onFailure {
                    _actions.send(
                        LocalAiModelPackUiAction.ShowTopPill(
                            UiText.StringResource(R.string.local_ai_pack_pill_action_failed)
                        )
                    )
                }
        }
    }

    private fun handleToggleWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            setLocalAiModelPackWifiOnlyUseCase(enabled)
        }
    }

    private fun emitAction(action: LocalAiModelPackUiAction) {
        viewModelScope.launch {
            _actions.send(action)
        }
    }
}
