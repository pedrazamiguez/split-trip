package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.features.settings.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeveloperServicesUiState(
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
    val extractedTitle: String? = null,
    val extractionSource: String? = null,
    val extractionConfidence: String? = null,
    val extractionCapability: String? = null,
    val extractionErrorMessage: UiText? = null
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
    data object RunOcr : DeveloperServicesUiEvent
    data object RunExtraction : DeveloperServicesUiEvent
    data object Reset : DeveloperServicesUiEvent
}

class DeveloperServicesViewModel(
    private val receiptOcrService: ReceiptOcrService,
    private val receiptExtractionService: ReceiptExtractionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperServicesUiState())
    val uiState: StateFlow<DeveloperServicesUiState> = _uiState.asStateFlow()

    private var lastRawReceiptText: RawReceiptText? = null

    fun onEvent(event: DeveloperServicesUiEvent) {
        when (event) {
            is DeveloperServicesUiEvent.FileSelected -> handleFileSelected(event.uri, event.name, event.mimeType)
            is DeveloperServicesUiEvent.RunOcr -> runOcr()
            is DeveloperServicesUiEvent.RunExtraction -> runExtraction()
            is DeveloperServicesUiEvent.Reset -> reset()
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
                extractedTitle = null,
                extractionSource = null,
                extractionConfidence = null,
                extractionCapability = null,
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
                            textBlocks = rawReceipt.blocks.map { block -> block.text }.toImmutableList(),
                            extractionCapability = receiptExtractionService.capability().name
                        )
                    }
                }
                .onFailure { error ->
                    val errorMsg = error.localizedMessage
                    val uiText = if (errorMsg != null) {
                        UiText.DynamicString(errorMsg)
                    } else {
                        UiText.StringResource(R.string.developer_services_ocr_error_fallback)
                    }
                    _uiState.update {
                        it.copy(
                            ocrStatus = OcrStatus.Error,
                            errorMessage = uiText
                        )
                    }
                }
        }
    }

    private fun runExtraction() {
        val rawText = lastRawReceiptText ?: return

        _uiState.update {
            it.copy(
                extractionStatus = ExtractionStatus.Loading,
                extractionErrorMessage = null
            )
        }

        viewModelScope.launch {
            receiptExtractionService.extract(rawText)
                .onSuccess { receipt ->
                    _uiState.update {
                        it.copy(
                            extractionStatus = ExtractionStatus.Success,
                            extractedAmount = receipt.amount?.toPlainString(),
                            extractedCurrency = receipt.currency,
                            extractedDate = receipt.date?.toString(),
                            extractedTitle = receipt.title,
                            extractionSource = receipt.source.name,
                            extractionConfidence = receipt.confidence.name
                        )
                    }
                }
                .onFailure { error ->
                    val errorMsg = error.localizedMessage
                    val uiText = if (errorMsg != null) {
                        UiText.DynamicString(errorMsg)
                    } else {
                        UiText.StringResource(R.string.developer_services_extraction_error_fallback)
                    }
                    _uiState.update {
                        it.copy(
                            extractionStatus = ExtractionStatus.Error,
                            extractionErrorMessage = uiText
                        )
                    }
                }
        }
    }

    private fun reset() {
        lastRawReceiptText = null
        _uiState.value = DeveloperServicesUiState()
    }
}
