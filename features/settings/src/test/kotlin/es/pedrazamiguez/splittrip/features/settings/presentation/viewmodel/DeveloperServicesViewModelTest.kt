package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.model.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.service.AiModelResolver
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
    private lateinit var aiModelResolver: AiModelResolver

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        receiptOcrService = mockk()
        receiptExtractionService = mockk()
        aiModelResolver = mockk(relaxed = true)
        every { aiModelResolver.getActiveModel() } returns flowOf(AiEngineType.AI_CORE_GEMMA_4)
        every { aiModelResolver.getDeveloperOverrideModel() } returns flowOf(null)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DeveloperServicesViewModel(
        receiptOcrService = receiptOcrService,
        receiptExtractionService = receiptExtractionService,
        aiModelResolver = aiModelResolver
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
        assertNull(state.extractedTime)
        assertNull(state.extractedVendor)
        assertNull(state.extractedPaymentMethod)
        assertNull(state.extractedCategory)
        assertNull(state.extractedNotes)
        assertEquals(DeveloperServicesTab.Ocr, state.selectedTab)
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
            assertNull(state.extractedTime)
            assertNull(state.extractedVendor)
            assertNull(state.extractedPaymentMethod)
            assertNull(state.extractedCategory)
            assertNull(state.extractedNotes)
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
    @DisplayName("Reset Event")
    inner class ResetEvent {

        @Test
        fun `clears all state but preserves selected tab`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))
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
            assertEquals(DeveloperServicesTab.AiExtraction, state.selectedTab)
        }
    }

    @Nested
    @DisplayName("SwitchTab Event")
    inner class SwitchTabEvent {

        @Test
        fun `updates selected tab`() {
            val viewModel = createViewModel()

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))
            assertEquals(DeveloperServicesTab.AiExtraction, viewModel.uiState.value.selectedTab)

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AvatarGen))
            assertEquals(DeveloperServicesTab.AvatarGen, viewModel.uiState.value.selectedTab)

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.Ocr))
            assertEquals(DeveloperServicesTab.Ocr, viewModel.uiState.value.selectedTab)
        }

        @Test
        fun `preserves other state when switching tabs`() {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))

            val state = viewModel.uiState.value
            assertEquals("content://media/external/file/123", state.selectedFileUri)
            assertEquals(DeveloperServicesTab.AiExtraction, state.selectedTab)
        }
    }

    @Nested
    @DisplayName("RunOcrAndExtract Event")
    inner class RunOcrAndExtractEvent {

        @Test
        fun `does nothing if no file is selected`() = runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            advanceUntilIdle()

            assertEquals(ExtractionStatus.Idle, viewModel.uiState.value.extractionStatus)
        }

        @Test
        fun `sets loading then success on OCR and extraction success`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val rawReceiptText = RawReceiptText(
                fullText = "Total 991.00 THB",
                blocks = persistentListOf(TextBlock(text = "Total 991.00 THB", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            val extracted = ExtractedReceipt(
                amount = BigDecimal("991.00"),
                currency = "THB",
                date = LocalDate.of(2026, 5, 20),
                time = java.time.LocalTime.of(12, 34),
                title = "Snacks",
                vendor = "7-Eleven Store",
                category = "FOOD",
                paymentMethod = "CASH",
                notes = "my notes",
                source = ExtractionSource.AI_CORE,
                confidence = ExtractionConfidence.HIGH
            )

            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            coEvery { receiptExtractionService.extract(any()) } returns Result.success(extracted)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            assertEquals(ExtractionStatus.Loading, viewModel.uiState.value.extractionStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Success, state.extractionStatus)
            assertEquals("991.00", state.extractedAmount)
            assertEquals("THB", state.extractedCurrency)
            assertEquals("2026-05-20", state.extractedDate)
            assertEquals("12:34", state.extractedTime)
            assertEquals("Snacks", state.extractedTitle)
            assertEquals("7-Eleven Store", state.extractedVendor)
            assertEquals("CASH", state.extractedPaymentMethod)
            assertEquals("FOOD", state.extractedCategory)
            assertEquals("my notes", state.extractedNotes)
            assertEquals(OcrStatus.Idle, state.ocrStatus)
        }

        @Test
        fun `maps OCR failure to extraction error state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            coEvery { receiptOcrService.recogniseText(any()) } returns Result.failure(RuntimeException("OCR failed"))

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Error, state.extractionStatus)
            assertEquals(UiText.DynamicString("OCR failed"), state.extractionErrorMessage)
            assertEquals(OcrStatus.Idle, state.ocrStatus)
        }
    }

    @Nested
    @DisplayName("SelectModel Event")
    inner class SelectModelEvent {

        @Test
        fun `SelectModel event calls setDeveloperOverrideModel`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            coEvery { aiModelResolver.setDeveloperOverrideModel(any()) } returns Unit

            viewModel.onEvent(DeveloperServicesUiEvent.SelectModel(AiEngineType.LITE_RT_LM))
            advanceUntilIdle()

            coVerify { aiModelResolver.setDeveloperOverrideModel(AiEngineType.LITE_RT_LM) }
        }
    }
}
