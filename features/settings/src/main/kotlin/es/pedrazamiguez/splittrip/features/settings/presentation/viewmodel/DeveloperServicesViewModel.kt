package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.DeveloperServicesUiAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DeveloperServicesTab {
    data object Ocr : DeveloperServicesTab
    data object AiExtraction : DeveloperServicesTab
    data object Billing : DeveloperServicesTab
}

data class DeveloperServicesUiState(
    val selectedTab: DeveloperServicesTab = DeveloperServicesTab.Ocr,
    val selectedFileUri: String? = null,
    val selectedFileName: String = "",
    val selectedFileMimeType: String = "",
    val ocrStatus: OcrStatus = OcrStatus.Idle,
    val extractedText: String = "",
    val textBlocks: ImmutableList<String> = persistentListOf(),
    val errorMessage: UiText? = null,
    val extractionStatus: ExtractionStatus = ExtractionStatus.Idle,
    val extractedAmount: String? = null,
    val extractedCurrency: String? = null,
    val extractedDate: String? = null,
    val extractedTime: String? = null,
    val extractedTitle: String? = null,
    val extractedVendor: String? = null,
    val extractedPaymentMethod: String? = null,
    val extractedCategory: String? = null,
    val extractedNotes: String? = null,
    val extractionSource: ExtractionSource? = null,
    val extractionConfidence: ExtractionConfidence? = null,
    val extractionErrorMessage: UiText? = null,
    val activeResolvedModel: AiEngineType? = null,
    val developerOverrideModel: AiEngineType? = null,
    val availableModels: ImmutableList<AiEngineType?> = persistentListOf(
        null,
        AiEngineType.AI_CORE_GEMMA_4,
        AiEngineType.LITE_RT_LM
    ),
    val currentTier: SubscriptionTier = SubscriptionTier.FREE,
    val isUpdatingTier: Boolean = false,
    val pendingTargetTier: SubscriptionTier? = null,
    val hasSignedInUser: Boolean = false,
    val billingErrorMessage: UiText? = null
)

sealed interface OcrStatus {
    data object Idle : OcrStatus
    data object Loading : OcrStatus
    data object Success : OcrStatus
    data object Error : OcrStatus
}

sealed interface ExtractionStatus {
    data object Idle : ExtractionStatus
    data object Loading : ExtractionStatus
    data object Success : ExtractionStatus
    data object Error : ExtractionStatus
}

sealed interface DeveloperServicesUiEvent {
    data class FileSelected(val uri: String, val name: String, val mimeType: String) : DeveloperServicesUiEvent
    data class SwitchTab(val tab: DeveloperServicesTab) : DeveloperServicesUiEvent
    data object RunOcr : DeveloperServicesUiEvent
    data object RunOcrAndExtract : DeveloperServicesUiEvent
    data object Reset : DeveloperServicesUiEvent
    data class SelectModel(val model: AiEngineType?) : DeveloperServicesUiEvent
    data class SetSubscriptionTier(val tier: SubscriptionTier) : DeveloperServicesUiEvent
}

class DeveloperServicesViewModel(
    private val receiptOcrService: ReceiptOcrService,
    private val receiptExtractionService: ReceiptExtractionService,
    private val aiModelResolver: AiModelResolverService,
    private val observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val updateUserTierUseCase: UpdateUserTierUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperServicesUiState())
    val uiState: StateFlow<DeveloperServicesUiState> = _uiState.asStateFlow()

    private val _actions = Channel<DeveloperServicesUiAction>(Channel.BUFFERED)
    val actions: Flow<DeveloperServicesUiAction> = _actions.receiveAsFlow()

    private var lastRawReceiptText: RawReceiptText? = null
    private var currentUserId: String? = null

    init {
        combine(
            aiModelResolver.getActiveModel(),
            aiModelResolver.getDeveloperOverrideModel()
        ) { activeModel, overrideModel ->
            _uiState.update {
                it.copy(
                    activeResolvedModel = activeModel,
                    developerOverrideModel = overrideModel
                )
            }
        }.launchIn(viewModelScope)
        observeUserProfile()
    }

    private fun observeUserProfile() {
        observeCurrentUserProfileUseCase()
            .onEach { user ->
                currentUserId = user?.userId
                _uiState.update {
                    it.copy(
                        currentTier = user?.tier ?: SubscriptionTier.FREE,
                        hasSignedInUser = user != null
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: DeveloperServicesUiEvent) {
        when (event) {
            is DeveloperServicesUiEvent.FileSelected -> handleFileSelected(event.uri, event.name, event.mimeType)
            is DeveloperServicesUiEvent.SwitchTab -> _uiState.update { it.copy(selectedTab = event.tab) }
            is DeveloperServicesUiEvent.RunOcr -> runOcr()
            is DeveloperServicesUiEvent.RunOcrAndExtract -> runOcrAndExtract()
            is DeveloperServicesUiEvent.Reset -> reset()
            is DeveloperServicesUiEvent.SelectModel -> selectModel(event.model)
            is DeveloperServicesUiEvent.SetSubscriptionTier -> setSubscriptionTier(event.tier)
        }
    }

    private fun selectModel(model: AiEngineType?) {
        viewModelScope.launch {
            aiModelResolver.setDeveloperOverrideModel(model)
        }
    }

    private fun setSubscriptionTier(targetTier: SubscriptionTier) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdatingTier = true,
                    pendingTargetTier = targetTier,
                    billingErrorMessage = null
                )
            }
            val userId = currentUserId ?: getCurrentUserProfileUseCase()?.userId
            if (userId == null) {
                val noUserError = UiText.StringResource(R.string.developer_services_billing_no_user_error)
                _uiState.update {
                    it.copy(
                        isUpdatingTier = false,
                        pendingTargetTier = null,
                        billingErrorMessage = noUserError
                    )
                }
                _actions.send(DeveloperServicesUiAction.ShowTopPill(noUserError))
                return@launch
            }

            updateUserTierUseCase(userId, targetTier)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isUpdatingTier = false,
                            pendingTargetTier = null,
                            billingErrorMessage = null,
                            currentTier = targetTier
                        )
                    }
                    val message = UiText.StringResource(
                        R.string.developer_services_billing_tier_updated,
                        targetTier.name
                    )
                    _actions.send(DeveloperServicesUiAction.ShowTopPill(message))
                }
                .onFailure { error ->
                    val errorMessage = error.toUiText(R.string.developer_services_billing_update_error)
                    _uiState.update {
                        it.copy(
                            isUpdatingTier = false,
                            pendingTargetTier = null,
                            billingErrorMessage = errorMessage
                        )
                    }
                    _actions.send(DeveloperServicesUiAction.ShowTopPill(errorMessage))
                }
        }
    }

    private fun handleFileSelected(uri: String, name: String, mimeType: String) {
        lastRawReceiptText = null
        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = name,
                selectedFileMimeType = mimeType,
                ocrStatus = OcrStatus.Idle,
                extractedText = "",
                textBlocks = persistentListOf(),
                errorMessage = null,
                extractionStatus = ExtractionStatus.Idle,
                extractedAmount = null,
                extractedCurrency = null,
                extractedDate = null,
                extractedTime = null,
                extractedTitle = null,
                extractedVendor = null,
                extractedPaymentMethod = null,
                extractedCategory = null,
                extractedNotes = null,
                extractionSource = null,
                extractionConfidence = null,
                extractionErrorMessage = null
            )
        }
    }

    private fun runOcr() {
        val currentState = _uiState.value
        val uriString = currentState.selectedFileUri ?: return
        val mimeType = currentState.selectedFileMimeType

        _uiState.update {
            it.copy(
                ocrStatus = OcrStatus.Loading,
                errorMessage = null,
                extractionStatus = ExtractionStatus.Idle,
                extractionErrorMessage = null
            )
        }

        viewModelScope.launch {
            val attachment = ReceiptAttachment(
                localUri = uriString,
                mimeType = mimeType,
                capturedAtMillis = System.currentTimeMillis()
            )

            receiptOcrService.recogniseText(attachment)
                .onSuccess { rawReceipt ->
                    lastRawReceiptText = rawReceipt
                    _uiState.update {
                        it.copy(
                            ocrStatus = OcrStatus.Success,
                            extractedText = rawReceipt.fullText,
                            textBlocks = rawReceipt.blocks.map { block -> block.text }.toImmutableList()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            ocrStatus = OcrStatus.Error,
                            errorMessage = error.toUiText(R.string.developer_services_ocr_error_fallback)
                        )
                    }
                }
        }
    }

    private fun runOcrAndExtract() {
        val currentState = _uiState.value
        val uriString = currentState.selectedFileUri ?: return
        val mimeType = currentState.selectedFileMimeType

        _uiState.update {
            it.copy(extractionStatus = ExtractionStatus.Loading, extractionErrorMessage = null)
        }

        viewModelScope.launch {
            val attachment = ReceiptAttachment(
                localUri = uriString,
                mimeType = mimeType,
                capturedAtMillis = System.currentTimeMillis()
            )

            receiptOcrService.recogniseText(attachment)
                .onSuccess { rawReceipt ->
                    lastRawReceiptText = rawReceipt
                    runExtractionInternal(rawReceipt)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            extractionStatus = ExtractionStatus.Error,
                            extractionErrorMessage = error.toUiText(R.string.developer_services_ocr_error_fallback)
                        )
                    }
                }
        }
    }

    private suspend fun runExtractionInternal(rawText: RawReceiptText) {
        receiptExtractionService.extract(rawText)
            .onSuccess { receipt ->
                _uiState.update {
                    it.copy(
                        extractionStatus = ExtractionStatus.Success,
                        extractedAmount = receipt.amount?.toPlainString(),
                        extractedCurrency = receipt.currency,
                        extractedDate = receipt.date?.toString(),
                        extractedTime = receipt.time?.toString(),
                        extractedTitle = receipt.title,
                        extractedVendor = receipt.vendor,
                        extractedPaymentMethod = receipt.paymentMethod,
                        extractedCategory = receipt.category,
                        extractedNotes = receipt.notes,
                        extractionSource = receipt.source,
                        extractionConfidence = receipt.confidence
                    )
                }
            }
            .onFailure { error ->
                _uiState.update {
                    it.copy(
                        extractionStatus = ExtractionStatus.Error,
                        extractionErrorMessage = error.toUiText(R.string.developer_services_extraction_error_fallback)
                    )
                }
            }
    }

    private fun reset() {
        lastRawReceiptText = null
        val current = _uiState.value
        _uiState.value = DeveloperServicesUiState(
            selectedTab = current.selectedTab,
            activeResolvedModel = current.activeResolvedModel,
            developerOverrideModel = current.developerOverrideModel,
            currentTier = current.currentTier,
            isUpdatingTier = current.isUpdatingTier,
            pendingTargetTier = current.pendingTargetTier,
            hasSignedInUser = current.hasSignedInUser,
            billingErrorMessage = current.billingErrorMessage
        )
    }

    private fun Throwable.toUiText(fallbackRes: Int): UiText {
        val msg = localizedMessage
        return if (msg != null) UiText.DynamicString(msg) else UiText.StringResource(fallbackRes)
    }
}
