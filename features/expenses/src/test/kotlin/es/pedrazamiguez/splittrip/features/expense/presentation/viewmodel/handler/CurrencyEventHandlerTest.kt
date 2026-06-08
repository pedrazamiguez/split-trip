package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.CashRatePreview
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CurrencyEventHandlerTest {

    private lateinit var handler: CurrencyEventHandler
    private lateinit var previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase
    private lateinit var getExchangeRateUseCase: GetExchangeRateUseCase
    private lateinit var expenseCalculatorService: ExpenseCalculatorService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var withdrawalPoolSelectionDelegate: WithdrawalPoolSelectionDelegate

    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val thbCurrency = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)

    private val cashPaymentMethod = PaymentMethodUiModel(id = "CASH", displayText = "Cash")
    private val debitCardPaymentMethod = PaymentMethodUiModel(id = "DEBIT_CARD", displayText = "Debit Card")

    private val groupFundingSource = FundingSourceUiModel(id = "GROUP", displayText = "Group Pocket")
    private val userFundingSource = FundingSourceUiModel(id = "USER", displayText = "My Money")

    /** Initial state simulating a CASH + foreign-currency scenario. */
    private val cashForeignState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = thbCurrency,
        selectedPaymentMethod = cashPaymentMethod,
        showExchangeRateSection = true,
        isExchangeRateLocked = true
    )

    /** Initial state simulating a non-CASH + foreign-currency scenario with a custom rate. */
    private val nonCashForeignState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = thbCurrency,
        selectedPaymentMethod = debitCardPaymentMethod,
        showExchangeRateSection = true,
        isExchangeRateLocked = false,
        displayExchangeRate = "35.5",
        sourceAmount = "1000"
    )

    @BeforeEach
    fun setUp() {
        previewCashExchangeRateUseCase = mockk()
        getExchangeRateUseCase = mockk(relaxed = true)
        expenseCalculatorService = ExpenseCalculatorServiceImpl()
        exchangeRateCalculationService = ExchangeRateCalculationServiceImpl()

        val localeProvider = mockk<LocaleProvider>()
        val resourceProvider = mockk<ResourceProvider>(relaxed = true)
        every { localeProvider.getCurrentLocale() } returns Locale.US

        val formattingHelper = FormattingHelper(localeProvider)
        val splitPreviewService = SplitPreviewServiceImpl()

        withdrawalPoolSelectionDelegate = mockk(relaxed = true)

        handler = CurrencyEventHandler(
            getExchangeRateUseCase = getExchangeRateUseCase,
            exchangeRateCalculationService = exchangeRateCalculationService,
            formattingHelper = formattingHelper,
            addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true)),
            withdrawalPoolSelectionDelegate = withdrawalPoolSelectionDelegate,
            cashRateDelegate = CashRateDelegate(
                previewCashExchangeRateUseCase = previewCashExchangeRateUseCase,
                expenseCalculatorService = expenseCalculatorService,
                splitPreviewService = splitPreviewService,
                formattingHelper = formattingHelper,
                addExpenseOptionsMapper = AddExpenseOptionsUiMapper(resourceProvider, mockk(relaxed = true))
            )
        )

        uiState = MutableStateFlow(cashForeignState)
        actions = MutableSharedFlow()
    }

    /**
     * Configures [withdrawalPoolSelectionDelegate] to immediately invoke the [onPoolResolved]
     * callback. Use in tests that verify GROUP CASH behaviour where the pool is auto-resolved
     * (single pool) and the rate preview should fire as part of the same test.
     */
    private fun configurePoolDelegateToResolveImmediately() {
        every {
            withdrawalPoolSelectionDelegate.fetchPools(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } answers {
            lastArg<() -> Unit>().invoke()
        }
    }

    // ── InsufficientCash ────────────────────────────────────────────────────

    @Nested
    inner class InsufficientCash {

        @Test
        fun `sets placeholder and warning hint when cash is insufficient`() = runTest {
            // Given: user typed 25225 THB but not enough cash
            uiState.value = cashForeignState.copy(sourceAmount = "25225")
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.InsufficientCash

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertEquals("—", state.displayExchangeRate)
            assertEquals("—", state.calculatedGroupAmount)
            assertTrue(state.isInsufficientCash)
            assertTrue(state.isExchangeRateLocked)
            assertEquals(
                UiText.StringResource(R.string.add_expense_cash_insufficient_hint),
                state.exchangeRateLockedHint
            )
        }

        @Test
        fun `clears insufficient cash when valid amount is entered after overshoot`() = runTest {
            // Given: previously insufficient
            uiState.value = cashForeignState.copy(
                sourceAmount = "100",
                displayExchangeRate = "—",
                calculatedGroupAmount = "—",
                isInsufficientCash = true
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.000000"),
                    groupAmountCents = 270L
                )
            )

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertFalse(state.isInsufficientCash)
            assertEquals("37", state.displayExchangeRate)
            assertEquals("2.70", state.calculatedGroupAmount)
            assertEquals(
                UiText.StringResource(R.string.add_expense_cash_rate_locked_hint),
                state.exchangeRateLockedHint
            )
        }

        @Test
        fun `same-currency insufficient cash does not overwrite rate fields with placeholder`() = runTest {
            // Given: same-currency CASH (showExchangeRateSection = false)
            val sameCurrencyState = AddExpenseUiState(
                loadedGroupId = "group-1",
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cashPaymentMethod,
                showExchangeRateSection = false,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = ""
            )
            uiState.value = sameCurrencyState.copy(sourceAmount = "60")
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.InsufficientCash

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then: isInsufficientCash set, but rate fields NOT overwritten with placeholder
            val state = uiState.value
            assertTrue(state.isInsufficientCash)
            assertEquals("1.0", state.displayExchangeRate) // unchanged
            assertEquals("", state.calculatedGroupAmount) // unchanged
        }

        @Test
        fun `same-currency insufficient cash clears when a smaller valid amount is typed`() = runTest {
            // Given: same-currency CASH previously flagged as insufficient
            val sameCurrencyState = AddExpenseUiState(
                loadedGroupId = "group-1",
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cashPaymentMethod,
                showExchangeRateSection = false,
                isInsufficientCash = true,
                displayExchangeRate = "1.0",
                calculatedGroupAmount = "",
                sourceAmount = "60"
            )
            uiState.value = sameCurrencyState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(displayRate = BigDecimal("1.0"), groupAmountCents = 400L)
            )

            handler.bind(uiState, actions, this)

            // When: user corrects the amount to something that fits
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then: insufficient flag is cleared
            val state = uiState.value
            assertFalse(state.isInsufficientCash)
        }
    }

    // ── NoWithdrawals ───────────────────────────────────────────────────────

    @Nested
    inner class NoWithdrawals {

        @Test
        fun `sets placeholder and generic hint when no withdrawals exist`() = runTest {
            uiState.value = cashForeignState.copy(sourceAmount = "100")
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.NoWithdrawals

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertEquals("—", state.displayExchangeRate)
            assertEquals("—", state.calculatedGroupAmount)
            assertFalse(state.isInsufficientCash)
            assertTrue(state.isExchangeRateLocked)
            assertEquals(
                UiText.StringResource(R.string.add_expense_cash_rate_locked_hint),
                state.exchangeRateLockedHint
            )
        }
    }

    // ── Available (FIFO) ────────────────────────────────────────────────────

    @Nested
    inner class Available {

        @Test
        fun `sets formatted rate and group amount for FIFO result`() = runTest {
            uiState.value = cashForeignState.copy(sourceAmount = "500")
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.037037"),
                    groupAmountCents = 1350L
                )
            )

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertFalse(state.isInsufficientCash)
            assertEquals("37.037037", state.displayExchangeRate)
            assertEquals("13.50", state.calculatedGroupAmount)
            assertTrue(state.isExchangeRateLocked)
        }

        @Test
        fun `sets weighted-average rate without group amount when no amount entered`() = runTest {
            // sourceAmount is empty — preview returns weighted avg (groupAmountCents = 0)
            uiState.value = cashForeignState.copy(sourceAmount = "")
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("36.855037"),
                    groupAmountCents = 0L
                )
            )

            handler.bind(uiState, actions, this)

            // When
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertFalse(state.isInsufficientCash)
            assertEquals("36.855037", state.displayExchangeRate)
            assertEquals("", state.calculatedGroupAmount)
            assertTrue(state.isExchangeRateLocked)
        }

        @Test
        fun `clears stale group amount when switching from FIFO to weighted-average`() = runTest {
            // Given: user previously had a FIFO-simulated result with a calculated group amount
            uiState.value = cashForeignState.copy(
                sourceAmount = "",
                calculatedGroupAmount = "13.50",
                displayExchangeRate = "37.037037"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("36.855037"),
                    groupAmountCents = 0L
                )
            )

            handler.bind(uiState, actions, this)

            // When: source amount is now empty, so weighted-average preview fires
            handler.fetchCashRate()
            advanceUntilIdle()

            // Then: stale "13.50" must be cleared
            val state = uiState.value
            assertFalse(state.isInsufficientCash)
            assertEquals("36.855037", state.displayExchangeRate)
            assertEquals("", state.calculatedGroupAmount)
            assertTrue(state.isExchangeRateLocked)
        }
    }

    // ── handlePaymentMethodChanged ────────────────────────────────────────────

    @Nested
    inner class PaymentMethodChanged {

        @Test
        fun `switching between non-CASH methods preserves custom exchange rate`() = runTest {
            // Given: user has a custom rate of "35.5" on DEBIT_CARD
            uiState.value = nonCashForeignState
            handler.bind(uiState, actions, this)

            // When: user switches to another non-CASH method (e.g. PAYPAL)
            handler.handlePaymentMethodChanged(isCash = false)
            advanceUntilIdle()

            // Then: rate is unchanged, no API call
            val state = uiState.value
            assertEquals("35.5", state.displayExchangeRate)
            assertFalse(state.isExchangeRateLocked)
            coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `switching to CASH saves current rate and locks exchange rate`() = runTest {
            // Given: user has a custom rate of "35.5"
            uiState.value = nonCashForeignState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.000000"),
                    groupAmountCents = 2703L
                )
            )
            // Pool delegate auto-resolves (simulates single pool auto-selected) so
            // the cash rate fetch fires as part of the same flow.
            configurePoolDelegateToResolveImmediately()
            handler.bind(uiState, actions, this)

            // When: user switches to CASH
            handler.handlePaymentMethodChanged(isCash = true)
            advanceUntilIdle()

            // Then: the custom rate is saved and the cash rate is applied
            val state = uiState.value
            assertEquals("35.5", state.preCashExchangeRate)
            assertTrue(state.isExchangeRateLocked)
            // Cash rate overwrites displayExchangeRate
            assertEquals("37", state.displayExchangeRate)
        }

        @Test
        fun `switching from CASH to non-CASH restores saved rate`() = runTest {
            // Given: user was on CASH with a saved pre-cash rate
            uiState.value = cashForeignState.copy(
                displayExchangeRate = "37.000000",
                preCashExchangeRate = "35.5",
                sourceAmount = "1000"
            )
            handler.bind(uiState, actions, this)

            // When: user switches back to non-CASH
            handler.handlePaymentMethodChanged(isCash = false)
            advanceUntilIdle()

            // Then: the saved rate is restored, no API call
            val state = uiState.value
            assertEquals("35.5", state.displayExchangeRate)
            assertFalse(state.isExchangeRateLocked)
            assertNull(state.preCashExchangeRate)
            coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `switching from CASH to non-CASH fetches API rate when no saved rate exists`() = runTest {
            // Given: user was on CASH but currency changed while on CASH (no saved rate)
            uiState.value = cashForeignState.copy(
                displayExchangeRate = "37.000000",
                preCashExchangeRate = null,
                sourceAmount = "1000"
            )
            coEvery {
                getExchangeRateUseCase(any(), any())
            } returns ExchangeRateWithStaleness(rate = BigDecimal("36.8"), isStale = false)
            handler.bind(uiState, actions, this)

            // When: user switches back to non-CASH
            handler.handlePaymentMethodChanged(isCash = false)
            advanceUntilIdle()

            // Then: API rate is fetched as fallback
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            coVerify(exactly = 1) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `round trip CASH and back preserves original custom rate`() = runTest {
            // Given: user has a custom rate of "35.5" on DEBIT_CARD
            uiState.value = nonCashForeignState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.000000"),
                    groupAmountCents = 2703L
                )
            )
            // Pool delegate auto-resolves so the cash rate fetch fires, then verify the
            // restored rate survives after switching back to non-CASH.
            configurePoolDelegateToResolveImmediately()
            handler.bind(uiState, actions, this)

            // When: user switches to CASH, then back to non-CASH
            handler.handlePaymentMethodChanged(isCash = true)
            advanceUntilIdle()
            handler.handlePaymentMethodChanged(isCash = false)
            advanceUntilIdle()

            // Then: original custom rate is restored
            val state = uiState.value
            assertEquals("35.5", state.displayExchangeRate)
            assertFalse(state.isExchangeRateLocked)
            assertNull(state.preCashExchangeRate)
            // No API call should have been made
            coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `switching to non-CASH with same currency does not fetch rate`() = runTest {
            // Given: same currency (not foreign)
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cashPaymentMethod,
                isExchangeRateLocked = true
            )
            handler.bind(uiState, actions, this)

            // When
            handler.handlePaymentMethodChanged(isCash = false)
            advanceUntilIdle()

            // Then: no rate fetch, just unlock
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            coVerify(exactly = 0) { getExchangeRateUseCase(any(), any()) }
        }

        @Test
        fun `in-flight cash rate job does not overwrite restored rate after switching away`() = runTest {
            // Given: user is on non-CASH with custom rate "35.5"
            uiState.value = nonCashForeignState
            // Simulate a slow CASH rate response
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } coAnswers {
                kotlinx.coroutines.delay(500L) // slow response
                CashRatePreviewResult.Available(
                    CashRatePreview(
                        displayRate = BigDecimal("37.000000"),
                        groupAmountCents = 2703L
                    )
                )
            }
            handler.bind(uiState, actions, this)

            // When: user switches to CASH (triggers async fetchCashRate)
            handler.handlePaymentMethodChanged(isCash = true)
            // Then immediately switches back to non-CASH before cash rate arrives
            handler.handlePaymentMethodChanged(isCash = false)
            // Now let all pending coroutines complete
            advanceUntilIdle()

            // Then: the restored custom rate must survive — the cancelled cash job
            // must NOT overwrite it
            val state = uiState.value
            assertEquals("35.5", state.displayExchangeRate)
            assertFalse(state.isExchangeRateLocked)
        }

        @Test
        fun `CASH with isGroupPocket false does not lock exchange rate`() = runTest {
            // Given: user has a custom rate of "35.5"
            uiState.value = nonCashForeignState
            handler.bind(uiState, actions, this)

            // When: user switches to CASH with USER funding source (not group pocket)
            handler.handlePaymentMethodChanged(isCash = true, isGroupPocket = false)
            advanceUntilIdle()

            // Then: rate is NOT locked — user pays from own money, can enter rate manually
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            assertNull(state.exchangeRateLockedHint)
            // No cash rate preview call
            coVerify(exactly = 0) { previewCashExchangeRateUseCase(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `CASH with isGroupPocket true locks exchange rate and fetches cash rate`() = runTest {
            // Given: user has a custom rate of "35.5"
            uiState.value = nonCashForeignState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.000000"),
                    groupAmountCents = 2703L
                )
            )
            // Pool delegate auto-resolves so the cash rate fetch fires via onPoolResolved.
            configurePoolDelegateToResolveImmediately()
            handler.bind(uiState, actions, this)

            // When: user switches to CASH with GROUP funding source
            handler.handlePaymentMethodChanged(isCash = true, isGroupPocket = true)
            advanceUntilIdle()

            // Then: rate is locked with cash preview
            val state = uiState.value
            assertTrue(state.isExchangeRateLocked)
            assertEquals("37", state.displayExchangeRate)
            coVerify(exactly = 1) { previewCashExchangeRateUseCase(any(), any(), any(), any(), any()) }
        }
    }

    // ── handleFundingSourceChanged ─────────────────────────────────────────────

    @Nested
    inner class FundingSourceChanged {

        @Test
        fun `switching to USER on CASH foreign unlocks exchange rate`() = runTest {
            // Given: CASH + foreign + GROUP (locked)
            uiState.value = cashForeignState.copy(
                selectedFundingSource = groupFundingSource,
                displayExchangeRate = "37.000000",
                preCashExchangeRate = "35.5"
            )
            handler.bind(uiState, actions, this)

            // When: funding source changes to USER
            handler.handleFundingSourceChanged(isGroupPocket = false)
            advanceUntilIdle()

            // Then: rate is unlocked and saved rate is restored
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            assertEquals("35.5", state.displayExchangeRate)
            assertNull(state.preCashExchangeRate)
        }

        @Test
        fun `switching back to GROUP on CASH foreign locks exchange rate`() = runTest {
            // Given: CASH + foreign + USER (unlocked)
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                groupCurrency = eurCurrency,
                selectedCurrency = thbCurrency,
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = userFundingSource,
                showExchangeRateSection = true,
                isExchangeRateLocked = false,
                displayExchangeRate = "35.5",
                sourceAmount = "1000"
            )
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.Available(
                CashRatePreview(
                    displayRate = BigDecimal("37.000000"),
                    groupAmountCents = 2703L
                )
            )
            // Pool delegate auto-resolves so the cash rate fetch fires via onPoolResolved.
            configurePoolDelegateToResolveImmediately()
            handler.bind(uiState, actions, this)

            // When: funding source changes to GROUP
            handler.handleFundingSourceChanged(isGroupPocket = true)
            advanceUntilIdle()

            // Then: rate is locked and cash rate is fetched
            val state = uiState.value
            assertTrue(state.isExchangeRateLocked)
            assertEquals("37", state.displayExchangeRate)
        }

        @Test
        fun `switching funding source on non-CASH does not affect exchange rate`() = runTest {
            // Given: DEBIT_CARD + foreign + GROUP
            uiState.value = nonCashForeignState.copy(
                selectedFundingSource = groupFundingSource
            )
            handler.bind(uiState, actions, this)

            // When: funding source changes to USER
            handler.handleFundingSourceChanged(isGroupPocket = false)
            advanceUntilIdle()

            // Then: rate is unchanged, no cash preview call
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            assertEquals("35.5", state.displayExchangeRate)
            coVerify(exactly = 0) { previewCashExchangeRateUseCase(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `switching funding source on same currency does not affect exchange rate`() = runTest {
            // Given: CASH + same currency
            uiState.value = AddExpenseUiState(
                loadedGroupId = "group-1",
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cashPaymentMethod,
                selectedFundingSource = groupFundingSource,
                isExchangeRateLocked = false
            )
            handler.bind(uiState, actions, this)

            // When: funding source changes to USER
            handler.handleFundingSourceChanged(isGroupPocket = false)
            advanceUntilIdle()

            // Then: nothing happens (same currency, no rate management)
            val state = uiState.value
            assertFalse(state.isExchangeRateLocked)
            coVerify(exactly = 0) { previewCashExchangeRateUseCase(any(), any(), any(), any(), any()) }
        }
    }

    // ── ExchangeRateErrorHandling ────────────────────────────────────────────

    @Nested
    inner class ExchangeRateErrorHandling {

        @Test
        fun `successful fetch rate resets error flag to false`() = runTest {
            uiState.value = nonCashForeignState.copy(isExchangeRateError = true)
            coEvery {
                getExchangeRateUseCase(any(), any())
            } returns ExchangeRateWithStaleness(rate = BigDecimal("36.8"), isStale = false)

            handler.bind(uiState, actions, this)

            // When
            handler.fetchRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
            assertEquals("36.8", state.displayExchangeRate)
        }

        @Test
        fun `null rateResult from fetch rate sets error flag to true`() = runTest {
            uiState.value = nonCashForeignState.copy(isExchangeRateError = false, displayExchangeRate = "")
            coEvery {
                getExchangeRateUseCase(any(), any())
            } returns null

            handler.bind(uiState, actions, this)

            // When
            handler.fetchRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertTrue(state.isExchangeRateError)
            assertEquals("", state.displayExchangeRate)
        }

        @Test
        fun `exception from fetch rate sets error flag to true`() = runTest {
            uiState.value = nonCashForeignState.copy(isExchangeRateError = false, displayExchangeRate = "")
            coEvery {
                getExchangeRateUseCase(any(), any())
            } throws RuntimeException("Network error")

            handler.bind(uiState, actions, this)

            // When
            handler.fetchRate()
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertTrue(state.isExchangeRateError)
            assertEquals("", state.displayExchangeRate)
        }

        @Test
        fun `manual exchange rate change resets error flag to false`() = runTest {
            uiState.value = nonCashForeignState.copy(isExchangeRateError = true)
            handler.bind(uiState, actions, this)

            // When
            handler.handleExchangeRateChanged("35.5")

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
            assertEquals("35.5", state.displayExchangeRate)
        }

        @Test
        fun `manual group amount change resets error flag to false`() = runTest {
            uiState.value = nonCashForeignState.copy(isExchangeRateError = true, displayExchangeRate = "")
            handler.bind(uiState, actions, this)

            // When
            handler.handleGroupAmountChanged("28.17")

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
        }

        @Test
        fun `recalculateForward leaves calculated amount empty if rate is blank`() = runTest {
            uiState.value = nonCashForeignState.copy(displayExchangeRate = "", calculatedGroupAmount = "10.00")
            handler.bind(uiState, actions, this)

            // When
            handler.recalculateForward()

            // Then
            val state = uiState.value
            assertEquals("", state.calculatedGroupAmount)
        }
    }
}
