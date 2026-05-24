package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptAutoFillEventHandlerTest {

    private lateinit var handler: ReceiptAutoFillEventHandler
    private lateinit var extractReceiptFieldsUseCase: ExtractReceiptFieldsUseCase
    private lateinit var receiptExtractionService: ReceiptExtractionService
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val capturedCurrencySelections = mutableListOf<String>()
    private val capturedAmountChanges = mutableListOf<String>()

    private val eur = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usd = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)

    @BeforeEach
    fun setUp() {
        extractReceiptFieldsUseCase = mockk()
        receiptExtractionService = mockk()
        formattingHelper = mockk(relaxed = true)
        capturedCurrencySelections.clear()
        capturedAmountChanges.clear()

        handler = ReceiptAutoFillEventHandler(
            extractReceiptFieldsUseCase = extractReceiptFieldsUseCase,
            receiptExtractionService = receiptExtractionService,
            formattingHelper = formattingHelper
        )
        handler.setOnCurrencySelected { capturedCurrencySelections.add(it) }
        handler.setOnAmountChanged { capturedAmountChanges.add(it) }

        uiState = MutableStateFlow(
            AddExpenseUiState(
                availableCurrencies = persistentListOf(eur, usd),
                selectedCurrency = eur
            )
        )
        actions = MutableSharedFlow(replay = 10)
        handler.bind(uiState, actions, TestScope(UnconfinedTestDispatcher()))
    }

    @Nested
    inner class SetAiModeActive {

        @Test
        fun `toggles mode active and updates state`() = runTest {
            uiState.value = uiState.value.copy(isAiModeActive = false)

            handler.handleSetAiModeActive(true)

            assertTrue(uiState.value.isAiModeActive)
        }

        @Test
        fun `navigates from RECEIPT to TITLE when disabling AI mode`() = runTest {
            uiState.value = uiState.value.copy(
                isAiModeActive = true,
                currentStep = AddExpenseStep.RECEIPT
            )

            handler.handleSetAiModeActive(false)

            assertFalse(uiState.value.isAiModeActive)
            assertEquals(AddExpenseStep.TITLE, uiState.value.currentStep)
        }

        @Test
        fun `navigates from TITLE to RECEIPT when enabling AI mode`() = runTest {
            uiState.value = uiState.value.copy(
                isAiCapable = true,
                isAiModeActive = false,
                currentStep = AddExpenseStep.TITLE
            )

            handler.handleSetAiModeActive(true)

            assertTrue(uiState.value.isAiModeActive)
            assertEquals(AddExpenseStep.RECEIPT, uiState.value.currentStep)
        }
    }

    @Nested
    inner class DismissAutoFillBanner {

        @Test
        fun `clears banner in state`() = runTest {
            uiState.value = uiState.value.copy(
                autoFillBanner = mockk()
            )

            handler.handleDismissAutoFillBanner()

            assertNull(uiState.value.autoFillBanner)
        }
    }

    @Nested
    inner class ReceiptAttachmentSuccess {

        private val attachment = ReceiptAttachment(
            localUri = "uri",
            mimeType = "image/webp",
            capturedAtMillis = 1000L
        )

        @Test
        fun `shows unavailable pill if device is not AI capable`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.UNSUPPORTED

            handler.handleReceiptAttached(attachment)

            val actionList = actions.replayCache
            assertTrue(actionList.any { it is AddExpenseUiAction.ShowPill })
        }

        @Test
        fun `extracts fields and merges them successfully`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Starbucks",
                    amount = BigDecimal("12.50"),
                    currency = "USD",
                    date = LocalDate.of(2025, 1, 1),
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )
            every { formattingHelper.formatCentsValue(1250L, 2) } returns "12.50"

            handler.handleReceiptAttached(attachment)

            assertEquals("Starbucks", uiState.value.expenseTitle)
            assertEquals("USD", capturedCurrencySelections.single())
            assertEquals("12.50", capturedAmountChanges.single())
            assertNotNull(uiState.value.autoFillBanner)
            assertEquals(ExtractionSource.AI_CORE, uiState.value.autoFillBanner?.source)
        }
    }
}
