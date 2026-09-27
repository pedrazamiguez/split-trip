package es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.action.AddCashWithdrawalUiAction
import es.pedrazamiguez.splittrip.features.withdrawal.presentation.viewmodel.state.AddCashWithdrawalUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WithdrawalSubmitHandlerTest {

    private lateinit var handler: WithdrawalSubmitHandler
    private lateinit var addCashWithdrawalUseCase: AddCashWithdrawalUseCase
    private lateinit var cashWithdrawalValidationService: CashWithdrawalValidationService
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService

    private lateinit var uiState: MutableStateFlow<AddCashWithdrawalUiState>
    private lateinit var actions: MutableSharedFlow<AddCashWithdrawalUiAction>

    private val eurModel = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)

    /** Minimal valid state: 100.00 EUR, no exchange rate, no fee. */
    private val validState = AddCashWithdrawalUiState(
        isConfigLoaded = true,
        groupCurrency = eurModel,
        selectedCurrency = eurModel,
        withdrawalAmount = "100.00",
        isAmountValid = true,
        showExchangeRateSection = false,
        hasFee = false,
        selectedMemberId = "user-1"
    )

    @BeforeEach
    fun setUp() {
        addCashWithdrawalUseCase = mockk()
        cashWithdrawalValidationService = mockk()
        exchangeRateCalculationService = mockk(relaxed = true)

        uiState = MutableStateFlow(validState)
        actions = MutableSharedFlow(extraBufferCapacity = 16)

        handler = WithdrawalSubmitHandler(
            addCashWithdrawalUseCase = addCashWithdrawalUseCase,
            cashWithdrawalValidationService = cashWithdrawalValidationService,
            exchangeRateCalculationService = exchangeRateCalculationService
        )

        // Stubs — default to valid
        every {
            cashWithdrawalValidationService.validateAmountWithdrawn(any())
        } returns CashWithdrawalValidationService.ValidationResult.Valid
        every {
            exchangeRateCalculationService.calculateExchangeRate(any(), any())
        } returns BigDecimal.ONE
    }

    // ── Early-exit guards ─────────────────────────────────────────────────

    @Nested
    inner class EarlyExitGuards {

        @Test
        fun `does nothing when groupId is null`() = runTest {
            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal(null) { onSuccessCalled = true }
            advanceUntilIdle()

            assertFalse(onSuccessCalled)
        }

        @Test
        fun `does nothing when selectedCurrency is null`() = runTest {
            uiState.value = validState.copy(selectedCurrency = null)
            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertFalse(onSuccessCalled)
        }

        @Test
        fun `does nothing when groupCurrency is null`() = runTest {
            uiState.value = validState.copy(groupCurrency = null)
            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertFalse(onSuccessCalled)
        }
    }

    // ── Input validation ──────────────────────────────────────────────────

    @Nested
    inner class InputValidation {

        @Test
        fun `sets isAmountValid to false when amount is zero`() = runTest {
            every {
                cashWithdrawalValidationService.validateAmountWithdrawn(0L)
            } returns CashWithdrawalValidationService.ValidationResult.Invalid(
                CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE
            )
            uiState.value = validState.copy(withdrawalAmount = "0.00")

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `sets isAmountValid to false when amount is negative`() = runTest {
            every {
                cashWithdrawalValidationService.validateAmountWithdrawn(any())
            } returns CashWithdrawalValidationService.ValidationResult.Invalid(
                CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE
            )
            uiState.value = validState.copy(withdrawalAmount = "-50.00")

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertFalse(uiState.value.isAmountValid)
        }

        @Test
        fun `does not call use case when amount validation fails`() = runTest {
            every {
                cashWithdrawalValidationService.validateAmountWithdrawn(any())
            } returns CashWithdrawalValidationService.ValidationResult.Invalid(
                CashWithdrawalValidationService.ValidationError.AMOUNT_MUST_BE_POSITIVE
            )
            uiState.value = validState.copy(withdrawalAmount = "0.00")

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertFalse(onSuccessCalled)
        }

        @Test
        fun `sets isFeeAmountValid to false when fee amount is zero and fee is enabled`() = runTest {
            uiState.value = validState.copy(
                hasFee = true,
                feeAmount = "0.00",
                feeCurrency = eurModel
            )

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertFalse(uiState.value.isFeeAmountValid)
        }

        @Test
        fun `skips fee validation when hasFee is false`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)
            uiState.value = validState.copy(hasFee = false, feeAmount = "")

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertTrue(onSuccessCalled)
        }

        @Test
        fun `skips fee validation when feeAmount is blank`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)
            uiState.value = validState.copy(hasFee = true, feeAmount = "")

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertTrue(onSuccessCalled)
        }
    }

    // ── Successful submission ─────────────────────────────────────────────

    @Nested
    inner class SuccessfulSubmission {

        @Test
        fun `calls onSuccess callback on successful submission`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertTrue(onSuccessCalled)
        }

        @Test
        fun `isLoading is set to true when submission starts and stays true on success`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)

            handler.bind(uiState, actions, this)
            assertFalse(uiState.value.isLoading) // initially false
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            // On success, the Feature navigates away via onSuccess() — isLoading is intentionally
            // NOT reset to false because the screen is dismissed.
            assertTrue(uiState.value.isLoading)
        }

        @Test
        fun `sets withdrawnBy from selectedMemberId`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)
            uiState.value = validState.copy(selectedMemberId = "user-2")

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            assertEquals("user-2", withdrawalSlot.captured.withdrawnBy)
        }

        @Test
        fun `sets withdrawnBy to empty string when selectedMemberId is null`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)
            uiState.value = validState.copy(selectedMemberId = null)

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            assertEquals("", withdrawalSlot.captured.withdrawnBy)
        }

        @Test
        fun `submitting large raw withdrawal amount parses correctly to cents`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)
            uiState.value = validState.copy(withdrawalAmount = "10450")

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            assertEquals(1045000L, withdrawalSlot.captured.amountWithdrawn)
        }
    }

    // ── Failed submission ─────────────────────────────────────────────────

    @Nested
    inner class FailedSubmission {

        @Test
        fun `emits ShowError action on exception from use case`() = runTest {
            coEvery {
                addCashWithdrawalUseCase(any(), any())
            } returns Result.failure(RuntimeException("server error"))

            val emittedActions = mutableListOf<AddCashWithdrawalUiAction>()
            val job = launch { actions.collect { emittedActions.add(it) } }

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(emittedActions.any { it is AddCashWithdrawalUiAction.ShowError })
            job.cancel()
        }

        @Test
        fun `sets isLoading to false after submission failure`() = runTest {
            coEvery {
                addCashWithdrawalUseCase(any(), any())
            } returns Result.failure(RuntimeException("error"))

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertFalse(uiState.value.isLoading)
        }

        @Test
        fun `does not call onSuccess on submission failure`() = runTest {
            coEvery {
                addCashWithdrawalUseCase(any(), any())
            } returns Result.failure(RuntimeException("error"))

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertFalse(onSuccessCalled)
        }
    }

    // ── Exchange-rate handling ─────────────────────────────────────────────

    @Nested
    inner class ExchangeRateHandling {

        @Test
        fun `uses exchange rate of ONE when same currency`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)
            uiState.value = validState.copy(showExchangeRateSection = false)

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            // No crash, onSuccess is called — exchange rate resolves to BigDecimal.ONE
            assertTrue(onSuccessCalled)
        }

        @Test
        fun `uses deductedAmount from state when foreign currency selected`() = runTest {
            coEvery { addCashWithdrawalUseCase(any(), any()) } returns Result.success(Unit)
            val thbModel = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)
            uiState.value = validState.copy(
                selectedCurrency = thbModel,
                showExchangeRateSection = true,
                withdrawalAmount = "1000",
                deductedAmount = "27.03",
                displayExchangeRate = "37.0"
            )

            handler.bind(uiState, actions, this)
            var onSuccessCalled = false
            handler.submitWithdrawal("group-1") { onSuccessCalled = true }
            advanceUntilIdle()

            assertTrue(onSuccessCalled)
        }
    }

    // ── Fee add-on building ─────────────────────────────────────────────────

    @Nested
    inner class FeeAddOn {

        @Test
        fun `includes FEE add-on with same currency when fee is enabled`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)

            uiState.value = validState.copy(
                hasFee = true,
                feeAmount = "5.00",
                feeCurrency = eurModel,
                showFeeExchangeRateSection = false
            )

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            val addOns = withdrawalSlot.captured.addOns
            assertEquals(1, addOns.size)
            assertEquals(AddOnType.FEE, addOns[0].type)
            assertEquals(500L, addOns[0].amountCents)
            assertEquals(BigDecimal.ONE, addOns[0].exchangeRate)
        }

        @Test
        fun `includes FEE add-on with foreign currency converted amount`() = runTest {
            val thbModel = CurrencyUiModel(code = "THB", displayText = "THB (฿)", decimalDigits = 2)
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)
            every {
                exchangeRateCalculationService.calculateExchangeRate(any(), any())
            } returns BigDecimal("37.0")

            uiState.value = validState.copy(
                hasFee = true,
                feeAmount = "100",
                feeCurrency = thbModel,
                showFeeExchangeRateSection = true,
                feeConvertedAmount = "2.70"
            )

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            val addOns = withdrawalSlot.captured.addOns
            assertEquals(1, addOns.size)
            assertEquals("THB", addOns[0].currency)
            assertEquals(270L, addOns[0].groupAmountCents)
        }

        @Test
        fun `returns empty add-ons when fee amount is blank`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)

            uiState.value = validState.copy(
                hasFee = true,
                feeAmount = ""
            )

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            assertTrue(withdrawalSlot.captured.addOns.isEmpty())
        }

        @Test
        fun `returns empty add-ons when hasFee is false`() = runTest {
            val withdrawalSlot = slot<CashWithdrawal>()
            coEvery { addCashWithdrawalUseCase(any(), capture(withdrawalSlot)) } returns Result.success(Unit)

            uiState.value = validState.copy(hasFee = false)

            handler.bind(uiState, actions, this)
            handler.submitWithdrawal("group-1") {}
            advanceUntilIdle()

            assertTrue(withdrawalSlot.isCaptured)
            assertTrue(withdrawalSlot.captured.addOns.isEmpty())
        }
    }
}
