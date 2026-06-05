package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.result.ExchangeRateWithStaleness
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.mapper.AddCashWithdrawalUiMapper
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.math.BigDecimal
import kotlinx.collections.immutable.toImmutableList
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
class WithdrawalCurrencyHandlerTest {

    private lateinit var handler: WithdrawalCurrencyHandler
    private lateinit var getExchangeRateUseCase: GetExchangeRateUseCase
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var addCashWithdrawalUiMapper: AddCashWithdrawalUiMapper
    private lateinit var formattingHelper: FormattingHelper

    private lateinit var uiState: MutableStateFlow<AddCashWithdrawalUiState>
    private lateinit var actions: MutableSharedFlow<AddCashWithdrawalUiAction>

    private val eurModel = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usdModel = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)
    private val thbModel = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)

    private val baseState = AddCashWithdrawalUiState(
        isConfigLoaded = true,
        groupCurrency = eurModel,
        selectedCurrency = eurModel,
        availableCurrencies = listOf(eurModel, usdModel, thbModel).toImmutableList()
    )

    @BeforeEach
    fun setUp() {
        getExchangeRateUseCase = mockk(relaxed = true)
        exchangeRateCalculationService = mockk(relaxed = true)
        addCashWithdrawalUiMapper = mockk(relaxed = true)
        formattingHelper = mockk(relaxed = true)

        // Default calculation stubs
        every {
            exchangeRateCalculationService.calculateGroupAmountFromDisplayRate(any(), any(), any(), any())
        } returns "27.03"
        every {
            exchangeRateCalculationService.calculateImpliedDisplayRateFromStrings(any(), any(), any())
        } returns "37.037"
        every { formattingHelper.formatForDisplay(any(), any(), any()) } returns "27.03"
        every { formattingHelper.formatRateForDisplay(any()) } returns "37.037"

        uiState = MutableStateFlow(baseState)
        actions = MutableSharedFlow(extraBufferCapacity = 1)

        handler = WithdrawalCurrencyHandler(
            getExchangeRateUseCase = getExchangeRateUseCase,
            exchangeRateCalculationService = exchangeRateCalculationService,
            addCashWithdrawalUiMapper = addCashWithdrawalUiMapper,
            formattingHelper = formattingHelper
        )
    }

    // ── CurrencySelected ──────────────────────────────────────────────────

    @Nested
    inner class CurrencySelected {

        @Test
        fun `selecting the group currency does not show exchange rate section`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("EUR")
            advanceUntilIdle()

            assertFalse(uiState.value.showExchangeRateSection)
        }

        @Test
        fun `selecting the group currency resets displayExchangeRate to 1`() = runTest {
            uiState.value = baseState.copy(displayExchangeRate = "37.0", showExchangeRateSection = true)
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("EUR")
            advanceUntilIdle()

            assertEquals("1.0", uiState.value.displayExchangeRate)
        }

        @Test
        fun `selecting the group currency resets deductedAmount`() = runTest {
            uiState.value = baseState.copy(deductedAmount = "27.03")
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("EUR")
            advanceUntilIdle()

            assertEquals("", uiState.value.deductedAmount)
        }

        @Test
        fun `selecting a foreign currency shows exchange rate section`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            assertTrue(uiState.value.showExchangeRateSection)
        }

        @Test
        fun `selecting a foreign currency updates selectedCurrency`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            assertEquals("THB", uiState.value.selectedCurrency?.code)
        }

        @Test
        fun `selecting a foreign currency fetches exchange rate`() = runTest {
            coEvery { getExchangeRateUseCase(any(), any()) } returns ExchangeRateWithStaleness(
                rate = BigDecimal("37.037"),
                isStale = false
            )
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            // Rate fetch should have completed — isLoadingRate is false
            assertFalse(uiState.value.isLoadingRate)
        }

        @Test
        fun `selecting a currency not in availableCurrencies does not change state`() = runTest {
            val originalState = uiState.value
            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("XYZ")
            advanceUntilIdle()

            assertEquals(originalState, uiState.value)
        }

        @Test
        fun `selecting EUR when groupCurrency is null does nothing`() = runTest {
            uiState.value = baseState.copy(groupCurrency = null)
            val originalSelectedCurrency = uiState.value.selectedCurrency
            handler.bind(uiState, actions, this)

            // EUR is not in the state's availableCurrencies when groupCurrency is missing…
            // but the handler should still work — verify no crash
            handler.handleCurrencySelected("EUR")
            advanceUntilIdle()

            // selectedCurrency should remain unchanged
            assertEquals(originalSelectedCurrency, uiState.value.selectedCurrency)
        }
    }

    // ── WithdrawalAmountChanged ───────────────────────────────────────────

    @Nested
    inner class WithdrawalAmountChanged {

        @Test
        fun `updates withdrawalAmount in state`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("100.00")
            advanceUntilIdle()

            assertEquals("100.00", uiState.value.withdrawalAmount)
        }

        @Test
        fun `valid numeric input keeps isAmountValid true`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("250.00")
            advanceUntilIdle()

            assertTrue(uiState.value.isAmountValid)
        }

        @Test
        fun `non-numeric input sets isAmountValid to false`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("abc")
            advanceUntilIdle()

            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `blank input is valid (clearing the field)`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("")
            advanceUntilIdle()

            assertTrue(uiState.value.isAmountValid)
        }

        @Test
        fun `clears error when amount changes`() = runTest {
            uiState.value = baseState.copy(error = mockk())
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("100.00")
            advanceUntilIdle()

            assertNull(uiState.value.error)
        }

        @Test
        fun `comma decimal input is valid`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("12,36")
            advanceUntilIdle()

            assertTrue(uiState.value.isAmountValid)
            assertEquals("12,36", uiState.value.withdrawalAmount)
        }

        @Test
        fun `European thousands with comma decimal is valid`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("1.234,56")
            advanceUntilIdle()

            assertTrue(uiState.value.isAmountValid)
            assertEquals("1.234,56", uiState.value.withdrawalAmount)
        }

        @Test
        fun `double dot input is valid because normalizer treats last dot as decimal`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("12..36")
            advanceUntilIdle()

            assertTrue(uiState.value.isAmountValid)
        }

        @Test
        fun `mixed letters and digits input is invalid`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleWithdrawalAmountChanged("12a36")
            advanceUntilIdle()

            assertFalse(uiState.value.isAmountValid)
        }
    }

    // ── ExchangeRateChanged ───────────────────────────────────────────────

    @Nested
    inner class ExchangeRateChanged {

        @Test
        fun `updates displayExchangeRate in state`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleExchangeRateChanged("37.5")
            advanceUntilIdle()

            assertEquals("37.5", uiState.value.displayExchangeRate)
        }

        @Test
        fun `triggers recalculation when exchange rate section is shown`() = runTest {
            uiState.value = baseState.copy(
                showExchangeRateSection = true,
                withdrawalAmount = "1000",
                selectedCurrency = thbModel,
                groupCurrency = eurModel
            )
            handler.bind(uiState, actions, this)
            handler.handleExchangeRateChanged("37.0")
            advanceUntilIdle()

            // deductedAmount should be recalculated (formattingHelper.formatForDisplay stub returns "27.03")
            assertEquals("27.03", uiState.value.deductedAmount)
        }
    }

    // ── DeductedAmountChanged ─────────────────────────────────────────────

    @Nested
    inner class DeductedAmountChanged {

        @Test
        fun `updates deductedAmount in state`() = runTest {
            handler.bind(uiState, actions, this)
            handler.handleDeductedAmountChanged("27.00")
            advanceUntilIdle()

            assertEquals("27.00", uiState.value.deductedAmount)
        }

        @Test
        fun `triggers implied rate recalculation when exchange rate section is shown`() = runTest {
            uiState.value = baseState.copy(
                showExchangeRateSection = true,
                withdrawalAmount = "1000",
                selectedCurrency = thbModel
            )
            handler.bind(uiState, actions, this)
            handler.handleDeductedAmountChanged("27.00")
            advanceUntilIdle()

            // displayExchangeRate should be recalculated (formattingHelper.formatRateForDisplay stub returns "37.037")
            assertEquals("37.037", uiState.value.displayExchangeRate)
        }
    }

    // ── FetchRate (private, exercised via handleCurrencySelected) ─────────

    @Nested
    inner class FetchRate {

        @Test
        fun `rateResult null sets exchange rate to empty and flags error`() = runTest {
            val originalRate = "37.5"
            uiState.value = baseState.copy(
                selectedCurrency = thbModel,
                displayExchangeRate = originalRate
            )
            coEvery { getExchangeRateUseCase(any(), any()) } returns null

            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            assertEquals("", uiState.value.displayExchangeRate)
            assertTrue(uiState.value.isExchangeRateError)
        }

        @Test
        fun `rateResult null skips async recalculate deducted so formatForDisplay is not called`() = runTest {
            // handleCurrencySelected always calls recalculateDeducted() synchronously.
            // Since displayExchangeRate is empty, it returns early.
            // When rateResult == null, the async call is skipped and rate stays empty — verify 0 invocations.
            uiState.value = baseState.copy(selectedCurrency = thbModel, showExchangeRateSection = true)
            coEvery { getExchangeRateUseCase(any(), any()) } returns null

            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            // Deducted amount calculation is skipped because exchange rate is empty.
            verify(exactly = 0) { formattingHelper.formatForDisplay(any(), any(), any()) }
        }

        @Test
        fun `exception during rate fetch sets isLoadingRate to false`() = runTest {
            uiState.value = baseState.copy(selectedCurrency = thbModel)
            coEvery { getExchangeRateUseCase(any(), any()) } throws IOException("Network error")

            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            assertFalse(uiState.value.isLoadingRate)
        }

        @Test
        fun `stale rate sets isExchangeRateStale to true`() = runTest {
            uiState.value = baseState.copy(selectedCurrency = thbModel)
            coEvery { getExchangeRateUseCase(any(), any()) } returns ExchangeRateWithStaleness(
                rate = BigDecimal("37.037"),
                isStale = true
            )

            handler.bind(uiState, actions, this)
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            assertTrue(uiState.value.isExchangeRateStale)
        }
    }

    // ── ExchangeRateErrorHandling ────────────────────────────────────────────

    @Nested
    inner class ExchangeRateErrorHandling {

        @Test
        fun `successful fetch rate resets error flag to false`() = runTest {
            uiState.value = baseState.copy(selectedCurrency = thbModel, isExchangeRateError = true)
            coEvery {
                getExchangeRateUseCase(any(), any())
            } returns ExchangeRateWithStaleness(rate = BigDecimal("37.037"), isStale = false)

            handler.bind(uiState, actions, this)

            // When
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
        }

        @Test
        fun `null rateResult from fetch rate sets error flag to true`() = runTest {
            uiState.value =
                baseState.copy(selectedCurrency = thbModel, isExchangeRateError = false, displayExchangeRate = "")
            coEvery {
                getExchangeRateUseCase(any(), any())
            } returns null

            handler.bind(uiState, actions, this)

            // When
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertTrue(state.isExchangeRateError)
            assertEquals("", state.displayExchangeRate)
        }

        @Test
        fun `exception from fetch rate sets error flag to true`() = runTest {
            uiState.value =
                baseState.copy(selectedCurrency = thbModel, isExchangeRateError = false, displayExchangeRate = "")
            coEvery {
                getExchangeRateUseCase(any(), any())
            } throws IOException("Network error")

            handler.bind(uiState, actions, this)

            // When
            handler.handleCurrencySelected("THB")
            advanceUntilIdle()

            // Then
            val state = uiState.value
            assertTrue(state.isExchangeRateError)
            assertEquals("", state.displayExchangeRate)
        }

        @Test
        fun `manual exchange rate change resets error flag to false`() = runTest {
            uiState.value = baseState.copy(isExchangeRateError = true)
            handler.bind(uiState, actions, this)

            // When
            handler.handleExchangeRateChanged("35.5")

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
            assertEquals("35.5", state.displayExchangeRate)
        }

        @Test
        fun `manual deducted amount change resets error flag to false`() = runTest {
            uiState.value = baseState.copy(isExchangeRateError = true, displayExchangeRate = "")
            handler.bind(uiState, actions, this)

            // When
            handler.handleDeductedAmountChanged("27.00")

            // Then
            val state = uiState.value
            assertFalse(state.isExchangeRateError)
        }

        @Test
        fun `recalculateDeducted leaves deducted amount empty if rate is blank`() = runTest {
            uiState.value =
                baseState.copy(displayExchangeRate = "", deductedAmount = "10.00", showExchangeRateSection = true)
            handler.bind(uiState, actions, this)

            // When
            handler.recalculateDeducted()

            // Then
            val state = uiState.value
            assertEquals("", state.deductedAmount)
        }
    }
}
