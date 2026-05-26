package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var userPreferenceRepository: UserPreferenceRepository
    private val activeEngineFlow = MutableStateFlow(AiEngineType.AI_CORE_GEMMA_4)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        receiptOcrService = mockk()
        receiptExtractionService = mockk()
        userPreferenceRepository = mockk()
        every { userPreferenceRepository.getActiveAiEngine() } returns activeEngineFlow
        coEvery { userPreferenceRepository.setActiveAiEngine(any()) } coAnswers {
            activeEngineFlow.value = firstArg()
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
        unmockkAll()
    }

    private fun createViewModel() = DeveloperServicesViewModel(
        receiptOcrService = receiptOcrService,
        receiptExtractionService = receiptExtractionService,
        userPreferenceRepository = userPreferenceRepository
    )

    @Test
    fun `initial state is idle and empty`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()
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
        assertEquals(AiEngineType.AI_CORE_GEMMA_4, state.selectedAiEngine)
    }

    @Nested
    @DisplayName("FileSelected Event")
    inner class FileSelectedEvent {

        @Test
        fun `updates state with file details`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

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
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcr)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(OcrStatus.Idle, state.ocrStatus)
        }

        @Test
        fun `successful OCR maps to success state`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
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
            advanceUntilIdle()
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
            advanceUntilIdle()
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
        fun `updates selected tab`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))
            assertEquals(DeveloperServicesTab.AiExtraction, viewModel.uiState.value.selectedTab)

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AvatarGen))
            assertEquals(DeveloperServicesTab.AvatarGen, viewModel.uiState.value.selectedTab)

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.Ocr))
            assertEquals(DeveloperServicesTab.Ocr, viewModel.uiState.value.selectedTab)
        }
    }

    @Nested
    @DisplayName("SelectAiEngine Event")
    inner class SelectAiEngineEvent {

        @Test
        fun `updates selected AI engine and triggers repository save`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.SelectAiEngine(AiEngineType.LITE_RT_LM))
            advanceUntilIdle()

            assertEquals(AiEngineType.LITE_RT_LM, viewModel.uiState.value.selectedAiEngine)
            coVerify(exactly = 1) { userPreferenceRepository.setActiveAiEngine(AiEngineType.LITE_RT_LM) }
        }
    }
}
