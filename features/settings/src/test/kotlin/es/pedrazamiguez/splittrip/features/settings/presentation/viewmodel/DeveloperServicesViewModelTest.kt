package es.pedrazamiguez.splittrip.features.settings.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.domain.enums.AiEngineType
import es.pedrazamiguez.splittrip.domain.enums.SubscriptionTier
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.RawReceiptText
import es.pedrazamiguez.splittrip.domain.model.TextBlock
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AiModelResolverService
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.service.ReceiptOcrService
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ObserveCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserTierUseCase
import es.pedrazamiguez.splittrip.features.settings.R
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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
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
    private lateinit var observeCurrentUserProfileUseCase: ObserveCurrentUserProfileUseCase
    private lateinit var getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase
    private lateinit var updateUserTierUseCase: UpdateUserTierUseCase
    private val activeEngineFlow = MutableStateFlow(AiEngineType.AI_CORE_GEMMA_4)
    private val overrideEngineFlow = MutableStateFlow<AiEngineType?>(null)
    private val userProfileFlow = MutableStateFlow<User?>(null)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        receiptOcrService = mockk()
        receiptExtractionService = mockk()
        aiModelResolver = mockk(relaxed = true)
        observeCurrentUserProfileUseCase = mockk()
        getCurrentUserProfileUseCase = mockk()
        updateUserTierUseCase = mockk()
        userProfileFlow.value = null
        every { aiModelResolver.getActiveModel() } returns activeEngineFlow
        every { aiModelResolver.getDeveloperOverrideModel() } returns overrideEngineFlow
        every { observeCurrentUserProfileUseCase() } returns userProfileFlow
        coEvery { getCurrentUserProfileUseCase() } returns null
        coEvery { updateUserTierUseCase(any(), any()) } returns Result.success(Unit)
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
        observeCurrentUserProfileUseCase = observeCurrentUserProfileUseCase,
        getCurrentUserProfileUseCase = getCurrentUserProfileUseCase,
        updateUserTierUseCase = updateUserTierUseCase
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
        assertEquals(AiEngineType.AI_CORE_GEMMA_4, state.activeResolvedModel)
        assertNull(state.developerOverrideModel)
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
        fun `clears all state but preserves selected tab and AI engine`() = runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()
            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.AiExtraction))
            viewModel.onEvent(DeveloperServicesUiEvent.SelectModel(AiEngineType.LITE_RT_LM))
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

            viewModel.onEvent(DeveloperServicesUiEvent.SwitchTab(DeveloperServicesTab.Billing))
            assertEquals(DeveloperServicesTab.Billing, viewModel.uiState.value.selectedTab)

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
    @DisplayName("BillingTab")
    inner class BillingTab {

        private fun createUser(tier: SubscriptionTier = SubscriptionTier.FREE) = User(
            userId = "user_1",
            email = "dev@splittrip.test",
            displayName = "Dev User",
            tier = tier
        )

        @Test
        fun `initial state observes current user tier as PRO when user is PRO`() = runTest(testDispatcher) {
            userProfileFlow.value = createUser(SubscriptionTier.PRO)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
            assertTrue(viewModel.uiState.value.hasSignedInUser)
            assertNull(viewModel.uiState.value.billingErrorMessage)
        }

        @Test
        fun `initial state observes current user tier as FREE when user is FREE`() = runTest(testDispatcher) {
            userProfileFlow.value = createUser(SubscriptionTier.FREE)

            val viewModel = createViewModel()
            advanceUntilIdle()

            assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.currentTier)
            assertTrue(viewModel.uiState.value.hasSignedInUser)
            assertNull(viewModel.uiState.value.billingErrorMessage)
        }

        @Test
        fun `SetSubscriptionTier FREE invokes updateUserTierUseCase with FREE, updates state, and emits ShowTopPill`() =
            runTest(testDispatcher) {
                userProfileFlow.value = createUser(SubscriptionTier.PRO)
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.FREE))
                advanceUntilIdle()

                coVerify(exactly = 1) { updateUserTierUseCase("user_1", SubscriptionTier.FREE) }
                assertEquals(SubscriptionTier.FREE, viewModel.uiState.value.currentTier)
                assertFalse(viewModel.uiState.value.isUpdatingTier)
                assertNull(viewModel.uiState.value.pendingTargetTier)
                assertNull(viewModel.uiState.value.billingErrorMessage)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(DeveloperServicesUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.developer_services_billing_tier_updated, message.resId)
                assertEquals(listOf("FREE"), message.args.toList())

                job.cancel()
            }

        @Test
        fun `SetSubscriptionTier PRO invokes updateUserTierUseCase with PRO, updates state, and emits ShowTopPill`() =
            runTest(testDispatcher) {
                userProfileFlow.value = createUser(SubscriptionTier.FREE)
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.PRO))
                advanceUntilIdle()

                coVerify(exactly = 1) { updateUserTierUseCase("user_1", SubscriptionTier.PRO) }
                assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
                assertFalse(viewModel.uiState.value.isUpdatingTier)
                assertNull(viewModel.uiState.value.pendingTargetTier)
                assertNull(viewModel.uiState.value.billingErrorMessage)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(DeveloperServicesUiAction.ShowTopPill::class.java, actions.first())
                val message = assertInstanceOf(UiText.StringResource::class.java, action.message)
                assertEquals(R.string.developer_services_billing_tier_updated, message.resId)
                assertEquals(listOf("PRO"), message.args.toList())

                job.cancel()
            }

        @Test
        fun `SetSubscriptionTier falls back to getCurrentUserProfileUseCase if stream has not emitted userId yet`() =
            runTest(testDispatcher) {
                coEvery { getCurrentUserProfileUseCase() } returns createUser(SubscriptionTier.FREE)
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.PRO))
                advanceUntilIdle()

                coVerify(exactly = 1) { getCurrentUserProfileUseCase() }
                coVerify(exactly = 1) { updateUserTierUseCase("user_1", SubscriptionTier.PRO) }
                assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
            }

        @Test
        fun `SetSubscriptionTier emits ShowTopPill and sets billingErrorMessage when no user is signed in`() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val actions = mutableListOf<DeveloperServicesUiAction>()
                val job = launch { viewModel.actions.collect { actions.add(it) } }

                viewModel.onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.PRO))
                advanceUntilIdle()

                coVerify(exactly = 0) { updateUserTierUseCase(any(), any()) }
                assertFalse(viewModel.uiState.value.isUpdatingTier)
                assertNull(viewModel.uiState.value.pendingTargetTier)
                val error = assertInstanceOf(
                    UiText.StringResource::class.java,
                    viewModel.uiState.value.billingErrorMessage
                )
                assertEquals(R.string.developer_services_billing_no_user_error, error.resId)
                assertEquals(1, actions.size)
                val action = assertInstanceOf(DeveloperServicesUiAction.ShowTopPill::class.java, actions.first())
                assertEquals(error, action.message)

                job.cancel()
            }

        @Test
        fun `SetSubscriptionTier emits ShowTopPill and sets error when updateUserTierUseCase fails`() = runTest(
            testDispatcher
        ) {
            userProfileFlow.value = createUser(SubscriptionTier.PRO)
            coEvery {
                updateUserTierUseCase("user_1", SubscriptionTier.FREE)
            } returns Result.failure(IllegalStateException("Firestore unavailable"))

            val viewModel = createViewModel()
            advanceUntilIdle()

            val actions = mutableListOf<DeveloperServicesUiAction>()
            val job = launch { viewModel.actions.collect { actions.add(it) } }

            viewModel.onEvent(DeveloperServicesUiEvent.SetSubscriptionTier(SubscriptionTier.FREE))
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isUpdatingTier)
            assertNull(viewModel.uiState.value.pendingTargetTier)
            assertEquals(SubscriptionTier.PRO, viewModel.uiState.value.currentTier)
            val error = assertInstanceOf(
                UiText.DynamicString::class.java,
                viewModel.uiState.value.billingErrorMessage
            )
            assertEquals("Firestore unavailable", error.value)
            assertEquals(1, actions.size)
            val action = assertInstanceOf(DeveloperServicesUiAction.ShowTopPill::class.java, actions.first())
            assertEquals(error, action.message)

            job.cancel()
        }
    }
}
