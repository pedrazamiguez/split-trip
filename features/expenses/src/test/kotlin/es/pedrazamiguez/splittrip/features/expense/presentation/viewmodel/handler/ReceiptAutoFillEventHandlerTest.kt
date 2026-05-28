package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.ExtractedReceipt
import es.pedrazamiguez.splittrip.domain.model.ExtractionCapability
import es.pedrazamiguez.splittrip.domain.model.ExtractionConfidence
import es.pedrazamiguez.splittrip.domain.model.ExtractionSource
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptExtractionService
import es.pedrazamiguez.splittrip.domain.usecase.expense.ExtractReceiptFieldsUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
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
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val capturedCurrencySelections = mutableListOf<String>()
    private val capturedAmountChanges = mutableListOf<String>()
    private val capturedCategorySelections = mutableListOf<String>()
    private val capturedPaymentMethodSelections = mutableListOf<String>()

    private val eur = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usd = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)
    private val foodCategory = CategoryUiModel(id = "FOOD", displayText = "Food")
    private val cashPaymentMethod = PaymentMethodUiModel(
        id = "CASH",
        displayText = "Cash"
    )

    @BeforeEach
    fun setUp() {
        extractReceiptFieldsUseCase = mockk()
        receiptExtractionService = mockk()
        formattingHelper = mockk(relaxed = true)
        capturedCurrencySelections.clear()
        capturedAmountChanges.clear()
        capturedCategorySelections.clear()
        capturedPaymentMethodSelections.clear()

        addExpenseUiMapper = mockk(relaxed = true)
        handler = ReceiptAutoFillEventHandler(
            extractReceiptFieldsUseCase = extractReceiptFieldsUseCase,
            receiptExtractionService = receiptExtractionService,
            formattingHelper = formattingHelper,
            addExpenseUiMapper = addExpenseUiMapper
        )
        handler.setOnCurrencySelected { capturedCurrencySelections.add(it) }
        handler.setOnAmountChanged { capturedAmountChanges.add(it) }
        handler.setOnCategorySelected { capturedCategorySelections.add(it) }
        handler.setOnPaymentMethodSelected { capturedPaymentMethodSelections.add(it) }

        uiState = MutableStateFlow(
            AddExpenseUiState(
                availableCurrencies = persistentListOf(eur, usd),
                selectedCurrency = eur,
                availableCategories = persistentListOf(foodCategory),
                paymentMethods = persistentListOf(cashPaymentMethod),
                isAiModeActive = true
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
                    time = java.time.LocalTime.of(10, 30),
                    vendor = "Starbucks Store",
                    category = "FOOD",
                    paymentMethod = "CASH",
                    notes = "my locator",
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )
            every { formattingHelper.formatCentsValue(1250L, 2) } returns "12.50"
            every { addExpenseUiMapper.formatExpenseDateForDisplay(1735727400000L) } returns "Jan 1, 2025, 10:30 AM"

            handler.handleReceiptAttached(attachment)

            assertEquals("Starbucks", uiState.value.expenseTitle)
            assertEquals("Starbucks Store", uiState.value.vendor)
            assertEquals("my locator", uiState.value.notes)
            // 2025-01-01T10:30 UTC epoch millis = 1735727400000L
            assertEquals(1735727400000L, uiState.value.expenseDateMillis)
            assertEquals("Jan 1, 2025, 10:30 AM", uiState.value.formattedExpenseDate)
            assertEquals("USD", capturedCurrencySelections.single())
            assertEquals("12.50", capturedAmountChanges.single())
            assertEquals("FOOD", capturedCategorySelections.single())
            assertEquals("CASH", capturedPaymentMethodSelections.single())
            assertNotNull(uiState.value.autoFillBanner)
            assertEquals(ExtractionSource.AI_CORE, uiState.value.autoFillBanner?.source)
            assertTrue(
                uiState.value.autoFillBanner?.fields?.contains(
                    UiText.StringResource(R.string.expense_field_notes)
                ) == true
            )
        }

        @Test
        fun `extracts category even when a different category is already selected`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Theater Ticket",
                    amount = BigDecimal("45.00"),
                    currency = "EUR",
                    date = LocalDate.of(2025, 1, 1),
                    time = java.time.LocalTime.of(19, 0),
                    vendor = "Teatro Lope de Vega",
                    category = "ENTERTAINMENT",
                    paymentMethod = "CREDIT_CARD",
                    notes = "Localizador: XYZ123",
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )
            every { formattingHelper.formatCentsValue(4500L, 2) } returns "45.00"

            val otherCategory = CategoryUiModel(id = "OTHER", displayText = "Other")
            val entertainmentCategory = CategoryUiModel(id = "ENTERTAINMENT", displayText = "Entertainment")
            uiState.value = uiState.value.copy(
                availableCategories = persistentListOf(foodCategory, otherCategory, entertainmentCategory),
                selectedCategory = foodCategory
            )

            handler.handleReceiptAttached(attachment)

            assertEquals("ENTERTAINMENT", capturedCategorySelections.single())
            assertTrue(
                uiState.value.autoFillBanner?.fields?.contains(
                    UiText.StringResource(R.string.add_expense_category_title)
                ) == true
            )
        }

        @Test
        fun `does not extract fields when AI mode is disabled`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            uiState.value = uiState.value.copy(isAiModeActive = false)

            handler.handleReceiptAttached(attachment)

            io.mockk.coVerify(exactly = 0) { extractReceiptFieldsUseCase(any()) }
        }

        @Test
        fun `does not overwrite pre-populated values`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Starbucks",
                    amount = BigDecimal("12.50"),
                    currency = "USD",
                    date = LocalDate.of(2025, 1, 1),
                    time = java.time.LocalTime.of(10, 30),
                    vendor = "Starbucks Store",
                    category = "FOOD",
                    paymentMethod = "CASH",
                    notes = "my locator",
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )

            uiState.value = uiState.value.copy(
                expenseTitle = "Pre-populated Title",
                vendor = "Pre-populated Vendor",
                notes = "Pre-populated Notes",
                sourceAmount = "10.00",
                expenseDateMillis = 9999L,
                isExpenseDateModifiedByUser = true,
                selectedCurrency = usd,
                selectedPaymentMethod = cashPaymentMethod
            )

            handler.handleReceiptAttached(attachment)

            assertEquals("Pre-populated Title", uiState.value.expenseTitle)
            assertEquals("Pre-populated Vendor", uiState.value.vendor)
            assertEquals("Pre-populated Notes", uiState.value.notes)
            assertEquals(9999L, uiState.value.expenseDateMillis)
            assertTrue(capturedCurrencySelections.isEmpty())
            assertTrue(capturedAmountChanges.isEmpty())
            assertTrue(capturedPaymentMethodSelections.isEmpty())
        }

        @Test
        fun `does not overwrite payment method if user changed it from default before scan`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Starbucks",
                    amount = BigDecimal("12.50"),
                    currency = "USD",
                    date = LocalDate.of(2025, 1, 1),
                    time = java.time.LocalTime.of(10, 30),
                    vendor = "Starbucks Store",
                    category = "FOOD",
                    paymentMethod = "CREDIT_CARD",
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )

            val creditCardPaymentMethod = PaymentMethodUiModel(
                id = "CREDIT_CARD",
                displayText = "Credit Card"
            )

            uiState.value = uiState.value.copy(
                paymentMethods = persistentListOf(cashPaymentMethod, creditCardPaymentMethod),
                selectedPaymentMethod = creditCardPaymentMethod
            )

            handler.handleReceiptAttached(attachment)

            assertTrue(capturedPaymentMethodSelections.isEmpty())
        }

        @Test
        fun `does not overwrite fields changed while extraction is in-flight`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } answers {
                uiState.value = uiState.value.copy(
                    expenseTitle = "In-flight Title",
                    vendor = "In-flight Vendor",
                    notes = "In-flight Notes",
                    expenseDateMillis = 8888L,
                    isExpenseDateModifiedByUser = true,
                    selectedCurrency = usd,
                    selectedPaymentMethod = cashPaymentMethod
                )
                Result.success(
                    ExtractedReceipt(
                        title = "Extracted Title",
                        amount = BigDecimal("12.50"),
                        currency = "EUR",
                        date = LocalDate.of(2025, 1, 1),
                        time = java.time.LocalTime.of(10, 30),
                        vendor = "Extracted Vendor",
                        category = "FOOD",
                        paymentMethod = "CREDIT_CARD",
                        notes = "Extracted Notes",
                        confidence = ExtractionConfidence.HIGH,
                        source = ExtractionSource.AI_CORE
                    )
                )
            }

            uiState.value = uiState.value.copy(
                expenseTitle = "",
                vendor = "",
                notes = "",
                expenseDateMillis = null,
                selectedCurrency = eur,
                selectedPaymentMethod = cashPaymentMethod
            )

            handler.handleReceiptAttached(attachment)

            assertEquals("In-flight Title", uiState.value.expenseTitle)
            assertEquals("In-flight Vendor", uiState.value.vendor)
            assertEquals("In-flight Notes", uiState.value.notes)
            assertEquals(8888L, uiState.value.expenseDateMillis)
            assertTrue(capturedCurrencySelections.isEmpty())
            assertTrue(capturedPaymentMethodSelections.isEmpty())
        }

        @Test
        fun `rounds extracted amount and handles overflow safely`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Starbucks",
                    amount = BigDecimal("12.34567"),
                    currency = "EUR",
                    date = LocalDate.of(2025, 1, 1),
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )
            every { formattingHelper.formatCentsValue(1235L, 2) } returns "12.35"

            handler.handleReceiptAttached(attachment)

            assertEquals("12.35", capturedAmountChanges.single())
        }

        @Test
        fun `handles extracted amount overflow safely`() = runTest {
            every { receiptExtractionService.capability() } returns ExtractionCapability.ON_DEVICE_AI
            coEvery { extractReceiptFieldsUseCase(attachment) } returns Result.success(
                ExtractedReceipt(
                    title = "Starbucks",
                    amount = BigDecimal("92233720368547758079223372036854775807"),
                    currency = "EUR",
                    date = LocalDate.of(2025, 1, 1),
                    confidence = ExtractionConfidence.HIGH,
                    source = ExtractionSource.AI_CORE
                )
            )

            handler.handleReceiptAttached(attachment)

            assertTrue(capturedAmountChanges.isEmpty())
        }
    }
}
