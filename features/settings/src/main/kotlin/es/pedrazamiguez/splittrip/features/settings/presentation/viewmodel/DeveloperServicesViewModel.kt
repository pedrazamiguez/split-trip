package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveDeveloperAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.DeveloperServicesUiAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
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
    data object CategorySuggestion : DeveloperServicesTab
}

sealed interface CategorySuggestionStatus {
    data object Idle : CategorySuggestionStatus
    data object Loading : CategorySuggestionStatus
    data object Matched : CategorySuggestionStatus
    data object NoMatch : CategorySuggestionStatus
    data object Error : CategorySuggestionStatus
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
    val extractedSubcategory: String? = null,
    val extractedNotes: String? = null,
    val extractionSource: ExtractionSource? = null,
    val extractionConfidence: ExtractionConfidence? = null,
    val extractionErrorMessage: UiText? = null,
    val activeResolvedModel: AiEngineType? = null,
    val developerOverrideModel: AiEngineType? = null,
    val installedLocalAiPack: InstalledLocalAiPackInfo? = null,
    val availableModels: ImmutableList<AiEngineType?> = persistentListOf(
        null,
        AiEngineType.AI_CORE_GEMMA_4,
        AiEngineType.LITE_RT_LM
    ),
    val categorySuggestionInput: String = "",
    val categorySuggestionContext: GroupContext = GroupContext.TRAVEL,
    val categorySuggestionStatus: CategorySuggestionStatus = CategorySuggestionStatus.Idle,
    val suggestedCategory: ExpenseCategory? = null,
    val suggestedSubcategory: ExpenseSubcategory? = null,
    val suggestedCategorySource: CategorySuggestionSource? = null,
    val categorySuggestionErrorMessage: UiText? = null
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
    data class CategoryTitleChanged(val title: String) : DeveloperServicesUiEvent
    data class CategoryContextChanged(val context: GroupContext) : DeveloperServicesUiEvent
    data object ClearCategoryInput : DeveloperServicesUiEvent
}

class DeveloperServicesViewModel(
    private val receiptOcrService: ReceiptOcrService,
    private val receiptExtractionService: ReceiptExtractionService,
    private val aiModelResolver: AiModelResolverService,
    private val observeLocalAiModelPackStateUseCase: ObserveLocalAiModelPackStateUseCase,
    private val suggestExpenseCategoryUseCase: SuggestExpenseCategoryUseCase,
    private val observeDeveloperAccessUseCase: ObserveDeveloperAccessUseCase,
    private val categorySuggestionDebounceMs: Long = CATEGORY_SUGGESTION_DEBOUNCE_MS
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperServicesUiState())
    val uiState: StateFlow<DeveloperServicesUiState> = _uiState.asStateFlow()

    private val _isDeveloperAccessGranted = MutableStateFlow<Boolean?>(null)
    val isDeveloperAccessGranted: StateFlow<Boolean?> = _isDeveloperAccessGranted.asStateFlow()

    private val _actions = Channel<DeveloperServicesUiAction>(Channel.BUFFERED)
    val actions: Flow<DeveloperServicesUiAction> = _actions.receiveAsFlow()

    private var lastRawReceiptText: RawReceiptText? = null
    private var categorySuggestionJob: Job? = null

    init {
        observeDeveloperAccess()
        combine(
            aiModelResolver.getActiveModel(),
            aiModelResolver.getDeveloperOverrideModel(),
            observeLocalAiModelPackStateUseCase()
        ) { activeModel, overrideModel, packState ->
            _uiState.update {
                it.copy(
                    activeResolvedModel = activeModel,
                    developerOverrideModel = overrideModel,
                    installedLocalAiPack = InstalledLocalAiPackInfo.fromState(packState)
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeDeveloperAccess() {
        observeDeveloperAccessUseCase()
            .onEach { granted ->
                _isDeveloperAccessGranted.value = granted
                if (!granted) {
                    _actions.send(DeveloperServicesUiAction.NavigateBack)
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
            is DeveloperServicesUiEvent.CategoryTitleChanged -> scheduleCategorySuggestion(
                title = event.title,
                context = _uiState.value.categorySuggestionContext
            )
            is DeveloperServicesUiEvent.CategoryContextChanged -> {
                if (event.context != _uiState.value.categorySuggestionContext) {
                    scheduleCategorySuggestion(
                        title = _uiState.value.categorySuggestionInput,
                        context = event.context
                    )
                }
            }
            is DeveloperServicesUiEvent.ClearCategoryInput -> clearCategoryInput()
        }
    }

    private fun selectModel(model: AiEngineType?) {
        viewModelScope.launch {
            aiModelResolver.setDeveloperOverrideModel(model)
        }
    }

    private fun scheduleCategorySuggestion(title: String, context: GroupContext) {
        categorySuggestionJob?.cancel()
        val trimmed = title.trim()
        if (trimmed.length < MIN_CATEGORY_TITLE_LENGTH) {
            _uiState.update {
                it.copy(
                    categorySuggestionInput = title,
                    categorySuggestionContext = context,
                    categorySuggestionStatus = CategorySuggestionStatus.Idle,
                    suggestedCategory = null,
                    suggestedSubcategory = null,
                    suggestedCategorySource = null,
                    categorySuggestionErrorMessage = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                categorySuggestionInput = title,
                categorySuggestionContext = context,
                categorySuggestionStatus = CategorySuggestionStatus.Loading,
                suggestedCategory = null,
                suggestedSubcategory = null,
                suggestedCategorySource = null,
                categorySuggestionErrorMessage = null
            )
        }

        categorySuggestionJob = viewModelScope.launch {
            delay(categorySuggestionDebounceMs)
            executeCategorySuggestion(trimmed = trimmed, context = context)
        }
    }

    private suspend fun executeCategorySuggestion(trimmed: String, context: GroupContext) {
        val suggestion = try {
            suggestExpenseCategoryUseCase(title = trimmed, groupContext = context)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isCurrentSuggestionRequest(trimmed, context)) return
            _uiState.update {
                it.copy(
                    categorySuggestionStatus = CategorySuggestionStatus.Error,
                    categorySuggestionErrorMessage = e.toUiText(R.string.developer_services_category_error_fallback)
                )
            }
            return
        }

        if (!isCurrentSuggestionRequest(trimmed, context)) return

        _uiState.update {
            if (suggestion != null) {
                it.copy(
                    categorySuggestionStatus = CategorySuggestionStatus.Matched,
                    suggestedCategory = suggestion.category,
                    suggestedSubcategory = suggestion.subcategory,
                    suggestedCategorySource = suggestion.source,
                    categorySuggestionErrorMessage = null
                )
            } else {
                it.copy(
                    categorySuggestionStatus = CategorySuggestionStatus.NoMatch,
                    suggestedCategory = null,
                    suggestedSubcategory = null,
                    suggestedCategorySource = null,
                    categorySuggestionErrorMessage = null
                )
            }
        }
    }

    private fun isCurrentSuggestionRequest(trimmed: String, context: GroupContext): Boolean {
        val current = _uiState.value
        return current.categorySuggestionInput.trim() == trimmed && current.categorySuggestionContext == context
    }

    private fun clearCategoryInput() {
        categorySuggestionJob?.cancel()
        categorySuggestionJob = null
        _uiState.update {
            it.copy(
                categorySuggestionInput = "",
                categorySuggestionStatus = CategorySuggestionStatus.Idle,
                suggestedCategory = null,
                suggestedSubcategory = null,
                suggestedCategorySource = null,
                categorySuggestionErrorMessage = null
            )
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
                extractedSubcategory = null,
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
                        extractedSubcategory = receipt.subcategory,
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
            installedLocalAiPack = current.installedLocalAiPack,
            categorySuggestionInput = current.categorySuggestionInput,
            categorySuggestionContext = current.categorySuggestionContext,
            categorySuggestionStatus = current.categorySuggestionStatus,
            suggestedCategory = current.suggestedCategory,
            suggestedSubcategory = current.suggestedSubcategory,
            suggestedCategorySource = current.suggestedCategorySource,
            categorySuggestionErrorMessage = current.categorySuggestionErrorMessage
        )
    }

    private fun Throwable.toUiText(fallbackRes: Int): UiText {
        val msg = localizedMessage
        return if (msg != null) UiText.DynamicString(msg) else UiText.StringResource(fallbackRes)
    }

    companion object {
        const val CATEGORY_SUGGESTION_DEBOUNCE_MS = 250L
        private const val MIN_CATEGORY_TITLE_LENGTH = 2
    }
}
