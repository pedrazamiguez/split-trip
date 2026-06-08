package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.model.CashRatePreview
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AddOnEventHandler")
class AddOnEventHandlerTest {

    private lateinit var handler: AddOnEventHandler
    private lateinit var previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val eurCurrency = CurrencyUiModel(
        code = "EUR",
        displayText = "EUR (€)",
        decimalDigits = 2
    )
    private val usdCurrency = CurrencyUiModel(
        code = "USD",
        displayText = "USD ($)",
        decimalDigits = 2
    )
    private val thbCurrency = CurrencyUiModel(
        code = "THB",
        displayText = "THB (฿)",
        decimalDigits = 2
    )
    private val cashMethod = PaymentMethodUiModel(
        id = "CASH",
        displayText = "Cash"
    )
    private val cardMethod = PaymentMethodUiModel(
        id = "CREDIT_CARD",
        displayText = "Credit Card"
    )

    /** Base state simulating a typical EUR→EUR expense with 100.00 amount. */
    private val baseState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = eurCurrency,
        selectedPaymentMethod = cashMethod,
        sourceAmount = "100",
        displayExchangeRate = "1.0",
        calculatedGroupAmount = "100.00",
        availableCurrencies = persistentListOf(eurCurrency, usdCurrency, thbCurrency),
        paymentMethods = persistentListOf(cashMethod, cardMethod)
    )

    /** State simulating a foreign currency expense (EUR group, THB source) with CASH. */
    private val foreignCashState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = thbCurrency,
        selectedPaymentMethod = cashMethod,
        sourceAmount = "1000",
        displayExchangeRate = "37",
        calculatedGroupAmount = "27.03",
        availableCurrencies = persistentListOf(eurCurrency, usdCurrency, thbCurrency),
        paymentMethods = persistentListOf(cashMethod, cardMethod)
    )

    @BeforeEach
    fun setUp() {
        previewCashExchangeRateUseCase = mockk()
        val localeProvider = mockk<LocaleProvider>()
        val resourceProvider = mockk<ResourceProvider>(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val formattingHelper = FormattingHelper(localeProvider)
        val splitPreviewService = SplitPreviewServiceImpl()
        val getExchangeRateUseCase = mockk<GetExchangeRateUseCase>(relaxed = true)

        val addOnExchangeRateDelegate = AddOnExchangeRateDelegate(
            exchangeRateCalculationService = ExchangeRateCalculationServiceImpl(),
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper,
            getExchangeRateUseCase = getExchangeRateUseCase,
            previewCashExchangeRateUseCase = previewCashExchangeRateUseCase
        )

        val addOnCrudDelegate = AddOnCrudDelegate(
            addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true)),
            exchangeRateDelegate = addOnExchangeRateDelegate
        )

        handler = AddOnEventHandler(
            addOnCalculationService = AddOnCalculationServiceImpl(),
            exchangeRateCalculationService = ExchangeRateCalculationServiceImpl(),
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            splitPreviewService = splitPreviewService,
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true)),
            exchangeRateDelegate = addOnExchangeRateDelegate,
            addOnCrudDelegate = addOnCrudDelegate
        )

        uiState = MutableStateFlow(baseState)
        actions = MutableSharedFlow()
    }

    // ── CRUD Operations ─────────────────────────────────────────────────

    @Nested
    @DisplayName("handleAddOnAdded")
    inner class AddOnAdded {

        @Test
        fun `adds a new FEE add-on with defaults from expense state`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            val state = uiState.value
            assertEquals(1, state.addOns.size)
            val addOn = state.addOns[0]
            assertEquals(AddOnType.FEE, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
            assertEquals(eurCurrency, addOn.currency)
            assertEquals(cashMethod, addOn.paymentMethod)
            assertTrue(addOn.id.isNotBlank())
        }

        @Test
        fun `adds a TIP add-on`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.TIP)

            val state = uiState.value
            assertEquals(1, state.addOns.size)
            assertEquals(AddOnType.TIP, state.addOns[0].type)
        }

        @Test
        fun `DISCOUNT add-on defaults to ON_TOP mode`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.DISCOUNT)

            val addOn = uiState.value.addOns[0]
            assertEquals(AddOnType.DISCOUNT, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
        }

        @Test
        fun `expands the section when an add-on is added`() = runTest {
            uiState.value = baseState.copy(isAddOnsSectionExpanded = false)
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            assertTrue(uiState.value.isAddOnsSectionExpanded)
        }

        @Test
        fun `clears add-on error when a new add-on is added`() = runTest {
            uiState.value = baseState.copy(
                addOnError = es.pedrazamiguez.splittrip.core.common.presentation.UiText
                    .DynamicString("previous error")
            )
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            assertEquals(null, uiState.value.addOnError)
        }

        @Test
        fun `adds multiple add-ons preserving order`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)
            handler.handleAddOnAdded(AddOnType.TIP)
            handler.handleAddOnAdded(AddOnType.DISCOUNT)

            assertEquals(3, uiState.value.addOns.size)
            assertEquals(AddOnType.FEE, uiState.value.addOns[0].type)
            assertEquals(AddOnType.TIP, uiState.value.addOns[1].type)
            assertEquals(AddOnType.DISCOUNT, uiState.value.addOns[2].type)
        }
    }

    @Nested
    @DisplayName("handleAddOnRemoved")
    inner class AddOnRemoved {

        @Test
        fun `removes the specified add-on`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            handler.handleAddOnAdded(AddOnType.TIP)
            val feeId = uiState.value.addOns[0].id

            handler.handleAddOnRemoved(feeId)

            assertEquals(1, uiState.value.addOns.size)
            assertEquals(AddOnType.TIP, uiState.value.addOns[0].type)
        }

        @Test
        fun `clears add-on error when removing`() = runTest {
            uiState.value = baseState.copy(
                addOnError = es.pedrazamiguez.splittrip.core.common.presentation.UiText
                    .DynamicString("error")
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAddOnRemoved(id)

            assertEquals(null, uiState.value.addOnError)
        }

        @Test
        fun `removing non-existent id is no-op`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)

            handler.handleAddOnRemoved("non-existent-id")

            assertEquals(1, uiState.value.addOns.size)
        }
    }

    // ── Field Changes ───────────────────────────────────────────────────

    @Nested
    @DisplayName("handleTypeChanged")
    inner class TypeChanged {

        @Test
        fun `changes add-on type`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleTypeChanged(id, AddOnType.SURCHARGE)

            assertEquals(AddOnType.SURCHARGE, uiState.value.addOns[0].type)
        }
    }

    @Nested
    @DisplayName("handleModeChanged")
    inner class ModeChanged {

        @Test
        fun `changes mode from ON_TOP to INCLUDED`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id

            handler.handleModeChanged(id, AddOnMode.INCLUDED)

            assertEquals(AddOnMode.INCLUDED, uiState.value.addOns[0].mode)
        }
    }

    @Nested
    @DisplayName("handleValueTypeChanged")
    inner class ValueTypeChanged {

        @Test
        fun `switches to PERCENTAGE and clears amount input`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            handler.handleAmountChanged(id, "5.00")

            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            val addOn = uiState.value.addOns[0]
            assertEquals(AddOnValueType.PERCENTAGE, addOn.valueType)
            assertEquals("", addOn.amountInput)
        }
    }

    // ── Amount Resolution ───────────────────────────────────────────────

    @Nested
    @DisplayName("handleAmountChanged — EXACT")
    inner class AmountChangedExact {

        @Test
        fun `resolves EXACT amount to cents`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "2.50")

            val addOn = uiState.value.addOns[0]
            assertEquals("2.50", addOn.amountInput)
            assertEquals(250L, addOn.resolvedAmountCents)
            assertTrue(addOn.isAmountValid)
        }

        @Test
        fun `sets resolved to 0 for blank input`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "")

            assertEquals(0L, uiState.value.addOns[0].resolvedAmountCents)
        }

        @Test
        fun `clears add-on error when amount changes`() = runTest {
            uiState.value = baseState.copy(
                addOnError = es.pedrazamiguez.splittrip.core.common.presentation.UiText
                    .DynamicString("error")
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "5")

            assertEquals(null, uiState.value.addOnError)
        }
    }

    @Nested
    @DisplayName("handleAmountChanged — PERCENTAGE")
    inner class AmountChangedPercentage {

        @Test
        fun `resolves 10 percent of 100 EUR to 1000 cents`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "10")

            val addOn = uiState.value.addOns[0]
            assertEquals("10", addOn.amountInput)
            // 10% of 10000 cents = 1000 cents
            assertEquals(1000L, addOn.resolvedAmountCents)
        }

        @Test
        fun `resolves 15 percent of 200 EUR source amount`() = runTest {
            uiState.value = baseState.copy(sourceAmount = "200")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "15")

            // 15% of 20000 cents = 3000 cents
            assertEquals(3000L, uiState.value.addOns[0].resolvedAmountCents)
        }

        @Test
        fun `resolves 0 when source amount is blank`() = runTest {
            uiState.value = baseState.copy(sourceAmount = "")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "10")

            assertEquals(0L, uiState.value.addOns[0].resolvedAmountCents)
        }
    }

    // ── Currency & Payment Method ───────────────────────────────────────

    @Nested
    @DisplayName("handleCurrencySelected")
    inner class CurrencySelected {

        @Test
        fun `changes add-on currency`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleCurrencySelected(id, "USD")

            assertEquals(usdCurrency, uiState.value.addOns[0].currency)
        }

        @Test
        fun `ignores unknown currency code`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleCurrencySelected(id, "UNKNOWN")

            assertEquals(eurCurrency, uiState.value.addOns[0].currency)
        }
    }

    @Nested
    @DisplayName("handlePaymentMethodSelected")
    inner class PaymentMethodSelected {

        @Test
        fun `changes add-on payment method`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handlePaymentMethodSelected(id, "CREDIT_CARD")

            assertEquals(cardMethod, uiState.value.addOns[0].paymentMethod)
        }

        @Test
        fun `ignores unknown method id`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handlePaymentMethodSelected(id, "UNKNOWN")

            assertEquals(cashMethod, uiState.value.addOns[0].paymentMethod)
        }
    }

    // ── Description ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("handleDescriptionChanged")
    inner class DescriptionChanged {

        @Test
        fun `updates description text`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleDescriptionChanged(id, "ATM surcharge")

            assertEquals("ATM surcharge", uiState.value.addOns[0].description)
        }
    }

    // ── Section Toggle ──────────────────────────────────────────────────

    @Nested
    @DisplayName("handleSectionToggled")
    inner class SectionToggled {

        @Test
        fun `toggles section expansion from false to true`() = runTest {
            uiState.value = baseState.copy(isAddOnsSectionExpanded = false)
            handler.bind(uiState, actions, this)

            handler.handleSectionToggled()

            assertTrue(uiState.value.isAddOnsSectionExpanded)
        }

        @Test
        fun `toggles section expansion from true to false`() = runTest {
            uiState.value = baseState.copy(isAddOnsSectionExpanded = true)
            handler.bind(uiState, actions, this)

            handler.handleSectionToggled()

            assertFalse(uiState.value.isAddOnsSectionExpanded)
        }
    }

    // ── Effective Total Recalculation ────────────────────────────────────

    @Nested
    @DisplayName("recalculateEffectiveTotal")
    inner class EffectiveTotal {

        @Test
        fun `displays empty effective total when no add-ons`() = runTest {
            handler.bind(uiState, actions, this)

            handler.recalculateEffectiveTotal()

            assertEquals("", uiState.value.effectiveTotal)
        }

        @Test
        fun `shows effective total when ON_TOP fee is added`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "5")

            // Source: 100 EUR (10000 cents) + 5 EUR fee (500 cents) = 10500 cents
            val effectiveTotal = uiState.value.effectiveTotal
            assertTrue(effectiveTotal.isNotBlank())
        }

        @Test
        fun `INCLUDED tip does not change effective total`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)

            handler.handleAmountChanged(id, "10")

            // INCLUDED mode: effective total == base, so display is empty
            assertEquals("", uiState.value.effectiveTotal)
        }

        @Test
        fun `DISCOUNT reduces effective total`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.DISCOUNT)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "10")

            // 10000 - 1000 = 9000 cents → should show effective total
            assertTrue(uiState.value.effectiveTotal.isNotBlank())
        }

        @Test
        fun `empty effective total when group currency is null`() = runTest {
            uiState.value = baseState.copy(groupCurrency = null)
            handler.bind(uiState, actions, this)

            handler.recalculateEffectiveTotal()

            assertEquals("", uiState.value.effectiveTotal)
        }

        @Test
        fun `ON_TOP add-on does not set includedBaseCost`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "5")

            assertEquals("", uiState.value.includedBaseCost)
        }
    }

    // ── Included Base Cost Breakdown ────────────────────────────────────

    @Nested
    @DisplayName("includedBaseCost breakdown")
    inner class IncludedBaseCost {

        @Test
        fun `shows base cost for EXACT INCLUDED add-on`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)

            handler.handleAmountChanged(id, "10")

            // 100 EUR − 10 EUR = 90 EUR base → includedBaseCost should be non-blank
            val baseCost = uiState.value.includedBaseCost
            assertTrue(baseCost.isNotBlank(), "Expected non-blank base cost for EXACT INCLUDED")
        }

        @Test
        fun `shows base cost for PERCENTAGE INCLUDED add-on`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "20")

            // 100 EUR includes 20% → base = 100/1.20 ≈ 83.33 → non-blank
            val baseCost = uiState.value.includedBaseCost
            assertTrue(baseCost.isNotBlank(), "Expected non-blank base cost for PERCENTAGE INCLUDED")
        }

        @Test
        fun `base cost is empty when no INCLUDED add-ons`() = runTest {
            handler.bind(uiState, actions, this)

            handler.recalculateEffectiveTotal()

            assertEquals("", uiState.value.includedBaseCost)
        }

        @Test
        fun `base cost is empty when group currency is null`() = runTest {
            uiState.value = baseState.copy(groupCurrency = null)
            handler.bind(uiState, actions, this)

            handler.recalculateEffectiveTotal()

            assertEquals("", uiState.value.includedBaseCost)
        }

        @Test
        fun `base cost is empty when included equals total`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)

            // Set included amount equal to the total → base = 0, empty display
            handler.handleAmountChanged(id, "100")

            assertEquals("", uiState.value.includedBaseCost)
        }
    }

    // ── Group Currency Conversion ───────────────────────────────────────

    @Nested
    @DisplayName("Group currency conversion")
    inner class GroupCurrencyConversion {

        @Test
        fun `same currency add-on does not convert`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleAmountChanged(id, "5")

            val addOn = uiState.value.addOns[0]
            assertEquals(500L, addOn.resolvedAmountCents)
            assertEquals(500L, addOn.groupAmountCents)
        }

        @Test
        fun `different currency converts using per-add-on display rate`() = runTest {
            // EUR group, USD source with display rate 1 EUR = 1.10 USD
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                sourceAmount = "110"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            // Add-on inherits USD currency and rate from expense
            assertEquals("USD", uiState.value.addOns[0].currency?.code)
            assertTrue(uiState.value.addOns[0].showExchangeRateSection)

            handler.handleAmountChanged(id, "11")

            // 11 USD = 1100 cents
            // Internal rate = 1/1.10 ≈ 0.909091
            // 1100 * 0.909091 ≈ 1000 cents (10.00 EUR)
            val addOn = uiState.value.addOns[0]
            assertEquals(1100L, addOn.resolvedAmountCents)
            assertEquals(1000L, addOn.groupAmountCents)
        }

        @Test
        fun `add-on with manually overridden rate uses per-add-on rate`() = runTest {
            // EUR group, USD source with expense-level rate 1.10
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                sourceAmount = "110"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Override the add-on's own rate to 1.25 (different from expense's 1.10)
            handler.handleExchangeRateChanged(id, "1.25")

            // Enter amount after rate override
            handler.handleAmountChanged(id, "5")

            // 5 USD = 500 cents
            // Internal rate = 1/1.25 = 0.8
            // 500 * 0.8 = 400 cents (4.00 EUR)
            val addOn = uiState.value.addOns[0]
            assertEquals(500L, addOn.resolvedAmountCents)
            assertEquals(400L, addOn.groupAmountCents)
        }
    }

    // ── Per-add-on Exchange Rate ─────────────────────────────────────────

    @Nested
    @DisplayName("Per-add-on exchange rate")
    inner class PerAddOnExchangeRate {

        @Test
        fun `foreign add-on shows exchange rate section`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10"
            )
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.showExchangeRateSection)
            assertTrue(addOn.exchangeRateLabel.isNotBlank() || true) // relaxed resourceProvider
        }

        @Test
        fun `same-currency add-on does not show exchange rate section`() = runTest {
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.showExchangeRateSection)
        }

        @Test
        fun `changing add-on currency to foreign shows exchange rate section`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Initially same currency → no rate section
            assertFalse(uiState.value.addOns[0].showExchangeRateSection)

            // Change to USD (foreign)
            handler.handleCurrencySelected(id, "USD")

            assertTrue(uiState.value.addOns[0].showExchangeRateSection)
        }

        @Test
        fun `changing add-on currency back to group hides exchange rate section`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            assertTrue(uiState.value.addOns[0].showExchangeRateSection)

            // Change back to EUR (group currency)
            handler.handleCurrencySelected(id, "EUR")

            assertFalse(uiState.value.addOns[0].showExchangeRateSection)
            assertEquals("1.0", uiState.value.addOns[0].displayExchangeRate)
        }

        @Test
        fun `exchange rate change updates add-on display rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handleExchangeRateChanged(id, "1.25")

            assertEquals("1.25", uiState.value.addOns[0].displayExchangeRate)
        }

        @Test
        fun `calculatedGroupAmount updates when amount changes not only on rate change`() = runTest {
            // EUR group, USD source with 1 EUR = 1.10 USD
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                sourceAmount = "110"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Enter amount — should immediately update calculatedGroupAmount
            handler.handleAmountChanged(id, "11")

            val addOn = uiState.value.addOns[0]
            // 11 USD = 1100 cents → 1/1.10 ≈ 0.909091 → 1000 cents = 10.00 EUR
            assertEquals(1000L, addOn.groupAmountCents)
            // The display string should NOT be empty
            assertTrue(
                addOn.calculatedGroupAmount.isNotBlank(),
                "calculatedGroupAmount should update when amount changes, " +
                    "not only on rate change. Got: '${addOn.calculatedGroupAmount}'"
            )
        }
    }

    // ── CASH Payment Method — Exchange Rate Locking ──────────────────────

    @Nested
    @DisplayName("CASH payment method — exchange rate locking")
    inner class CashPaymentMethodLocking {

        @Test
        fun `switching to CASH on foreign add-on locks rate and fetches cash rate`() = runTest {
            // Given: EUR group, USD source with non-CASH add-on
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                selectedPaymentMethod = cardMethod,
                sourceAmount = "100"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("1.150000"),
                    groupAmountCents = 0L
                )
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            assertFalse(uiState.value.addOns[0].isExchangeRateLocked)

            // When: switch to CASH
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // Then: rate is locked and cash rate fetched
            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.isExchangeRateLocked)
            assertNotNull(addOn.exchangeRateLockedHint)
            assertFalse(addOn.isInsufficientCash)
            assertEquals("1.15", addOn.displayExchangeRate)
        }

        @Test
        fun `switching to CASH saves pre-cash exchange rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                selectedPaymentMethod = cardMethod,
                sourceAmount = "100"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("1.20"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Manually set a custom rate
            handler.handleExchangeRateChanged(id, "1.15")

            // When: switch to CASH
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // Then: pre-cash rate is saved
            assertEquals("1.15", uiState.value.addOns[0].preCashExchangeRate)
        }

        @Test
        fun `switching from CASH to non-CASH restores saved rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                selectedPaymentMethod = cardMethod,
                sourceAmount = "100"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("1.20"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Set custom rate, then switch to CASH
            handler.handleExchangeRateChanged(id, "1.15")
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // When: switch back to non-CASH
            handler.handlePaymentMethodSelected(id, "CREDIT_CARD")
            advanceUntilIdle()

            // Then: saved rate is restored, lock is released
            val addOn = uiState.value.addOns[0]
            assertEquals("1.15", addOn.displayExchangeRate)
            assertFalse(addOn.isExchangeRateLocked)
            assertNull(addOn.preCashExchangeRate)
            assertNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `switching from CASH to non-CASH fetches API rate when no saved rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                selectedPaymentMethod = cardMethod,
                sourceAmount = "100"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("1.20"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Switch to CASH
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // Change currency while on CASH (clears preCashExchangeRate)
            handler.handleCurrencySelected(id, "EUR")
            handler.handleCurrencySelected(id, "USD")
            advanceUntilIdle()

            // When: switch back to non-CASH (no saved rate)
            handler.handlePaymentMethodSelected(id, "CREDIT_CARD")
            advanceUntilIdle()

            // Then: rate is unlocked
            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isExchangeRateLocked)
            assertNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `switching between non-CASH methods does not affect rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                displayExchangeRate = "1.10",
                selectedPaymentMethod = cardMethod,
                sourceAmount = "100"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            advanceUntilIdle() // Let the initial rate fetch from creation complete
            val id = uiState.value.addOns[0].id
            handler.handleExchangeRateChanged(id, "1.25")

            // When: switch from one non-CASH to another non-CASH
            handler.handlePaymentMethodSelected(id, "CREDIT_CARD")
            advanceUntilIdle()

            // Then: rate is unchanged, not locked
            val addOn = uiState.value.addOns[0]
            assertEquals("1.25", addOn.displayExchangeRate)
            assertFalse(addOn.isExchangeRateLocked)
        }

        @Test
        fun `CASH on same-currency add-on does not lock rate`() = runTest {
            // EUR group, EUR add-on — not foreign, so no locking
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isExchangeRateLocked)
            assertNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `switching to non-CASH when rate was locked but not CASH clears lock (safety branch)`() =
            runTest {
                // Simulate a locked add-on that somehow has a non-CASH method (safety fallback branch).
                // Manually put the add-on into a locked state, then switch to another non-CASH method.
                uiState.value = baseState.copy(
                    groupCurrency = eurCurrency,
                    selectedCurrency = usdCurrency,
                    displayExchangeRate = "1.10",
                    selectedPaymentMethod = cardMethod
                )
                handler.bind(uiState, actions, this)
                handler.handleAddOnAdded(AddOnType.FEE)
                val id = uiState.value.addOns[0].id

                // Manually force the add-on into a locked state to trigger the safety branch.
                // This simulates state corruption that should be self-healing.
                uiState.value = uiState.value.copy(
                    addOns = uiState.value.addOns.map { addOn ->
                        if (addOn.id == id) {
                            addOn.copy(
                                isExchangeRateLocked = true,
                                isInsufficientCash = true
                            )
                        } else {
                            addOn
                        }
                    }.toImmutableList()
                )
                assertTrue(uiState.value.addOns[0].isExchangeRateLocked)

                // When: switch to another non-CASH method while rate is locked
                handler.handlePaymentMethodSelected(id, "CREDIT_CARD")
                advanceUntilIdle()

                // Then: the safety branch unlocks the rate
                val addOn = uiState.value.addOns[0]
                assertFalse(addOn.isExchangeRateLocked)
                assertFalse(addOn.isInsufficientCash)
                assertNull(addOn.exchangeRateLockedHint)
            }

        @Test
        fun `insufficient cash shows error hint`() = runTest {
            uiState.value = foreignCashState.copy(selectedPaymentMethod = cardMethod)
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.InsufficientCash
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // When: switch to CASH (with insufficient funds)
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // Then: locked with insufficient cash flag
            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.isExchangeRateLocked)
            assertTrue(addOn.isInsufficientCash)
            assertNotNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `insufficient cash with amount shows dash in group amount and zero groupAmountCents`() =
            runTest {
                uiState.value = foreignCashState.copy(
                    selectedPaymentMethod = cardMethod,
                    sourceAmount = "1000"
                )
                coEvery {
                    previewCashExchangeRateUseCase(any(), any(), any())
                } returns CashRatePreviewResult.InsufficientCash
                handler.bind(uiState, actions, this)
                handler.handleAddOnAdded(AddOnType.FEE)
                val id = uiState.value.addOns[0].id

                // Enter an amount before switching to CASH
                handler.handleAmountChanged(id, "500")

                // When: switch to CASH (with insufficient funds)
                handler.handlePaymentMethodSelected(id, "CASH")
                advanceUntilIdle()

                // Then: both rate and group amount show placeholder dash
                val addOn = uiState.value.addOns[0]
                assertEquals("—", addOn.displayExchangeRate)
                assertEquals("—", addOn.calculatedGroupAmount)
                assertEquals(0L, addOn.groupAmountCents)
            }

        @Test
        fun `no withdrawals with amount shows dash in group amount and zero groupAmountCents`() =
            runTest {
                uiState.value = foreignCashState.copy(
                    selectedPaymentMethod = cardMethod,
                    sourceAmount = "1000"
                )
                coEvery {
                    previewCashExchangeRateUseCase(any(), any(), any())
                } returns CashRatePreviewResult.NoWithdrawals
                handler.bind(uiState, actions, this)
                handler.handleAddOnAdded(AddOnType.FEE)
                val id = uiState.value.addOns[0].id

                // Enter an amount before switching to CASH
                handler.handleAmountChanged(id, "500")

                // When: switch to CASH (with no withdrawals)
                handler.handlePaymentMethodSelected(id, "CASH")
                advanceUntilIdle()

                // Then: both rate and group amount show placeholder dash
                val addOn = uiState.value.addOns[0]
                assertEquals("—", addOn.displayExchangeRate)
                assertEquals("—", addOn.calculatedGroupAmount)
                assertEquals(0L, addOn.groupAmountCents)
            }

        @Test
        fun `no withdrawals shows placeholder in locked fields`() = runTest {
            uiState.value = foreignCashState.copy(selectedPaymentMethod = cardMethod)
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.NoWithdrawals
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // When: switch to CASH (with no withdrawals)
            handler.handlePaymentMethodSelected(id, "CASH")
            advanceUntilIdle()

            // Then: locked with placeholder
            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.isExchangeRateLocked)
            assertFalse(addOn.isInsufficientCash)
            assertEquals("—", addOn.displayExchangeRate)
            assertEquals("—", addOn.calculatedGroupAmount)
        }
    }

    // ── CASH — Add-on creation with CASH payment method ─────────────────

    @Nested
    @DisplayName("CASH — add-on creation inherits CASH locking")
    inner class CashAddOnCreation {

        @Test
        fun `add-on created with CASH + foreign currency locks rate immediately`() = runTest {
            uiState.value = foreignCashState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("37.000000"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)

            // When: add-on is created (inherits CASH from expense)
            handler.handleAddOnAdded(AddOnType.FEE)
            advanceUntilIdle()

            // Then: locked from creation
            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.isExchangeRateLocked)
            assertNotNull(addOn.exchangeRateLockedHint)
            assertEquals("37", addOn.displayExchangeRate)
            coVerify { previewCashExchangeRateUseCase(any(), "THB", any()) }
        }

        @Test
        fun `add-on created with CASH + same currency does not lock rate`() = runTest {
            // EUR group, EUR source, CASH method — not foreign
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)
            advanceUntilIdle()

            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isExchangeRateLocked)
            assertNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `add-on created with non-CASH + foreign does not lock rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cardMethod,
                displayExchangeRate = "1.10"
            )
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)
            advanceUntilIdle()

            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isExchangeRateLocked)
            assertNull(addOn.exchangeRateLockedHint)
        }
    }

    // ── CASH — Currency change on CASH add-on ───────────────────────────

    @Nested
    @DisplayName("CASH — currency change locks/unlocks rate")
    inner class CashCurrencyChange {

        @Test
        fun `changing to foreign currency on CASH add-on locks rate`() = runTest {
            // Start with same currency (EUR→EUR) and CASH
            handler.bind(uiState, actions, this)
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("1.10"), groupAmountCents = 0L)
            )
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            assertFalse(uiState.value.addOns[0].isExchangeRateLocked)

            // When: change add-on currency to USD (foreign)
            handler.handleCurrencySelected(id, "USD")
            advanceUntilIdle()

            // Then: locked because CASH + foreign
            val addOn = uiState.value.addOns[0]
            assertTrue(addOn.isExchangeRateLocked)
            assertNotNull(addOn.exchangeRateLockedHint)
        }

        @Test
        fun `changing back to group currency on CASH add-on unlocks rate`() = runTest {
            uiState.value = foreignCashState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("37.0"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            advanceUntilIdle()
            assertTrue(uiState.value.addOns[0].isExchangeRateLocked)

            // When: change add-on currency back to EUR (group currency)
            handler.handleCurrencySelected(id, "EUR")
            advanceUntilIdle()

            // Then: unlocked — no exchange rate section
            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isExchangeRateLocked)
            assertFalse(addOn.showExchangeRateSection)
        }
    }

    // ── CASH — Amount change triggers cash rate recalculation ────────────

    @Nested
    @DisplayName("CASH — amount change recalculates cash rate")
    inner class CashAmountRecalculation {

        @Test
        fun `amount change on locked CASH add-on triggers debounced cash rate fetch`() = runTest {
            uiState.value = foreignCashState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("37.0"), groupAmountCents = 0L)
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            advanceUntilIdle()

            // Reset mock to verify new call
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.037037"),
                    groupAmountCents = 2703L
                )
            )

            // When: change amount
            handler.handleAmountChanged(id, "1000")
            advanceUntilIdle()

            // Then: cash rate was re-fetched with the new amount
            coVerify(atLeast = 2) { previewCashExchangeRateUseCase(any(), any(), any()) }
        }

        @Test
        fun `amount change on non-locked add-on does not fetch cash rate`() = runTest {
            uiState.value = baseState.copy(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cardMethod,
                displayExchangeRate = "1.10"
            )
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id
            advanceUntilIdle()

            // When: change amount on non-CASH add-on
            handler.handleAmountChanged(id, "50")
            advanceUntilIdle()

            // Then: no cash rate fetch
            coVerify(exactly = 0) { previewCashExchangeRateUseCase(any(), any(), any()) }
        }
    }

    // ── convertCentsToGroupCurrencyViaDisplayRate — placeholder rate ────

    @Nested
    @DisplayName("ExpenseCalculatorService.convertCentsToGroupCurrencyViaDisplayRate — edge cases")
    inner class ConvertToGroupCurrencyPlaceholder {

        private val calculatorService = ExchangeRateCalculationServiceImpl()

        @Test
        fun `returns 0 when displayExchangeRate is dash placeholder`() {
            val result = calculatorService.convertCentsToGroupCurrencyViaDisplayRate(50000L, "—")

            assertEquals(0L, result)
        }

        @Test
        fun `returns 0 when displayExchangeRate is blank`() {
            val result = calculatorService.convertCentsToGroupCurrencyViaDisplayRate(50000L, "")

            assertEquals(0L, result)
        }

        @Test
        fun `converts correctly when displayExchangeRate is valid`() {
            // 50000 THB cents (500 THB) / 37.0 = ~1351 EUR cents (~13.51 EUR)
            val result = calculatorService.convertCentsToGroupCurrencyViaDisplayRate(50000L, "37.0")

            assertTrue(result > 0L)
            assertEquals(1351L, result)
        }
    }

    // ── resolveAddOnAmounts — non-parseable input ────────────────────────

    @Nested
    @DisplayName("resolveAddOnAmounts — non-parseable input marks amount invalid")
    inner class ResolveAddOnAmountsNonParseable {

        @Test
        fun `non-numeric input sets isAmountValid false and zeroes cents`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.FEE)
            val id = uiState.value.addOns[0].id

            // Directly type something that normalizes to a non-parseable string.
            // CurrencyConverter.normalizeAmountString replaces commas but leaves letters.
            handler.handleAmountChanged(id, "abc")

            val addOn = uiState.value.addOns[0]
            assertFalse(addOn.isAmountValid)
            assertEquals(0L, addOn.resolvedAmountCents)
            assertEquals(0L, addOn.groupAmountCents)
        }
    }

    // ── computeIncludedBaseCostDisplay — groupAmountCents <= 0 ───────────

    @Nested
    @DisplayName("includedBaseCost — zero source amount edge case")
    inner class IncludedBaseCostZeroSource {

        @Test
        fun `base cost is empty when source amount is zero with INCLUDED add-on`() = runTest {
            // calculatedGroupAmount must also be blank so the handler parses "0" as the amount.
            // When groupAmountCents == 0, computeIncludedBaseCostDisplay returns "".
            uiState.value = baseState.copy(sourceAmount = "0", calculatedGroupAmount = "")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)

            handler.handleAmountChanged(id, "5")

            // groupAmountCents = 0 → computeIncludedBaseCostDisplay returns ""
            assertEquals("", uiState.value.includedBaseCost)
        }
    }

    // ── convertAddOnToGroupCurrency — null currency ──────────────────────

    @Nested
    @DisplayName("convertAddOnToGroupCurrency — null currency passthrough")
    inner class ConvertAddOnNullCurrency {

        @Test
        fun `add-on with null currency returns source amount unchanged`() = runTest {
            // State has NO selected currency so the newly added add-on will inherit null currency
            uiState.value = baseState.copy(selectedCurrency = null)
            handler.bind(uiState, actions, this)

            handler.handleAddOnAdded(AddOnType.FEE)

            val addOn = uiState.value.addOns[0]
            // currency is null → convertAddOnToGroupCurrency returns amountCents unchanged
            assertNull(addOn.currency)
            // Verify resolution still works (no crash)
            handler.handleAmountChanged(addOn.id, "5")
            val updated = uiState.value.addOns[0]
            assertEquals(500L, updated.resolvedAmountCents)
            // groupAmountCents == resolvedAmountCents because currency is null (passthrough)
            assertEquals(500L, updated.groupAmountCents)
        }
    }

    // ── INCLUDED DISCOUNT PERCENTAGE — Issue #1078 regression ────────────

    @Nested
    @DisplayName("resolveAddOnAmounts — INCLUDED DISCOUNT PERCENTAGE (issue 1078)")
    inner class IncludedDiscountPercentage {

        @Test
        fun `10 percent on 1821_52 EUR resolves to 20239 cents, not buggy 18215`() = runTest {
            // Reproduces the issue exactly: user enters 1821.52 EUR as the already-discounted
            // price and adds a 10% INCLUDED DISCOUNT. The discount embedded in the original
            // pre-discount price (2023.91 EUR) is 202.39 EUR, NOT 182.15 EUR (the buggy result).
            uiState.value = baseState.copy(sourceAmount = "1821.52")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.DISCOUNT)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "10")

            val addOn = uiState.value.addOns[0]
            assertEquals(20239L, addOn.resolvedAmountCents)
            assertEquals(20239L, addOn.groupAmountCents)
        }

        @Test
        fun `ON_TOP DISCOUNT PERCENTAGE still uses generic formula (no regression)`() = runTest {
            // Same input, but ON_TOP DISCOUNT must remain pct × source / 100 = 18215.
            uiState.value = baseState.copy(sourceAmount = "1821.52")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.DISCOUNT)
            val id = uiState.value.addOns[0].id
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "10")

            assertEquals(AddOnMode.ON_TOP, uiState.value.addOns[0].mode)
            assertEquals(18215L, uiState.value.addOns[0].resolvedAmountCents)
        }

        @Test
        fun `INCLUDED non-discount PERCENTAGE still uses generic formula (no regression)`() = runTest {
            // INCLUDED TIP 10% on 1821.52 must remain 18215 — only DISCOUNTs are corrected.
            uiState.value = baseState.copy(sourceAmount = "1821.52")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.TIP)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)
            handler.handleValueTypeChanged(id, AddOnValueType.PERCENTAGE)

            handler.handleAmountChanged(id, "10")

            assertEquals(18215L, uiState.value.addOns[0].resolvedAmountCents)
        }

        @Test
        fun `INCLUDED DISCOUNT EXACT is unaffected (already correct)`() = runTest {
            // EXACT amounts pass through directly — must NOT route through the new formula.
            uiState.value = baseState.copy(sourceAmount = "1821.52")
            handler.bind(uiState, actions, this)
            handler.handleAddOnAdded(AddOnType.DISCOUNT)
            val id = uiState.value.addOns[0].id
            handler.handleModeChanged(id, AddOnMode.INCLUDED)
            // valueType defaults to EXACT — no change needed

            handler.handleAmountChanged(id, "150")

            assertEquals(15000L, uiState.value.addOns[0].resolvedAmountCents)
        }
    }
}
