package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.CategoryPredictionOutcome
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestion
import es.pedrazamiguez.splittrip.domain.model.CategorySuggestionSource
import es.pedrazamiguez.splittrip.domain.model.DeviceCompatibilityResult
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.LocalAiModelPackState
import es.pedrazamiguez.splittrip.domain.model.ModelPackCatalogEntry
import es.pedrazamiguez.splittrip.domain.model.ModelPackConfig
import es.pedrazamiguez.splittrip.domain.model.ModelPackStatus
import es.pedrazamiguez.splittrip.domain.model.ModelPackTier
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.expense.SuggestExpenseCategoryUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveDeveloperAccessUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.ObserveLocalAiModelPackStateUseCase
import es.pedrazamiguez.splittrip.features.settings.R
import es.pedrazamiguez.splittrip.features.settings.presentation.model.InstalledLocalAiPackInfo
import es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel.action.DeveloperServicesUiAction
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private lateinit var aiModelResolver: AiModelResolverService
    private lateinit var observeLocalAiModelPackStateUseCase: ObserveLocalAiModelPackStateUseCase
    private lateinit var suggestExpenseCategoryUseCase: SuggestExpenseCategoryUseCase
    private lateinit var observeDeveloperAccessUseCase: ObserveDeveloperAccessUseCase
    private val activeEngineFlow = MutableStateFlow(AiEngineType.AI_CORE_GEMMA_4)
    private val overrideEngineFlow = MutableStateFlow<AiEngineType?>(null)
    private val localAiModelPackStateFlow = MutableStateFlow(createLocalAiModelPackState())
    private val developerAccessFlow = MutableStateFlow(true)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        receiptOcrService = mockk()
        receiptExtractionService = mockk()
        aiModelResolver = mockk(relaxed = true)
        observeLocalAiModelPackStateUseCase = mockk()
        suggestExpenseCategoryUseCase = mockk()
        observeDeveloperAccessUseCase = mockk()
        activeEngineFlow.value = AiEngineType.AI_CORE_GEMMA_4
        overrideEngineFlow.value = null
        localAiModelPackStateFlow.value = createLocalAiModelPackState()
        developerAccessFlow.value = true
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { aiModelResolver.getDeveloperOverrideModel() } returns overrideEngineFlow
        every { observeLocalAiModelPackStateUseCase() } returns localAiModelPackStateFlow
        every { observeDeveloperAccessUseCase() } returns developerAccessFlow
        coEvery { suggestExpenseCategoryUseCase(any(), any()) } returns null
        coEvery { suggestExpenseCategoryUseCase.diagnose(any(), any()) } returns CategoryPredictionOutcome.NoMatch
        coEvery { aiModelResolver.setDeveloperOverrideModel(any()) } coAnswers {
            overrideEngineFlow.value = firstArg()
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
        aiModelResolver = aiModelResolver,
        observeLocalAiModelPackStateUseCase = observeLocalAiModelPackStateUseCase,
        suggestExpenseCategoryUseCase = suggestExpenseCategoryUseCase,
        observeDeveloperAccessUseCase = observeDeveloperAccessUseCase
    )

    private fun createCompatibilityResult() = DeviceCompatibilityResult(
        isCompatible = true,
        reason = null,
        availableRamMb = 8192L,
        requiredRamMb = 4096,
        availableStorageBytes = 10_000_000_000L,
        requiredStorageBytes = 2_000_000_000L,
        supportedAbis = listOf("arm64-v8a")
    )

    private fun createLocalAiModelPackState(
        status: ModelPackStatus = ModelPackStatus.NotInstalled(
            availableVersion = "1.0.0",
            downloadSizeBytes = 100L
        ),
        catalogEntries: List<ModelPackCatalogEntry> = emptyList(),
        installedPackId: String? = null
    ) = LocalAiModelPackState(
        status = status,
        compatibility = createCompatibilityResult(),
        isEntitled = true,
        isWifiOnly = true,
        isAiCoreSupported = false,
        activeEngine = AiEngineType.LITE_RT_LM,
        catalogEntries = catalogEntries,
        recommendedPackId = null,
        installedPackId = installedPackId
    )

    private fun createCatalogEntry(
        packId: String,
        displayName: String,
        tier: ModelPackTier,
        isInstalled: Boolean = true
    ): ModelPackCatalogEntry {
        val status = if (isInstalled) {
            ModelPackStatus.Installed(
                version = "1.0.0",
                sizeBytes = 100L,
                isUpdateAvailable = false,
                packId = packId
            )
        } else {
            ModelPackStatus.NotInstalled(
                availableVersion = "1.0.0",
                downloadSizeBytes = 100L,
                packId = packId
            )
        }
        return ModelPackCatalogEntry(
            config = ModelPackConfig(
                packId = packId,
                displayName = displayName,
                tier = tier,
                priority = tier.rank,
                version = "1.0.0",
                downloadUrl = "https://example.test/$packId.litertlm",
                sha256Checksum = "abc123",
                sizeBytes = 100L,
                minRamMb = 4096,
                minFreeStorageBytes = 2_000_000_000L,
                isEnabled = true
            ),
            compatibility = createCompatibilityResult(),
            status = status,
            isRecommended = false,
            isInstalled = isInstalled,
            isActiveDownload = false
        )
    }

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
        assertNull(state.extractedSubcategory)
        assertNull(state.extractedNotes)
        assertEquals(DeveloperServicesTab.Ocr, state.selectedTab)
        assertEquals(AiEngineType.AI_CORE_GEMMA_4, state.activeResolvedModel)
        assertNull(state.developerOverrideModel)
        assertNull(state.installedLocalAiPack)
        assertEquals("", state.categorySuggestionInput)
        assertEquals(GroupContext.TRAVEL, state.categorySuggestionContext)
        assertEquals(CategorySuggestionStatus.Idle, state.categorySuggestionStatus)
        assertNull(state.suggestedCategory)
        assertNull(state.suggestedSubcategory)
        assertNull(state.suggestedCategorySource)
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
            assertNull(state.extractedSubcategory)
            assertNull(state.extractedNotes)
        }

        @Test
        fun `clears previously populated extractedSubcategory back to null`() = runTest(testDispatcher) {
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
                fullText = "Total: 50.00 EUR",
                blocks = persistentListOf(TextBlock(text = "Total: 50.00 EUR", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            val extractedReceipt = ExtractedReceipt(
                amount = BigDecimal("50.00"),
                currency = "EUR",
                date = LocalDate.of(2025, 3, 10),
                time = LocalTime.of(13, 45),
                title = "Dinner",
                vendor = "Restaurant",
                category = "FOOD",
                subcategory = "RESTAURANT",
                paymentMethod = "CASH",
                notes = null,
                source = ExtractionSource.AI_CORE,
                confidence = ExtractionConfidence.HIGH
            )
            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            coEvery { receiptExtractionService.extract(rawReceiptText) } returns Result.success(extractedReceipt)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            advanceUntilIdle()
            assertEquals("RESTAURANT", viewModel.uiState.value.extractedSubcategory)

            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/456",
                    name = "receipt2.pdf",
                    mimeType = "application/pdf"
                )
            )
            assertNull(viewModel.uiState.value.extractedSubcategory)
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
        fun `clears all state but preserves selected tab, AI engine, pack info, and category state`() =
            runTest(testDispatcher) {
                val entry = createCatalogEntry(
                    "qwen2.5-1.5b-litert-v1",
                    "Balanced (Qwen 2.5 1.5B)",
                    ModelPackTier.BALANCED
                )
                localAiModelPackStateFlow.value = createLocalAiModelPackState(
                    status = entry.status,
                    catalogEntries = listOf(entry),
                    installedPackId = entry.config.packId
                )
                coEvery {
                    suggestExpenseCategoryUseCase.diagnose("Dinner", GroupContext.TRAVEL)
                } returns CategoryPredictionOutcome.Matched(
                    CategorySuggestion(
                        category = ExpenseCategory.FOOD,
                        subcategory = ExpenseSubcategory.RESTAURANT,
                        source = CategorySuggestionSource.LOCAL_RULES
                    )
                )

                val viewModel = createViewModel()
                advanceUntilIdle()
                viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))
                viewModel.onEvent(DeveloperServicesUiEvent.SelectModel(AiEngineType.LITE_RT_LM))
                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Dinner"))
                advanceUntilIdle()
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
                assertEquals(AiEngineType.LITE_RT_LM, state.developerOverrideModel)
                assertEquals("qwen2.5-1.5b-litert-v1", state.installedLocalAiPack?.packId)
                assertEquals("Dinner", state.categorySuggestionInput)
                assertEquals(ExpenseCategory.FOOD, state.suggestedCategory)
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

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.CategorySuggestion))
            assertEquals(DeveloperServicesTab.CategorySuggestion, viewModel.uiState.value.selectedTab)

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.Ocr))
            assertEquals(DeveloperServicesTab.Ocr, viewModel.uiState.value.selectedTab)
        }
    }

    @Nested
    @DisplayName("RunOcrAndExtract Event")
    inner class RunOcrAndExtractEvent {

        @Test
        fun `does nothing if no file is selected`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Idle, state.extractionStatus)
        }

        @Test
        fun `successful OCR and extraction updates extraction status to success`() = runTest(testDispatcher) {
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
                fullText = "Total: 50.00 EUR",
                blocks = persistentListOf(TextBlock(text = "Total: 50.00 EUR", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            val extractedReceipt = ExtractedReceipt(
                amount = BigDecimal("50.00"),
                currency = "EUR",
                date = LocalDate.of(2025, 3, 10),
                time = LocalTime.of(13, 45),
                title = "Dinner",
                vendor = "Restaurant",
                category = "FOOD",
                subcategory = "RESTAURANT",
                paymentMethod = "CASH",
                notes = "Nice food",
                source = ExtractionSource.AI_CORE,
                confidence = ExtractionConfidence.HIGH
            )

            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            coEvery { receiptExtractionService.extract(rawReceiptText) } returns Result.success(extractedReceipt)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)

            assertEquals(ExtractionStatus.Loading, viewModel.uiState.value.extractionStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Success, state.extractionStatus)
            assertEquals("50.00", state.extractedAmount)
            assertEquals("EUR", state.extractedCurrency)
            assertEquals("2025-03-10", state.extractedDate)
            assertEquals("13:45", state.extractedTime)
            assertEquals("Dinner", state.extractedTitle)
            assertEquals("Restaurant", state.extractedVendor)
            assertEquals("CASH", state.extractedPaymentMethod)
            assertEquals("FOOD", state.extractedCategory)
            assertEquals("RESTAURANT", state.extractedSubcategory)
            assertEquals("Nice food", state.extractedNotes)
            assertEquals(ExtractionSource.AI_CORE, state.extractionSource)
            assertEquals(ExtractionConfidence.HIGH, state.extractionConfidence)
        }

        @Test
        fun `failed OCR maps to extraction error`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(
                DeveloperServicesUiEvent.FileSelected(
                    uri = "content://media/external/file/123",
                    name = "receipt.pdf",
                    mimeType = "application/pdf"
                )
            )

            val exception = RuntimeException("OCR failed")
            coEvery { receiptOcrService.recogniseText(any()) } returns Result.failure(exception)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)

            assertEquals(ExtractionStatus.Loading, viewModel.uiState.value.extractionStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Error, state.extractionStatus)
            assertEquals(UiText.DynamicString("OCR failed"), state.extractionErrorMessage)
        }

        @Test
        fun `successful OCR but failed extraction maps to extraction error`() = runTest(testDispatcher) {
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
                fullText = "Total: 50.00 EUR",
                blocks = persistentListOf(TextBlock(text = "Total: 50.00 EUR", confidence = 1.0f)),
                recognisedAt = Instant.EPOCH
            )
            val exception = RuntimeException("Extraction failed")

            coEvery { receiptOcrService.recogniseText(any()) } returns Result.success(rawReceiptText)
            coEvery { receiptExtractionService.extract(rawReceiptText) } returns Result.failure(exception)

            viewModel.onEvent(DeveloperServicesUiEvent.RunOcrAndExtract)

            assertEquals(ExtractionStatus.Loading, viewModel.uiState.value.extractionStatus)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ExtractionStatus.Error, state.extractionStatus)
            assertEquals(UiText.DynamicString("Extraction failed"), state.extractionErrorMessage)
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

    @Nested
    @DisplayName("LocalAiModelPackObservation")
    inner class LocalAiModelPackObservation {

        @Test
        fun `observes installed catalog entry displayName and tier`() = runTest(testDispatcher) {
            val entry = createCatalogEntry(
                packId = "qwen2.5-1.5b-litert-v1",
                displayName = "Balanced (Qwen 2.5 1.5B)",
                tier = ModelPackTier.BALANCED
            )
            localAiModelPackStateFlow.value = createLocalAiModelPackState(
                status = entry.status,
                catalogEntries = listOf(entry),
                installedPackId = entry.config.packId
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(
                InstalledLocalAiPackInfo(
                    packId = "qwen2.5-1.5b-litert-v1",
                    displayName = "Balanced (Qwen 2.5 1.5B)",
                    tier = ModelPackTier.BALANCED
                ),
                viewModel.uiState.value.installedLocalAiPack
            )
        }

        @Test
        fun `falls back to packId when installed catalog entry has blank displayName`() = runTest(testDispatcher) {
            val entry = createCatalogEntry(
                packId = "qwen3-0.6b-litert-v1",
                displayName = "",
                tier = ModelPackTier.COMPACT
            )
            localAiModelPackStateFlow.value = createLocalAiModelPackState(
                status = entry.status,
                catalogEntries = listOf(entry),
                installedPackId = entry.config.packId
            )

            val viewModel = createViewModel()
            advanceUntilIdle()

            val packInfo = viewModel.uiState.value.installedLocalAiPack
            assertEquals("qwen3-0.6b-litert-v1", packInfo?.displayName)
            assertEquals(ModelPackTier.COMPACT, packInfo?.tier)
        }

        @Test
        fun `falls back to ModelPackStatus Installed packId with null tier when catalog is empty`() =
            runTest(testDispatcher) {
                localAiModelPackStateFlow.value = createLocalAiModelPackState(
                    status = ModelPackStatus.Installed(
                        version = "1.0.0",
                        sizeBytes = 100L,
                        isUpdateAvailable = false,
                        packId = "qwen3-0.6b-litert-v1"
                    ),
                    catalogEntries = emptyList()
                )

                val viewModel = createViewModel()
                advanceUntilIdle()

                assertEquals(
                    InstalledLocalAiPackInfo(
                        packId = "qwen3-0.6b-litert-v1",
                        displayName = "qwen3-0.6b-litert-v1",
                        tier = null
                    ),
                    viewModel.uiState.value.installedLocalAiPack
                )
            }

        @Test
        fun `clears installedLocalAiPack to null when pack is deleted`() = runTest(testDispatcher) {
            val entry = createCatalogEntry(
                packId = "qwen2.5-1.5b-litert-v1",
                displayName = "Balanced (Qwen 2.5 1.5B)",
                tier = ModelPackTier.BALANCED
            )
            localAiModelPackStateFlow.value = createLocalAiModelPackState(
                status = entry.status,
                catalogEntries = listOf(entry),
                installedPackId = entry.config.packId
            )
            val viewModel = createViewModel()
            advanceUntilIdle()
            assertEquals("qwen2.5-1.5b-litert-v1", viewModel.uiState.value.installedLocalAiPack?.packId)

            localAiModelPackStateFlow.value = createLocalAiModelPackState(
                status = ModelPackStatus.NotInstalled(
                    availableVersion = "1.0.0",
                    downloadSizeBytes = 100L
                ),
                catalogEntries = emptyList(),
                installedPackId = null
            )
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.installedLocalAiPack)
        }
    }

    @Nested
    @DisplayName("CategorySuggestion")
    inner class CategorySuggestionTests {

        @Test
        fun `blank or single-character title resets to Idle without invoking use case`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged(""))
            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("a"))
            advanceUntilIdle()

            assertEquals(CategorySuggestionStatus.Idle, viewModel.uiState.value.categorySuggestionStatus)
            coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
        }

        @Test
        fun `debounces rapid title updates and invokes use case only once with final trimmed title`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Ub"))
                advanceTimeBy(100)
                runCurrent()
                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Uber"))
                advanceTimeBy(100)
                runCurrent()
                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("  Uber airport  "))
                runCurrent()

                assertEquals(CategorySuggestionStatus.Loading, viewModel.uiState.value.categorySuggestionStatus)
                coVerify(exactly = 0) { suggestExpenseCategoryUseCase.diagnose(any(), any()) }

                advanceTimeBy(DeveloperServicesViewModel.CATEGORY_SUGGESTION_DEBOUNCE_MS)
                runCurrent()

                coVerify(exactly = 1) {
                    suggestExpenseCategoryUseCase.diagnose("Uber airport", GroupContext.TRAVEL)
                }
            }

        @Test
        fun `LOCAL_RULES match updates state to Matched with category, subcategory, and LOCAL_RULES source`() =
            runTest(testDispatcher) {
                coEvery {
                    suggestExpenseCategoryUseCase.diagnose("Uber", GroupContext.TRAVEL)
                } returns CategoryPredictionOutcome.Matched(
                    CategorySuggestion(
                        category = ExpenseCategory.TRANSPORT,
                        subcategory = ExpenseSubcategory.TAXI_RIDESHARE,
                        source = CategorySuggestionSource.LOCAL_RULES
                    )
                )

                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Uber"))
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(CategorySuggestionStatus.Matched, state.categorySuggestionStatus)
                assertEquals(ExpenseCategory.TRANSPORT, state.suggestedCategory)
                assertEquals(ExpenseSubcategory.TAXI_RIDESHARE, state.suggestedSubcategory)
                assertEquals(CategorySuggestionSource.LOCAL_RULES, state.suggestedCategorySource)
            }

        @Test
        fun `ON_DEVICE_AI match updates state to Matched with nullable subcategory and ON_DEVICE_AI source`() =
            runTest(testDispatcher) {
                coEvery {
                    suggestExpenseCategoryUseCase.diagnose("Museum visit", GroupContext.TRAVEL)
                } returns CategoryPredictionOutcome.Matched(
                    CategorySuggestion(
                        category = ExpenseCategory.ACTIVITIES,
                        subcategory = null,
                        source = CategorySuggestionSource.ON_DEVICE_AI
                    )
                )

                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Museum visit"))
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(CategorySuggestionStatus.Matched, state.categorySuggestionStatus)
                assertEquals(ExpenseCategory.ACTIVITIES, state.suggestedCategory)
                assertNull(state.suggestedSubcategory)
                assertEquals(CategorySuggestionSource.ON_DEVICE_AI, state.suggestedCategorySource)
            }

        @Test
        fun `NoMatch outcome updates state to NoMatch and clears prior matched result`() = runTest(testDispatcher) {
            coEvery {
                suggestExpenseCategoryUseCase.diagnose("Dinner", GroupContext.TRAVEL)
            } returns CategoryPredictionOutcome.Matched(
                CategorySuggestion(
                    category = ExpenseCategory.FOOD,
                    subcategory = ExpenseSubcategory.RESTAURANT,
                    source = CategorySuggestionSource.LOCAL_RULES
                )
            )
            coEvery {
                suggestExpenseCategoryUseCase.diagnose("Unknown xyz", GroupContext.TRAVEL)
            } returns CategoryPredictionOutcome.NoMatch

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Dinner"))
            advanceUntilIdle()
            assertEquals(CategorySuggestionStatus.Matched, viewModel.uiState.value.categorySuggestionStatus)

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Unknown xyz"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CategorySuggestionStatus.NoMatch, state.categorySuggestionStatus)
            assertNull(state.suggestedCategory)
            assertNull(state.suggestedSubcategory)
            assertNull(state.suggestedCategorySource)
        }

        @Test
        fun `AiUnavailable outcome updates state to Error with unavailable string resource`() =
            runTest(testDispatcher) {
                coEvery {
                    suggestExpenseCategoryUseCase.diagnose("Towels", GroupContext.EVERYDAY)
                } returns CategoryPredictionOutcome.AiUnavailable

                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryContextChanged(GroupContext.EVERYDAY))
                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Towels"))
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(CategorySuggestionStatus.Error, state.categorySuggestionStatus)
                assertEquals(
                    UiText.StringResource(R.string.developer_services_category_ai_unavailable),
                    state.categorySuggestionErrorMessage
                )
            }

        @Test
        fun `AiTimeout outcome updates state to Error with timeout seconds`() = runTest(testDispatcher) {
            coEvery {
                suggestExpenseCategoryUseCase.diagnose("Towels", GroupContext.EVERYDAY)
            } returns CategoryPredictionOutcome.AiTimeout(15_000L)

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryContextChanged(GroupContext.EVERYDAY))
            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Towels"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CategorySuggestionStatus.Error, state.categorySuggestionStatus)
            assertEquals(
                UiText.StringResource(R.string.developer_services_category_ai_timeout, 15L),
                state.categorySuggestionErrorMessage
            )
        }

        @Test
        fun `AiFailure outcome updates state to Error with failure message`() = runTest(testDispatcher) {
            coEvery {
                suggestExpenseCategoryUseCase.diagnose("Towels", GroupContext.EVERYDAY)
            } returns CategoryPredictionOutcome.AiFailure("AICore busy")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryContextChanged(GroupContext.EVERYDAY))
            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Towels"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CategorySuggestionStatus.Error, state.categorySuggestionStatus)
            assertEquals(UiText.DynamicString("AICore busy"), state.categorySuggestionErrorMessage)
        }

        @Test
        fun `exception from use case updates state to Error with error message`() = runTest(testDispatcher) {
            coEvery {
                suggestExpenseCategoryUseCase.diagnose("Coffee", GroupContext.TRAVEL)
            } throws IllegalStateException("Inference error")

            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Coffee"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CategorySuggestionStatus.Error, state.categorySuggestionStatus)
            assertEquals(UiText.DynamicString("Inference error"), state.categorySuggestionErrorMessage)
        }

        @Test
        fun `CategoryContextChanged re-triggers debounced prediction with new GroupContext`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Coffee"))
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryContextChanged(GroupContext.EVERYDAY))
                advanceUntilIdle()

                coVerify(exactly = 1) {
                    suggestExpenseCategoryUseCase.diagnose("Coffee", GroupContext.EVERYDAY)
                }
            }

        @Test
        fun `CategoryContextChanged with short title updates context without invoking use case`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryContextChanged(GroupContext.EVERYDAY))
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(GroupContext.EVERYDAY, state.categorySuggestionContext)
                assertEquals(CategorySuggestionStatus.Idle, state.categorySuggestionStatus)
                coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
            }

        @Test
        fun `ClearCategoryInput cancels pending debounced prediction and resets state to Idle`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.CategoryTitleChanged("Dinner"))
                advanceTimeBy(100)
                runCurrent()

                viewModel.onEvent(DeveloperServicesUiEvent.ClearCategoryInput)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(CategorySuggestionStatus.Idle, state.categorySuggestionStatus)
                assertEquals("", state.categorySuggestionInput)
                coVerify(exactly = 0) { suggestExpenseCategoryUseCase(any(), any()) }
            }
    }

    @Nested
    @DisplayName("DeveloperAccess")
    inner class DeveloperAccess {

        @Test
        fun `isDeveloperAccessGranted emits true and does not send NavigateBack when access is granted`() =
            runTest(testDispatcher) {
                developerAccessFlow.value = true
                val viewModel = createViewModel()
                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                advanceUntilIdle()

                assertEquals(true, viewModel.isDeveloperAccessGranted.value)
                assertTrue(actions.isEmpty())
                job.cancel()
            }

        @Test
        fun `isDeveloperAccessGranted emits false and sends NavigateBack when access is denied`() =
            runTest(testDispatcher) {
                developerAccessFlow.value = false
                val viewModel = createViewModel()
                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                advanceUntilIdle()

                assertEquals(false, viewModel.isDeveloperAccessGranted.value)
                assertEquals(listOf(DeveloperServicesUiAction.NavigateBack), actions)
                job.cancel()
            }

        @Test
        fun `revoking access while active updates isDeveloperAccessGranted to false and sends NavigateBack`() =
            runTest(testDispatcher) {
                developerAccessFlow.value = true
                val viewModel = createViewModel()
                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                advanceUntilIdle()
                assertEquals(true, viewModel.isDeveloperAccessGranted.value)
                assertTrue(actions.isEmpty())

                developerAccessFlow.value = false
                advanceUntilIdle()

                assertEquals(false, viewModel.isDeveloperAccessGranted.value)
                assertEquals(listOf(DeveloperServicesUiAction.NavigateBack), actions)
                job.cancel()
            }
    }
}
