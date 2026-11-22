package es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewPrompt
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.ProFeaturePreviewType
import es.pedrazamiguez.splittrip.core.designsystem.telemetry.trackLimitGateBlocked
import es.pedrazamiguez.splittrip.core.logging.MonetizationTelemetry
import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import es.pedrazamiguez.splittrip.domain.service.featuregate.GatedLimit
import es.pedrazamiguez.splittrip.features.group.R
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.action.CreateEditGroupUiAction
import es.pedrazamiguez.splittrip.features.group.presentation.viewmodel.state.CreateEditGroupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateEditGroupImageEventHandlerImpl(
    private val groupImageStorageService: GroupImageStorageService,
    private val featureGateService: FeatureGateService,
    private val telemetryTracker: TelemetryTracker
) : CreateEditGroupImageEventHandler {
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

    override fun handleGroupImagePicked(uri: String) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val quota = featureGateService.observeMediaUploadQuota().first()
            if (!quota.canUpload) {
                telemetryTracker.trackLimitGateBlocked(
                    limit = GatedLimit.MANUAL_RECEIPT_MONTHLY_ATTACHMENTS,
                    entryPoint = MonetizationTelemetry.GateEntryPoint.GROUP_COVER_PICK,
                    upgradePrompted = quota.requiresUpgrade
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        proPreviewPrompt = ProFeaturePreviewPrompt(
                            previewType = ProFeaturePreviewType.GROUP_COVER_PHOTO,
                            entrySource = MonetizationTelemetry.EntrySource.GROUP_COVER_UPLOAD
                        )
                    )
                }
                return@launch
            }
            runCatching {
                groupImageStorageService.saveTempGroupImage(uri)
            }.onSuccess { tempUri ->
                _uiState.update { it.copy(localGroupImagePath = tempUri, imageUrl = null, isLoading = false) }
            }.onFailure { e ->
                Timber.e(e, "Failed to process group image")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringResource(R.string.group_error_image_processing_failed)
                    )
                }
            }
        }
    }

    override fun handleGroupImageRemoved() {
        _uiState.update { it.copy(localGroupImagePath = null, imageUrl = null) }
    }

    override fun handleShowImageSourceSheet(show: Boolean) {
        _uiState.update { it.copy(showImageSourceSheet = show) }
    }

    override fun cleanTempImages() {
        scope.launch {
            try {
                groupImageStorageService.cleanTempGroupImages()
            } catch (e: Exception) {
                Timber.e(e, "Failed to clean temp group images")
            }
        }
    }
}
