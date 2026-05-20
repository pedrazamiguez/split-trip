package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
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
    val errorMessage: UiText? = null
)

sealed interface OcrStatus {
    data object Idle : OcrStatus
    data object Loading : OcrStatus
    data object Success : OcrStatus
    data object Error : OcrStatus
}

sealed interface DeveloperServicesUiEvent {
    data class FileSelected(val uri: String, val name: String, val mimeType: String) : DeveloperServicesUiEvent
    data object RunOcr : DeveloperServicesUiEvent
    data object Reset : DeveloperServicesUiEvent
}

class DeveloperServicesViewModel(
    private val receiptOcrService: ReceiptOcrService
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeveloperServicesUiState())
    val uiState: StateFlow<DeveloperServicesUiState> = _uiState.asStateFlow()

    fun onEvent(event: DeveloperServicesUiEvent) {
        when (event) {
            is DeveloperServicesUiEvent.FileSelected -> handleFileSelected(event.uri, event.name, event.mimeType)
            is DeveloperServicesUiEvent.RunOcr -> runOcr()
            is DeveloperServicesUiEvent.Reset -> reset()
        }
    }

    private fun handleFileSelected(uri: String, name: String, mimeType: String) {
        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = name,
                selectedFileMimeType = mimeType,
                ocrStatus = OcrStatus.Idle,
                extractedText = "",
                textBlocks = persistentListOf(),
                errorMessage = null
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
                errorMessage = null
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
                    _uiState.update {
                        it.copy(
                            ocrStatus = OcrStatus.Success,
                            extractedText = rawReceipt.fullText,
                            textBlocks = rawReceipt.blocks.map { block -> block.text }.toImmutableList()
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

    private fun reset() {
        _uiState.value = DeveloperServicesUiState()
    }
}
