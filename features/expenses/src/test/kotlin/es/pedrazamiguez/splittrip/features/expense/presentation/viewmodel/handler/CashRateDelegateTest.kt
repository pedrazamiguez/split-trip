package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.model.CashRatePreview
import es.pedrazamiguez.splittrip.domain.model.CashRatePreviewResult
import es.pedrazamiguez.splittrip.domain.model.CashTranchePreview
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.service.split.impl.SplitPreviewServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.PreviewCashExchangeRateUseCase
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.FundingSourceUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CashRateDelegateTest {

    private lateinit var previewCashExchangeRateUseCase: PreviewCashExchangeRateUseCase
    private lateinit var delegate: CashRateDelegate
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val thbCurrency = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)
    private val cashPaymentMethod = PaymentMethodUiModel(id = "CASH", displayText = "Cash")
    private val groupFundingSource = FundingSourceUiModel(id = "GROUP", displayText = "Group Pocket")

    private val foreignCashState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = thbCurrency,
        selectedPaymentMethod = cashPaymentMethod,
        selectedFundingSource = groupFundingSource,
        showExchangeRateSection = true,
        sourceAmount = "100"
    )

    private val sameCurrencyCashState = AddExpenseUiState(
        loadedGroupId = "group-1",
        groupCurrency = eurCurrency,
        selectedCurrency = eurCurrency,
        selectedPaymentMethod = cashPaymentMethod,
        selectedFundingSource = groupFundingSource,
        showExchangeRateSection = false,
        displayExchangeRate = "1.0",
        calculatedGroupAmount = "",
        sourceAmount = "50"
    )

    @BeforeEach
    fun setUp() {
        previewCashExchangeRateUseCase = mockk()
        val resourceProvider = mockk<ResourceProvider>(relaxed = true)
        val formattingHelper = mockk<FormattingHelper>(relaxed = true)
        val optionsMapper = AddExpenseOptionsUiMapper(resourceProvider, formattingHelper)

        delegate = CashRateDelegate(
            previewCashExchangeRateUseCase = previewCashExchangeRateUseCase,
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            splitPreviewService = SplitPreviewServiceImpl(),
            addExpenseOptionsMapper = optionsMapper
        )
        uiState = MutableStateFlow(foreignCashState)
    }

    @Test
    fun `fetchCashRate sets isNoCashAvailable and no-withdrawals hint on foreign currency NoWithdrawals`() = runTest {
        coEvery {
            previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns CashRatePreviewResult.NoWithdrawals

        delegate.setup(uiState, this)
        delegate.fetchCashRate()
        advanceUntilIdle()

        val state = uiState.value
        assertTrue(state.isNoCashAvailable)
        assertFalse(state.isInsufficientCash)
        assertEquals(CashRateDelegate.EMPTY_FIELD_PLACEHOLDER, state.displayExchangeRate)
        assertEquals(CashRateDelegate.EMPTY_FIELD_PLACEHOLDER, state.calculatedGroupAmount)
        assertFalse(state.isExchangeRateValid)
        assertFalse(state.isGroupAmountValid)
        assertTrue(state.isExchangeRateLocked)
        assertEquals(
            UiText.StringResource(R.string.add_expense_cash_no_withdrawals_hint, "THB"),
            state.exchangeRateLockedHint
        )
        assertTrue(state.cashTranchePreviews.isEmpty())
    }

    @Test
    fun `fetchCashRate sets isNoCashAvailable without overwriting rate fields on same currency NoWithdrawals`() =
        runTest {
            uiState.value = sameCurrencyCashState
            coEvery {
                previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
            } returns CashRatePreviewResult.NoWithdrawals

            delegate.setup(uiState, this)
            delegate.fetchCashRate()
            advanceUntilIdle()

            val state = uiState.value
            assertTrue(state.isNoCashAvailable)
            assertFalse(state.isInsufficientCash)
            assertFalse(state.isExchangeRateLocked)
            assertEquals("1.0", state.displayExchangeRate)
            assertEquals("", state.calculatedGroupAmount)
        }

    @Test
    fun `mapCashRateResult uses generic no-withdrawals hint when sourceCurrencyCode is blank`() {
        val result = delegate.mapCashRateResult(
            current = foreignCashState,
            result = CashRatePreviewResult.NoWithdrawals,
            targetDecimalDigits = 2,
            sourceCurrencyCode = "",
            updateRateFields = true
        )

        assertTrue(result.isNoCashAvailable)
        assertFalse(result.isInsufficientCash)
        assertEquals(
            UiText.StringResource(R.string.add_expense_cash_no_withdrawals_generic_hint),
            result.exchangeRateLockedHint
        )
    }

    @Test
    fun `fetchCashRate sets isInsufficientCash true and isNoCashAvailable false on InsufficientCash`() = runTest {
        coEvery {
            previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns CashRatePreviewResult.InsufficientCash

        delegate.setup(uiState, this)
        delegate.fetchCashRate()
        advanceUntilIdle()

        val state = uiState.value
        assertTrue(state.isInsufficientCash)
        assertFalse(state.isNoCashAvailable)
        assertEquals(
            UiText.StringResource(R.string.add_expense_cash_insufficient_hint),
            state.exchangeRateLockedHint
        )
    }

    @Test
    fun `fetchCashRate clears isNoCashAvailable and isInsufficientCash when Available is returned`() = runTest {
        uiState.value = foreignCashState.copy(isNoCashAvailable = true, isInsufficientCash = true)
        coEvery {
            previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns CashRatePreviewResult.Available(
            CashRatePreview(
                displayRate = BigDecimal("37.00"),
                groupAmountCents = 270L,
                tranches = listOf(
                    CashTranchePreview(
                        withdrawalId = "w1",
                        withdrawalTitle = "ATM",
                        withdrawalDate = LocalDateTime.of(2026, 1, 1, 12, 0),
                        amountConsumedCents = 10000L,
                        remainingAfterCents = 5000L,
                        withdrawalRate = BigDecimal("37.00")
                    )
                )
            )
        )

        delegate.setup(uiState, this)
        delegate.fetchCashRate()
        advanceUntilIdle()

        val state = uiState.value
        assertFalse(state.isNoCashAvailable)
        assertFalse(state.isInsufficientCash)
        assertEquals("37", state.displayExchangeRate)
        assertEquals("2.70", state.calculatedGroupAmount)
        assertEquals(1, state.cashTranchePreviews.size)
    }

    @Test
    fun `fetchCashRate cancels pending debounced recalculateCashForward job`() = runTest {
        coEvery {
            previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
        } returns CashRatePreviewResult.Available(
            CashRatePreview(displayRate = BigDecimal("37.00"), groupAmountCents = 270L)
        )

        delegate.setup(uiState, this)
        delegate.recalculateCashForward()
        delegate.fetchCashRate()
        advanceUntilIdle()

        coVerify(exactly = 1) {
            previewCashExchangeRateUseCase(any(), any(), any(), any(), any(), any(), any())
        }
    }
}
