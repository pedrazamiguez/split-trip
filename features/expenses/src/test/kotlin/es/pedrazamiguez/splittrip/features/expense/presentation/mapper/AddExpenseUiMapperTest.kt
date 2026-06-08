package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.impl.RemainderDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitTypeUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.EntitySplitFlattenDelegate
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AddExpenseUiMapperTest {

    private lateinit var mapper: AddExpenseUiMapper
    private lateinit var addOnMapper: AddExpenseAddOnUiMapper
    private lateinit var splitMapper: AddExpenseSplitUiMapper
    private lateinit var localeProvider: LocaleProvider
    private lateinit var resourceProvider: ResourceProvider

    // UI Models for test state construction
    private val eurUi = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usdUi = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)
    private val jpyUi = CurrencyUiModel(code = "JPY", displayText = "JPY (¥)", decimalDigits = 0)
    private val tndUi = CurrencyUiModel(code = "TND", displayText = "TND (د.ت)", decimalDigits = 3)

    private val cashPaymentMethod = PaymentMethodUiModel(id = "CASH", displayText = "Cash")
    private val creditCardPaymentMethod = PaymentMethodUiModel(id = "CREDIT_CARD", displayText = "Credit Card")
    private val debitCardPaymentMethod = PaymentMethodUiModel(id = "DEBIT_CARD", displayText = "Debit Card")
    private val bankTransferPaymentMethod = PaymentMethodUiModel(id = "BANK_TRANSFER", displayText = "Transfer")

    @BeforeEach
    fun setup() {
        localeProvider = mockk()
        resourceProvider = mockk(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val formattingHelper =
            es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper(
                localeProvider
            )
        val splitPreviewService = SplitPreviewServiceImpl()
        val remainderDistributionService = RemainderDistributionServiceImpl()
        splitMapper = AddExpenseSplitUiMapper(
            localeProvider,
            formattingHelper,
            splitPreviewService,
            EntitySplitFlattenDelegate(splitPreviewService, remainderDistributionService)
        )
        addOnMapper = AddExpenseAddOnUiMapper()
        mapper = AddExpenseUiMapper(
            localeProvider,
            resourceProvider,
            splitMapper,
            addOnMapper,
            splitPreviewService
        )
    }

    @Nested
    inner class MapToDomain {

        @Test
        fun `maps basic expense with same currency`() {
            val state = AddExpenseUiState(
                expenseTitle = "Lunch",
                sourceAmount = "10.50",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals("group-123", expense.groupId)
            assertEquals("Lunch", expense.title)
            assertEquals(1050L, expense.sourceAmount)
            assertEquals("EUR", expense.sourceCurrency)
            assertEquals(1050L, expense.groupAmount)
            assertEquals("EUR", expense.groupCurrency)
            assertEquals(0, BigDecimal.ONE.compareTo(expense.exchangeRate))
            assertEquals(PaymentMethod.CASH, expense.paymentMethod)
        }

        @Test
        fun `maps expense with different currencies and explicit group amount`() {
            val state = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "100.00",
                selectedCurrency = usdUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.086956522",
                calculatedGroupAmount = "92.00",
                selectedPaymentMethod = creditCardPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-456")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(10000L, expense.sourceAmount)
            assertEquals("USD", expense.sourceCurrency)
            assertEquals(9200L, expense.groupAmount)
            assertEquals("EUR", expense.groupCurrency)
            assertEquals(
                0,
                BigDecimal("0.92").compareTo(expense.exchangeRate.setScale(2, java.math.RoundingMode.HALF_UP))
            )
        }

        @Test
        fun `calculates group amount when not explicitly set`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                selectedCurrency = usdUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.086956522",
                calculatedGroupAmount = "",
                selectedPaymentMethod = debitCardPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-789")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(500L, expense.sourceAmount)
            assertEquals(460L, expense.groupAmount)
        }

        @Test
        fun `handles European format with comma decimal`() {
            val state = AddExpenseUiState(
                expenseTitle = "Museum",
                sourceAmount = "15,50",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(1550L, expense.sourceAmount)
        }

        @Test
        fun `handles US format with thousand separator`() {
            val state = AddExpenseUiState(
                expenseTitle = "Hotel",
                sourceAmount = "1,250.00",
                selectedCurrency = usdUi,
                groupCurrency = usdUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = creditCardPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(125000L, expense.sourceAmount)
        }

        @Test
        fun `handles European format with thousand separator`() {
            val state = AddExpenseUiState(
                expenseTitle = "Rent",
                sourceAmount = "1.250,00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = bankTransferPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(125000L, expense.sourceAmount)
        }
    }

    @Nested
    inner class CurrencyDecimalPlaces {

        @Test
        fun `handles JPY with 0 decimal places`() {
            val state = AddExpenseUiState(
                expenseTitle = "Sushi",
                sourceAmount = "1500",
                selectedCurrency = jpyUi,
                groupCurrency = jpyUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(1500L, expense.sourceAmount)
            assertEquals("JPY", expense.sourceCurrency)
        }

        @Test
        fun `handles TND with 3 decimal places`() {
            val state = AddExpenseUiState(
                expenseTitle = "Taxi",
                sourceAmount = "10.500",
                selectedCurrency = tndUi,
                groupCurrency = tndUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(10500L, expense.sourceAmount)
            assertEquals("TND", expense.sourceCurrency)
        }

        @Test
        fun `converts between currencies with different decimal places`() {
            val state = AddExpenseUiState(
                expenseTitle = "Exchange",
                sourceAmount = "1000",
                selectedCurrency = jpyUi,
                groupCurrency = eurUi,
                displayExchangeRate = "149.2537313",
                calculatedGroupAmount = "6.70",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(1000L, expense.sourceAmount)
            assertEquals("JPY", expense.sourceCurrency)
            assertEquals(670L, expense.groupAmount)
            assertEquals("EUR", expense.groupCurrency)
            assertEquals(
                0,
                BigDecimal("0.0067").compareTo(expense.exchangeRate.setScale(4, java.math.RoundingMode.HALF_UP))
            )
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `handles empty amount as zero`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(0L, expense.sourceAmount)
        }

        @Test
        fun `handles whitespace amount as zero`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "   ",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(0L, expense.sourceAmount)
        }

        @Test
        fun `uses default EUR when currency is null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = null,
                groupCurrency = null,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals("EUR", expense.sourceCurrency)
            assertEquals("EUR", expense.groupCurrency)
            assertEquals(1000L, expense.sourceAmount)
        }

        @Test
        fun `uses default rate of 1 when exchange rate is invalid`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "invalid",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(0, BigDecimal.ONE.compareTo(expense.exchangeRate))
        }

        @Test
        fun `handles amount with leading and trailing whitespace`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "  25.99  ",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(2599L, expense.sourceAmount)
        }

        @Test
        fun `uses CASH when payment method is null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PaymentMethod.CASH, expense.paymentMethod)
        }
    }

    @Nested
    inner class FormatDueDateForDisplay {

        @Test
        fun `formats UTC millis using US locale`() {
            // 2026-03-16 00:00:00 UTC in millis
            val millis = 1773619200000L

            val result = mapper.formatDueDateForDisplay(millis)

            // US locale MEDIUM format: "Mar 16, 2026"
            assertEquals("Mar 16, 2026", result)
        }

        @Test
        fun `formats UTC millis using Spanish locale`() {
            every { localeProvider.getCurrentLocale() } returns Locale.forLanguageTag("es-ES")
            // 2026-03-16 00:00:00 UTC in millis
            val millis = 1773619200000L

            val result = mapper.formatDueDateForDisplay(millis)

            // Spanish locale MEDIUM format: "16 mar 2026"
            assertTrue(result.contains("16"))
            assertTrue(result.contains("2026"))
        }
    }

    @Nested
    inner class MapToDomainExtended {

        @Test
        fun `maps category from selected category UI model`() {
            val state = AddExpenseUiState(
                expenseTitle = "Groceries",
                sourceAmount = "50.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedCategory = CategoryUiModel(id = "FOOD", displayText = "Food")
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals(ExpenseCategory.FOOD, result.getOrThrow().category)
        }

        @Test
        fun `defaults category to OTHER when null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedCategory = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals(ExpenseCategory.OTHER, result.getOrThrow().category)
        }

        @Test
        fun `maps vendor from state`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                vendor = "Starbucks",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals("Starbucks", result.getOrThrow().vendor)
        }

        @Test
        fun `maps blank vendor to null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                vendor = "   ",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().vendor)
        }

        @Test
        fun `maps notes from state`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                notes = "Shared with team",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals("Shared with team", result.getOrThrow().notes)
        }

        @Test
        fun `maps blank notes to null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                notes = "   ",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().notes)
        }

        @Test
        fun `maps empty notes to null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                notes = "",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().notes)
        }

        @Test
        fun `maps payment status from selected status UI model`() {
            val state = AddExpenseUiState(
                expenseTitle = "Bill",
                sourceAmount = "100.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedPaymentStatus = PaymentStatusUiModel(id = "SCHEDULED", displayText = "Scheduled")
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals(PaymentStatus.SCHEDULED, result.getOrThrow().paymentStatus)
        }

        @Test
        fun `defaults payment status to FINISHED when null`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedPaymentStatus = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals(PaymentStatus.FINISHED, result.getOrThrow().paymentStatus)
        }

        @Test
        fun `maps due date when status is SCHEDULED and millis present`() {
            // 2026-03-16 00:00:00 UTC
            val millis = 1773619200000L
            val state = AddExpenseUiState(
                expenseTitle = "Rent",
                sourceAmount = "500.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedPaymentStatus = PaymentStatusUiModel(id = "SCHEDULED", displayText = "Scheduled"),
                dueDateMillis = millis
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertNotNull(expense.dueDate)
            assertEquals(2026, expense.dueDate?.year)
            assertEquals(3, expense.dueDate?.monthValue)
            assertEquals(16, expense.dueDate?.dayOfMonth)
        }

        @Test
        fun `due date is null when status is not SCHEDULED`() {
            val state = AddExpenseUiState(
                expenseTitle = "Lunch",
                sourceAmount = "15.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedPaymentStatus = PaymentStatusUiModel(id = "FINISHED", displayText = "Paid"),
                dueDateMillis = 1773619200000L
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().dueDate)
        }

        @Test
        fun `maps receipt attachment from state`() {
            val attachment = es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment(
                localUri = "/data/user/0/receipts/abc.webp",
                mimeType = "image/webp",
                capturedAtMillis = 1716000000000L
            )
            val state = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "80.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                receiptUri = "/data/user/0/receipts/abc.webp",
                receiptAttachment = attachment
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertEquals(attachment, result.getOrThrow().receiptAttachment)
        }

        @Test
        fun `receipt attachment is null when not provided`() {
            val state = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "80.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                receiptUri = null,
                receiptAttachment = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().receiptAttachment)
        }
    }

    @Nested
    inner class MapAddOnsToDomain {

        @Test
        fun `filters out add-ons with zero resolved amount`() {
            val addOns = listOf(
                AddOnUiModel(
                    id = "a1",
                    type = AddOnType.FEE,
                    resolvedAmountCents = 500,
                    currency = eurUi,
                    paymentMethod = cashPaymentMethod
                ),
                AddOnUiModel(
                    id = "a2",
                    type = AddOnType.TIP,
                    resolvedAmountCents = 0,
                    currency = eurUi,
                    paymentMethod = cashPaymentMethod
                )
            )

            val result = addOnMapper.mapAddOnsToDomain(addOns, "EUR")

            assertEquals(1, result.size)
            assertEquals("a1", result[0].id)
        }

        @Test
        fun `maps all fields correctly for same-currency add-on`() {
            val addOns = listOf(
                AddOnUiModel(
                    id = "fee-1",
                    type = AddOnType.FEE,
                    mode = AddOnMode.ON_TOP,
                    valueType = AddOnValueType.EXACT,
                    resolvedAmountCents = 250,
                    groupAmountCents = 250,
                    currency = eurUi,
                    paymentMethod = PaymentMethodUiModel(
                        id = "DEBIT_CARD",
                        displayText = "Debit Card"
                    ),
                    description = "Bank fee"
                )
            )

            val result = addOnMapper.mapAddOnsToDomain(addOns, "EUR")

            assertEquals(1, result.size)
            val addOn = result[0]
            assertEquals("fee-1", addOn.id)
            assertEquals(AddOnType.FEE, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
            assertEquals(AddOnValueType.EXACT, addOn.valueType)
            assertEquals(250L, addOn.amountCents)
            assertEquals("EUR", addOn.currency)
            assertEquals(250L, addOn.groupAmountCents)
            assertEquals(PaymentMethod.DEBIT_CARD, addOn.paymentMethod)
            assertEquals("Bank fee", addOn.description)
        }

        @Test
        fun `maps blank description to null`() {
            val addOns = listOf(
                AddOnUiModel(
                    id = "a1",
                    resolvedAmountCents = 100,
                    currency = eurUi,
                    description = ""
                )
            )

            val result = addOnMapper.mapAddOnsToDomain(addOns, "EUR")

            assertNull(result[0].description)
        }

        @Test
        fun `defaults payment method to OTHER when null`() {
            val addOns = listOf(
                AddOnUiModel(
                    id = "a1",
                    resolvedAmountCents = 100,
                    currency = eurUi,
                    paymentMethod = null
                )
            )

            val result = addOnMapper.mapAddOnsToDomain(addOns, "EUR")

            assertEquals(PaymentMethod.OTHER, result[0].paymentMethod)
        }

        @Test
        fun `computes exchange rate from per-add-on display rate`() {
            val addOns = listOf(
                AddOnUiModel(
                    id = "a1",
                    type = AddOnType.FEE,
                    resolvedAmountCents = 1100,
                    groupAmountCents = 1000,
                    currency = usdUi,
                    paymentMethod = cashPaymentMethod,
                    displayExchangeRate = "1.1" // Per-add-on rate: 1 EUR = 1.1 USD
                )
            )

            val result = addOnMapper.mapAddOnsToDomain(addOns, "EUR")

            val rate = result[0].exchangeRate
            // Internal rate = 1/1.1 ≈ 0.909091
            assertEquals(
                0,
                BigDecimal("0.909091").compareTo(
                    rate.setScale(DomainConstants.RATE_PRECISION, java.math.RoundingMode.HALF_UP)
                )
            )
        }

        @Test
        fun `returns empty list for empty input`() {
            val result = addOnMapper.mapAddOnsToDomain(emptyList<AddOnUiModel>(), "EUR")
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class MapToDomainWithAddOns {

        @Test
        fun `includes add-ons in mapped expense`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                type = AddOnType.TIP,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.PERCENTAGE,
                amountInput = "10",
                resolvedAmountCents = 1000,
                groupAmountCents = 1000,
                currency = eurUi,
                paymentMethod = cashPaymentMethod,
                description = "Restaurant tip"
            )

            val state = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "100.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                addOns = kotlinx.collections.immutable.persistentListOf(addOn)
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(1, expense.addOns.size)
            assertEquals("addon-1", expense.addOns[0].id)
            assertEquals(AddOnType.TIP, expense.addOns[0].type)
            assertEquals(AddOnMode.ON_TOP, expense.addOns[0].mode)
            assertEquals(AddOnValueType.PERCENTAGE, expense.addOns[0].valueType)
            assertEquals(1000L, expense.addOns[0].amountCents)
            assertEquals("Restaurant tip", expense.addOns[0].description)
        }

        @Test
        fun `excludes add-ons with zero resolved amount from expense`() {
            val unresolved = AddOnUiModel(
                id = "addon-u",
                resolvedAmountCents = 0,
                currency = eurUi
            )
            val resolved = AddOnUiModel(
                id = "addon-r",
                resolvedAmountCents = 500,
                groupAmountCents = 500,
                currency = eurUi
            )

            val state = AddExpenseUiState(
                expenseTitle = "Lunch",
                sourceAmount = "50.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                addOns = kotlinx.collections.immutable.persistentListOf(
                    unresolved,
                    resolved
                )
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(1, expense.addOns.size)
            assertEquals("addon-r", expense.addOns[0].id)
        }
    }

    @Nested
    inner class MapPayerTypeAndPayerId {

        @Test
        fun `defaults to GROUP payerType when no funding source selected`() {
            val state = AddExpenseUiState(
                expenseTitle = "Lunch",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `maps GROUP funding source to GROUP payerType with null payerId`() {
            val state = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "25.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = FundingSourceUiModel(id = "GROUP", displayText = "Group Pocket"),
                currentUserId = "user-42"
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `maps USER funding source to USER payerType with currentUserId as payerId`() {
            val state = AddExpenseUiState(
                expenseTitle = "Coffee",
                sourceAmount = "5.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = FundingSourceUiModel(id = "USER", displayText = "My Money"),
                currentUserId = "user-42"
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PayerType.USER, expense.payerType)
            assertEquals("user-42", expense.payerId)
        }

        @Test
        fun `maps USER funding source with null currentUserId to null payerId`() {
            val state = AddExpenseUiState(
                expenseTitle = "Taxi",
                sourceAmount = "15.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = FundingSourceUiModel(id = "USER", displayText = "My Money"),
                currentUserId = null
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PayerType.USER, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `falls back to GROUP for unknown funding source id`() {
            val state = AddExpenseUiState(
                expenseTitle = "Test",
                sourceAmount = "10.00",
                selectedCurrency = eurUi,
                groupCurrency = eurUi,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = FundingSourceUiModel(id = "UNKNOWN", displayText = "???"),
                currentUserId = "user-42"
            )

            val result = mapper.mapToDomain(state, "group-123")

            assertTrue(result.isSuccess)
            val expense = result.getOrThrow()
            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }
    }

    // ── Lookup state for mapExpenseToState tests ───────────────────────────

    private val cashStatus = PaymentStatusUiModel(id = "FINISHED", displayText = "Paid")
    private val scheduledStatus = PaymentStatusUiModel(id = "SCHEDULED", displayText = "Scheduled")
    private val splitTypeEqual = SplitTypeUiModel(id = "EQUAL", displayText = "Equal")
    private val groupFundingSource = FundingSourceUiModel(id = "GROUP", displayText = "Group Pocket")
    private val travelCategory = CategoryUiModel(id = "TRANSPORT", displayText = "Transport")
    private val foodCategory = CategoryUiModel(id = "FOOD", displayText = "Food")

    /** Builds a state that contains all the look-up lists [mapExpenseToState] needs to resolve. */
    private fun baseEditState() = AddExpenseUiState(
        groupCurrency = eurUi,
        availableCurrencies = persistentListOf(eurUi, usdUi),
        paymentMethods = persistentListOf(cashPaymentMethod, creditCardPaymentMethod),
        availablePaymentStatuses = persistentListOf(cashStatus, scheduledStatus),
        availableSplitTypes = persistentListOf(splitTypeEqual),
        availableCategories = persistentListOf(travelCategory, foodCategory),
        fundingSources = persistentListOf(groupFundingSource)
    )

    @Nested
    inner class MapExpenseToState {

        @Test
        fun `maps basic same-currency expense to UI state`() {
            val expense = Expense(
                id = "exp-1",
                groupId = "group-1",
                title = "Taxi",
                sourceAmount = 1500L,
                sourceCurrency = "EUR",
                groupAmount = 1500L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                category = ExpenseCategory.TRANSPORT,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals("Taxi", result.expenseTitle)
            assertEquals("15", result.sourceAmount)
            assertEquals("EUR", result.selectedCurrency?.code)
            assertEquals(PaymentMethod.CASH.name, result.selectedPaymentMethod?.id)
            assertEquals(PaymentStatus.FINISHED.name, result.selectedPaymentStatus?.id)
            assertEquals(ExpenseCategory.TRANSPORT.name, result.selectedCategory?.id)
            assertFalse(result.showExchangeRateSection)
            assertNull(result.receiptAttachment)
            assertFalse(result.isAiModeActive)
        }

        @Test
        fun `maps cross-currency expense and sets exchange rate display`() {
            val expense = Expense(
                id = "exp-2",
                groupId = "group-1",
                title = "Hotel",
                sourceAmount = 30000L,
                sourceCurrency = "USD",
                groupAmount = 27500L,
                groupCurrency = "EUR",
                // internalRate USD→EUR = 1/1.09 ≈ 0.9174; displayRate stored as inverse = 1.09
                exchangeRate = BigDecimal("0.917431"),
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals("Hotel", result.expenseTitle)
            assertEquals("USD", result.selectedCurrency?.code)
            assertTrue(result.showExchangeRateSection)
            assertTrue(result.displayExchangeRate.isNotBlank())
        }

        @Test
        fun `maps expense with SCHEDULED status and due date`() {
            val dueDateTime = LocalDateTime.of(2026, 12, 31, 0, 0)
            val expense = Expense(
                id = "exp-3",
                groupId = "group-1",
                title = "Subscription",
                sourceAmount = 999L,
                sourceCurrency = "EUR",
                groupAmount = 999L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CREDIT_CARD,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = dueDateTime,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals(PaymentStatus.SCHEDULED.name, result.selectedPaymentStatus?.id)
            assertNotNull(result.dueDateMillis)
            assertTrue(result.formattedDueDate.isNotBlank())
            assertTrue(result.showDueDateSection)
        }

        @Test
        fun `null due date for FINISHED expense leaves dueDate fields empty`() {
            val expense = Expense(
                id = "exp-4",
                groupId = "group-1",
                title = "Coffee",
                sourceAmount = 350L,
                sourceCurrency = "EUR",
                groupAmount = 350L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertNull(result.dueDateMillis)
            assertTrue(result.formattedDueDate.isEmpty())
            assertFalse(result.showDueDateSection)
        }

        @Test
        fun `contribution scope and subunitId are applied from contribution`() {
            val expense = Expense(
                id = "exp-5",
                groupId = "group-1",
                title = "Tour",
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5000L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )
            val contribution = Contribution(
                contributionScope = PayerType.SUBUNIT,
                subunitId = "subunit-42"
            )

            val result = mapper.mapExpenseToState(
                expense,
                contribution,
                baseEditState(),
                emptyMap(),
                emptyList()
            )

            assertEquals(PayerType.SUBUNIT, result.contributionScope)
            assertEquals("subunit-42", result.selectedContributionSubunitId)
        }

        @Test
        fun `null contribution defaults scope to USER`() {
            val expense = Expense(
                id = "exp-6",
                groupId = "group-1",
                title = "Lunch",
                sourceAmount = 1200L,
                sourceCurrency = "EUR",
                groupAmount = 1200L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals(PayerType.USER, result.contributionScope)
            assertNull(result.selectedContributionSubunitId)
        }

        @Test
        fun `maps add-ons from expense to state`() {
            val addOn = AddOn(
                id = "addon-1",
                type = AddOnType.TIP,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 500L,
                currency = "EUR",
                groupAmountCents = 500L,
                paymentMethod = PaymentMethod.CASH
            )
            val expense = Expense(
                id = "exp-7",
                groupId = "group-1",
                title = "Restaurant",
                sourceAmount = 5000L,
                sourceCurrency = "EUR",
                groupAmount = 5500L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                addOns = listOf(addOn),
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals(1, result.addOns.size)
            assertEquals("addon-1", result.addOns[0].id)
            assertEquals(AddOnType.TIP, result.addOns[0].type)
        }

        @Test
        fun `expense with zero exchange rate uses 1 dot 0 as display rate`() {
            val expense = Expense(
                id = "exp-8",
                groupId = "group-1",
                title = "Airport",
                sourceAmount = 2000L,
                sourceCurrency = "USD",
                groupAmount = 2000L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ZERO,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals("1.0", result.displayExchangeRate)
        }

        @Test
        fun `maps vendor and notes when present`() {
            val expense = Expense(
                id = "exp-9",
                groupId = "group-1",
                title = "Supermarket",
                sourceAmount = 3000L,
                sourceCurrency = "EUR",
                groupAmount = 3000L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                vendor = "Lidl",
                notes = "Weekly groceries",
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals("Lidl", result.vendor)
            assertEquals("Weekly groceries", result.notes)
        }

        @Test
        fun `null vendor and notes map to empty strings`() {
            val expense = Expense(
                id = "exp-10",
                groupId = "group-1",
                title = "Bus",
                sourceAmount = 200L,
                sourceCurrency = "EUR",
                groupAmount = 200L,
                groupCurrency = "EUR",
                exchangeRate = BigDecimal.ONE,
                vendor = null,
                notes = null,
                paymentMethod = PaymentMethod.CASH,
                paymentStatus = PaymentStatus.FINISHED,
                splitType = SplitType.EQUAL
            )

            val result = mapper.mapExpenseToState(expense, null, baseEditState(), emptyMap(), emptyList())

            assertEquals("", result.vendor)
            assertEquals("", result.notes)
        }
    }
}
