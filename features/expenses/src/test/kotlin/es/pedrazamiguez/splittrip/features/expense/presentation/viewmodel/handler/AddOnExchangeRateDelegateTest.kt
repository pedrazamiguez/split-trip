package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.CashRatePreview
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.util.Locale
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("AddOnExchangeRateDelegate")
class AddOnExchangeRateDelegateTest {

    private lateinit var delegate: AddOnExchangeRateDelegate
    private lateinit var getExchangeRateUseCase: GetExchangeRateUseCase
    private lateinit var previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val thbCurrency = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)
    private val cashMethod = PaymentMethodUiModel(id = "CASH", displayText = "Cash")

    @BeforeEach
    fun setUp() {
        val localeProvider = mockk<LocaleProvider>()
        every { localeProvider.getCurrentLocale() } returns Locale.US

        getExchangeRateUseCase = mockk(relaxed = true)
        previewCashExchangeRateUseCase = mockk(relaxed = true)

        delegate = AddOnExchangeRateDelegate(
            exchangeRateCalculationService = ExchangeRateCalculationServiceImpl(),
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            splitPreviewService = SplitPreviewServiceImpl(),
            formattingHelper = FormattingHelper(localeProvider),
            getExchangeRateUseCase = getExchangeRateUseCase,
            previewCashExchangeRateUseCase = previewCashExchangeRateUseCase
        )
    }

    // ── isCashMethod ────────────────────────────────────────────────────

    @Nested
    @DisplayName("isCashMethod")
    inner class IsCashMethod {

        @Test
        fun `returns true for CASH`() {
            assertTrue(delegate.isCashMethod("CASH"))
        }

        @Test
        fun `returns false for CREDIT_CARD`() {
            assertFalse(delegate.isCashMethod("CREDIT_CARD"))
        }

        @Test
        fun `returns false for null`() {
            assertFalse(delegate.isCashMethod(null))
        }

        @Test
        fun `returns false for invalid string`() {
            assertFalse(delegate.isCashMethod("NOT_A_METHOD"))
        }

        @Test
        fun `returns false for empty string`() {
            assertFalse(delegate.isCashMethod(""))
        }
    }

    // ── recalculateForward ──────────────────────────────────────────────

    @Nested
    @DisplayName("recalculateForward")
    inner class RecalculateForward {

        @Test
        fun `no-ops when add-on not found in state`() {
            val stateFlow = MutableStateFlow(AddExpenseUiState(addOns = persistentListOf()))
            var wasUpdated = false

            delegate.recalculateForward("missing-id", stateFlow) { _, _ -> wasUpdated = true }

            assertFalse(wasUpdated)
        }

        @Test
        fun `no-ops when showExchangeRateSection is false`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = false,
                currency = thbCurrency,
                displayExchangeRate = "37.0",
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var wasUpdated = false

            delegate.recalculateForward("addon-1", stateFlow) { _, _ -> wasUpdated = true }

            assertFalse(wasUpdated)
        }

        @Test
        fun `calculates group amount from source amount and display rate`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = true,
                currency = thbCurrency,
                displayExchangeRate = "37.0",
                amountInput = "1000",
                resolvedAmountCents = 0L
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var updatedGroupAmount = ""

            delegate.recalculateForward("addon-1", stateFlow) { id, transform ->
                val result = transform(addOn)
                updatedGroupAmount = result.calculatedGroupAmount
            }

            // 1000 / 37 = 27.03
            assertTrue(updatedGroupAmount.contains("27.03"))
        }

        @Test
        fun `uses resolvedAmountCents when positive`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = true,
                currency = thbCurrency,
                displayExchangeRate = "37.0",
                amountInput = "999",
                resolvedAmountCents = 100000L // 1000.00 THB
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var updatedGroupAmount = ""

            delegate.recalculateForward("addon-1", stateFlow) { _, transform ->
                val result = transform(addOn)
                updatedGroupAmount = result.calculatedGroupAmount
            }

            // 1000.00 / 37 = 27.03
            assertTrue(updatedGroupAmount.contains("27.03"))
        }
    }

    // ── recalculateReverse ──────────────────────────────────────────────

    @Nested
    @DisplayName("recalculateReverse")
    inner class RecalculateReverse {

        @Test
        fun `no-ops when add-on not found`() {
            val stateFlow = MutableStateFlow(AddExpenseUiState(addOns = persistentListOf()))
            var wasUpdated = false

            delegate.recalculateReverse("missing-id", stateFlow) { _, _ -> wasUpdated = true }

            assertFalse(wasUpdated)
        }

        @Test
        fun `no-ops when showExchangeRateSection is false`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = false,
                currency = thbCurrency,
                calculatedGroupAmount = "27.03",
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var wasUpdated = false

            delegate.recalculateReverse("addon-1", stateFlow) { _, _ -> wasUpdated = true }

            assertFalse(wasUpdated)
        }

        @Test
        fun `computes implied display rate from group amount`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = true,
                currency = thbCurrency,
                calculatedGroupAmount = "27.03",
                amountInput = "1000",
                resolvedAmountCents = 0L
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var updatedRate = ""

            delegate.recalculateReverse("addon-1", stateFlow) { _, transform ->
                val result = transform(addOn)
                updatedRate = result.displayExchangeRate
            }

            // 1000 / 27.03 ≈ 37.0
            assertTrue(updatedRate.startsWith("36.99") || updatedRate.startsWith("37"))
        }

        @Test
        fun `uses resolvedAmountCents when positive`() {
            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = true,
                currency = thbCurrency,
                calculatedGroupAmount = "27.03",
                amountInput = "999",
                resolvedAmountCents = 100000L // 1000.00 THB
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var updatedRate = ""

            delegate.recalculateReverse("addon-1", stateFlow) { _, transform ->
                val result = transform(addOn)
                updatedRate = result.displayExchangeRate
            }

            assertTrue(updatedRate.isNotBlank())
        }
    }

    // ── fetchRate ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchRate")
    inner class FetchRate {

        @Test
        fun `updates add-on with fetched rate on success`() = runTest {
            coEvery {
                getExchangeRateUseCase(
                    baseCurrencyCode = "EUR",
                    targetCurrencyCode = "THB"
                )
            } returns ExchangeRateWithStaleness(rate = BigDecimal("0.027"), isStale = false)

            val addOn = AddOnUiModel(
                id = "addon-1",
                showExchangeRateSection = true,
                currency = thbCurrency,
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var latestAddOn = addOn
            var rateApplied = false

            delegate.fetchRate(
                addOnId = "addon-1",
                baseCurrencyCode = "EUR",
                targetCurrencyCode = "THB",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = { rateApplied = true }
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
            assertTrue(rateApplied)
        }

        @Test
        fun `sets loading false on exception`() = runTest {
            coEvery {
                getExchangeRateUseCase(
                    baseCurrencyCode = "EUR",
                    targetCurrencyCode = "THB"
                )
            } throws RuntimeException("Network error")

            val addOn = AddOnUiModel(id = "addon-1", currency = thbCurrency)
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var latestAddOn = addOn

            delegate.fetchRate(
                addOnId = "addon-1",
                baseCurrencyCode = "EUR",
                targetCurrencyCode = "THB",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
        }

        @Test
        fun `ignores stale result when currency changed`() = runTest {
            coEvery {
                getExchangeRateUseCase(
                    baseCurrencyCode = "EUR",
                    targetCurrencyCode = "THB"
                )
            } returns ExchangeRateWithStaleness(rate = BigDecimal("0.027"), isStale = false)

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                showExchangeRateSection = true,
                amountInput = "1000"
            )
            // State has been changed: add-on now uses EUR (not THB)
            val changedAddOn = addOn.copy(currency = eurCurrency)
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(changedAddOn)
                )
            )
            var latestAddOn = addOn

            delegate.fetchRate(
                addOnId = "addon-1",
                baseCurrencyCode = "EUR",
                targetCurrencyCode = "THB",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            // Should only set isLoadingRate to false, not apply the rate
            assertFalse(latestAddOn.isLoadingRate)
        }

        @Test
        fun `does not call onRateApplied when rate is null`() = runTest {
            coEvery {
                getExchangeRateUseCase(
                    baseCurrencyCode = "EUR",
                    targetCurrencyCode = "THB"
                )
            } returns null

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                showExchangeRateSection = true,
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var rateApplied = false

            delegate.fetchRate(
                addOnId = "addon-1",
                baseCurrencyCode = "EUR",
                targetCurrencyCode = "THB",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> transform(addOn) },
                onRateApplied = { rateApplied = true }
            )

            advanceUntilIdle()

            assertFalse(rateApplied)
        }
    }

    // ── fetchCashRate ────────────────────────────────────────────────────

    @Nested
    @DisplayName("fetchCashRate")
    inner class FetchCashRate {

        private fun foreignCashState(addOn: AddOnUiModel) = AddExpenseUiState(
            loadedGroupId = "group-1",
            groupCurrency = eurCurrency,
            selectedCurrency = thbCurrency,
            selectedPaymentMethod = cashMethod,
            addOns = persistentListOf(addOn)
        )

        @Test
        fun `no-ops when add-on not found`() = runTest {
            val stateFlow = MutableStateFlow(AddExpenseUiState(addOns = persistentListOf()))
            var wasUpdated = false

            delegate.fetchCashRate(
                addOnId = "missing",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, _ -> wasUpdated = true },
                onRateApplied = {}
            )
            advanceUntilIdle()

            assertFalse(wasUpdated)
        }

        @Test
        fun `no-ops when groupId is null`() = runTest {
            val addOn = AddOnUiModel(id = "addon-1", currency = thbCurrency)
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    loadedGroupId = null,
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var wasUpdated = false

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, _ -> wasUpdated = true },
                onRateApplied = {}
            )
            advanceUntilIdle()

            assertFalse(wasUpdated)
        }

        @Test
        fun `no-ops when source currency matches group currency`() = runTest {
            val addOn = AddOnUiModel(id = "addon-1", currency = eurCurrency)
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    loadedGroupId = "group-1",
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var wasUpdated = false

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, _ -> wasUpdated = true },
                onRateApplied = {}
            )
            advanceUntilIdle()

            assertFalse(wasUpdated)
        }

        @Test
        fun `applies available cash rate result`() = runTest {
            val preview = CashRatePreview(
                displayRate = BigDecimal("37.0"),
                groupAmountCents = 2703L
            )
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } returns CashRatePreviewResult.Available(preview)

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000",
                resolvedAmountCents = 100000L
            )
            val stateFlow = MutableStateFlow(foreignCashState(addOn))
            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
            assertTrue(latestAddOn.isExchangeRateLocked)
            assertFalse(latestAddOn.isInsufficientCash)
        }

        @Test
        fun `applies insufficient cash result with locked rate`() = runTest {
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } returns CashRatePreviewResult.InsufficientCash

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(foreignCashState(addOn))
            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
            assertTrue(latestAddOn.isExchangeRateLocked)
            assertTrue(latestAddOn.isInsufficientCash)
            assertEquals("—", latestAddOn.displayExchangeRate)
        }

        @Test
        fun `applies no withdrawals result`() = runTest {
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } returns CashRatePreviewResult.NoWithdrawals

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(foreignCashState(addOn))
            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
            assertTrue(latestAddOn.isExchangeRateLocked)
            assertFalse(latestAddOn.isInsufficientCash)
            assertEquals("—", latestAddOn.displayExchangeRate)
        }

        @Test
        fun `handles exception gracefully`() = runTest {
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } throws RuntimeException("DB error")

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000"
            )
            val stateFlow = MutableStateFlow(foreignCashState(addOn))
            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertFalse(latestAddOn.isLoadingRate)
        }

        @Test
        fun `ignores stale result when group changed`() = runTest {
            val preview = CashRatePreview(
                displayRate = BigDecimal("37.0"),
                groupAmountCents = 2703L
            )

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000"
            )
            // stateFlow starts with group-1 so fetchCashRate captures requestedGroupId="group-1"
            val stateFlow = MutableStateFlow(foreignCashState(addOn))

            // While the use-case is "in-flight", simulate a group navigation change
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } answers {
                // Group changes between request start and result delivery
                stateFlow.value = stateFlow.value.copy(loadedGroupId = "group-2")
                CashRatePreviewResult.Available(preview)
            }

            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            // Stale check fires: requestedGroupId="group-1" != current "group-2"
            // → only isLoadingRate reset, rate must NOT be locked
            assertFalse(latestAddOn.isLoadingRate)
            assertFalse(latestAddOn.isExchangeRateLocked)
        }

        @Test
        fun `available result with zero groupAmountCents shows empty formattedAmount`() = runTest {
            val preview = CashRatePreview(
                displayRate = BigDecimal("37.0"),
                groupAmountCents = 0L
            )
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } returns CashRatePreviewResult.Available(preview)

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = ""
            )
            val stateFlow = MutableStateFlow(foreignCashState(addOn))
            var latestAddOn = addOn

            delegate.fetchCashRate(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = {}
            )

            advanceUntilIdle()

            assertEquals("", latestAddOn.calculatedGroupAmount)
            assertTrue(latestAddOn.isExchangeRateLocked)
        }
    }

    // ── cancelPendingJobs ────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelPendingJobs")
    inner class CancelPendingJobs {

        @Test
        fun `does not throw when no jobs exist`() {
            // Should simply no-op without exceptions
            delegate.cancelPendingJobs("nonexistent-id")
        }
    }

    // ── recalculateCashForward ───────────────────────────────────────────

    @Nested
    @DisplayName("recalculateCashForward")
    inner class RecalculateCashForward {

        @Test
        fun `debounces and eventually fetches cash rate`() = runTest {
            val preview = CashRatePreview(
                displayRate = BigDecimal("37.0"),
                groupAmountCents = 2703L
            )
            coEvery {
                previewCashExchangeRateUseCase(
                    groupId = "group-1",
                    sourceCurrency = "THB",
                    sourceAmountCents = any()
                )
            } returns CashRatePreviewResult.Available(preview)

            val addOn = AddOnUiModel(
                id = "addon-1",
                currency = thbCurrency,
                amountInput = "1000",
                resolvedAmountCents = 100000L
            )
            val stateFlow = MutableStateFlow(
                AddExpenseUiState(
                    loadedGroupId = "group-1",
                    groupCurrency = eurCurrency,
                    addOns = persistentListOf(addOn)
                )
            )
            var latestAddOn = addOn
            var rateApplied = false

            delegate.recalculateCashForward(
                addOnId = "addon-1",
                scope = this,
                stateFlow = stateFlow,
                updateAddOn = { _, transform -> latestAddOn = transform(latestAddOn) },
                onRateApplied = { rateApplied = true }
            )

            advanceUntilIdle()

            // After debounce + fetch, the rate should be applied
            assertTrue(rateApplied)
            assertTrue(latestAddOn.isExchangeRateLocked)
        }
    }
}
