package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("DeveloperServicesViewModel")
class DeveloperServicesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var receiptOcrService: ReceiptOcrService
    private lateinit var receiptExtractionService: ReceiptExtractionService

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        receiptOcrService = mockk()
        receiptExtractionService = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DeveloperServicesViewModel(
        receiptOcrService = receiptOcrService,
        receiptExtractionService = receiptExtractionService
    )

    @Test
    fun `initial state is idle and empty`() {
        val viewModel = createViewModel()
        val state = viewModel.uiState.value

        assertNull(state.selectedFileUri)
        assertEquals("", state.selectedFileName)
        assertEquals("", state.selectedFileMimeType)
        assertEquals(OcrStatus.Idle, state.ocrStatus)
        assertEquals("", state.extractedText)
        assertTrue(state.textBlocks.isEmpty())
        assertNull(state.errorMessage)
    }

    @Nested
    @DisplayName("FileSelected Event")
    inner class FileSelectedEvent {

        @Test
        fun `updates state with file details`() {
            val viewModel = createViewModel()

            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val state = viewModel.uiState.value
            assertEquals("content://media/external/file/123", state.selectedFileUri)
            assertEquals("receipt.pdf", state.selectedFileName)
            assertEquals("application/pdf", state.selectedFileMimeType)
            assertEquals(OcrStatus.Idle, state.ocrStatus)
            assertEquals("", state.extractedText)
            assertTrue(state.textBlocks.isEmpty())
            assertNull(state.errorMessage)
        }
    }

    @Nested
    @DisplayName("RunOcr Event")
    inner class RunOcrEvent {

        @Test
        fun `does nothing if no file is selected`() = runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(OcrStatus.Idle, state.ocrStatus)
        }

        @Test
        fun `successful OCR maps to success state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val rawReceiptText = RawReceiptText(
                fullText = "Total amount: 50.00 EUR",
                blocks = persistentListOf(TextBlock(text = "Total amount: 50.00 EUR", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )

            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)

            // Verify status is Loading immediately after event trigger
            assertEquals(OcrStatus.Loading, viewModel.uiState.value.ocrStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(OcrStatus.Success, state.ocrStatus)
            assertEquals("Total amount: 50.00 EUR", state.extractedText)
            assertEquals(1, state.textBlocks.size)
            assertEquals("Total amount: 50.00 EUR", state.textBlocks[0])
            assertNull(state.errorMessage)
        }

        @Test
        fun `failed OCR maps to error state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val exception = RuntimeException("OCR Engine error")
            coEvery { receiptOcrService.recogniseText(any()) } returns Result.failure(exception)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)

            // Verify status is Loading immediately
            assertEquals(OcrStatus.Loading, viewModel.uiState.value.ocrStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(OcrStatus.Error, state.ocrStatus)
            assertEquals(UiText.DynamicString("OCR Engine error"), state.errorMessage)
        }
    }

    @Nested
    @DisplayName("RunExtraction Event")
    inner class RunExtractionEvent {

        private suspend fun TestScope.prepareOcrSuccess(viewModel: DeveloperServicesViewModel) {
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )
            val rawReceiptText = RawReceiptText(
                fullText = "Total amount: 50.00 EUR",
                blocks = persistentListOf(TextBlock(text = "Total amount: 50.00 EUR", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)
            advanceUntilIdle()
        }

        @Test
        fun `does nothing if OCR has not been run yet`() = runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(DeveloperServicesUiEvent.RunExtraction)
            advanceUntilIdle()

            assertEquals(ExtractionStatus.Idle, viewModel.uiState.value.extractionStatus)
        }

        @Test
        fun `successful extraction maps to success state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            prepareOcrSuccess(viewModel)

            val extracted = ExtractedReceipt(
                amount = BigDecimal("50.00"),
                currency = "EUR",
                date = LocalDate.of(2026, 5, 20),
                title = "Test Store",
                source = ExtractionSource.AI_CORE,
                confidence = ExtractionConfidence.HIGH
            )
            coEvery { receiptExtractionService.extract(any()) } returns Result.success(extracted)

            viewModel.onEvent(DeveloperServicesUiEvent.RunExtraction)
            assertEquals(ExtractionStatus.Loading, viewModel.uiState.value.extractionStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Success, state.extractionStatus)
            assertEquals("50.00", state.extractedAmount)
            assertEquals("EUR", state.extractedCurrency)
            assertEquals("2026-05-20", state.extractedDate)
            assertEquals("Test Store", state.extractedTitle)
            assertEquals("AI_CORE", state.extractionSource)
            assertEquals("HIGH", state.extractionConfidence)
            assertNull(state.extractionErrorMessage)
        }

        @Test
        fun `failed extraction maps to error state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            prepareOcrSuccess(viewModel)

            val exception = RuntimeException("AICore model not available")
            coEvery { receiptExtractionService.extract(any()) } returns Result.failure(exception)

            viewModel.onEvent(DeveloperServicesUiEvent.RunExtraction)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Error, state.extractionStatus)
            assertEquals(UiText.DynamicString("AICore model not available"), state.extractionErrorMessage)
        }
    }

    @Nested
    @DisplayName("Reset Event")
    inner class ResetEvent {

        @Test
        fun `clears all state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val rawReceiptText = RawReceiptText(
                fullText = "Total amount: 50.00",
                blocks = persistentListOf(TextBlock(text = "Total amount: 50.00", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.Reset)

            val state = viewModel.uiState.value
            assertNull(state.selectedFileUri)
            assertEquals("", state.selectedFileName)
            assertEquals("", state.selectedFileMimeType)
            assertEquals(OcrStatus.Idle, state.ocrStatus)
            assertEquals("", state.extractedText)
            assertTrue(state.textBlocks.isEmpty())
            assertNull(state.errorMessage)
        }
    }
}
