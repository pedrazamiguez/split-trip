package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.exception.InsufficientCashException
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategoryUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SubmitResultDelegate].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubmitResultDelegateTest {

    private lateinit var delegate: SubmitResultDelegate
    private lateinit var savePrefs: SaveLastUsedPreferencesBundle
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actionsFlow: MutableSharedFlow<AddExpenseUiAction>

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val paymentMethod = PaymentMethodUiModel(id = "CARD", displayText = "Card")
    private val category = CategoryUiModel(id = "FOOD", displayText = "Food")

    @BeforeEach
    fun setUp() {
        savePrefs = SaveLastUsedPreferencesBundle(
            setGroupLastUsedCurrencyUseCase = mockk(relaxed = true),
            setGroupLastUsedPaymentMethodUseCase = mockk(relaxed = true),
            setGroupLastUsedCategoryUseCase = mockk(relaxed = true)
        )
        formattingHelper = mockk(relaxed = true)

        delegate = SubmitResultDelegate(
            saveLastUsedPreferences = savePrefs,
            formattingHelper = formattingHelper
        )

        uiState = MutableStateFlow(
            AddExpenseUiState(
                isLoading = true,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = paymentMethod,
                selectedCategory = category
            )
        )
        actionsFlow = MutableSharedFlow()
    }

    // ── handleSuccess ────────────────────────────────────────────────────

    @Nested
    inner class HandleSuccess {

        @Test
        fun `saves currency preference`() = runTest {
            var successCalled = false
            delegate.handleSuccess(uiState, "group-1") { successCalled = true }

            coVerify { savePrefs.setGroupLastUsedCurrencyUseCase("group-1", "EUR") }
            assertTrue(successCalled)
        }

        @Test
        fun `saves payment method preference`() = runTest {
            delegate.handleSuccess(uiState, "group-1") {}

            coVerify { savePrefs.setGroupLastUsedPaymentMethodUseCase("group-1", "CARD") }
        }

        @Test
        fun `saves category preference`() = runTest {
            delegate.handleSuccess(uiState, "group-1") {}

            coVerify { savePrefs.setGroupLastUsedCategoryUseCase("group-1", "FOOD") }
        }

        @Test
        fun `clears loading state`() = runTest {
            delegate.handleSuccess(uiState, "group-1") {}

            assertFalse(uiState.value.isLoading)
        }

        @Test
        fun `null currency does not crash`() = runTest {
            uiState.value = uiState.value.copy(selectedCurrency = null)

            delegate.handleSuccess(uiState, "group-1") {}

            coVerify(exactly = 0) { savePrefs.setGroupLastUsedCurrencyUseCase(any(), any()) }
        }

        @Test
        fun `null payment method does not crash`() = runTest {
            uiState.value = uiState.value.copy(selectedPaymentMethod = null)

            delegate.handleSuccess(uiState, "group-1") {}

            coVerify(exactly = 0) { savePrefs.setGroupLastUsedPaymentMethodUseCase(any(), any()) }
        }

        @Test
        fun `null category does not crash`() = runTest {
            uiState.value = uiState.value.copy(selectedCategory = null)

            delegate.handleSuccess(uiState, "group-1") {}

            coVerify(exactly = 0) { savePrefs.setGroupLastUsedCategoryUseCase(any(), any()) }
        }
    }

    // ── handleFailure ────────────────────────────────────────────────────

    @Nested
    inner class HandleFailure {

        @Test
        fun `InsufficientCashException emits conflict resolution when preview showed available cash`() = runTest {
            every { formattingHelper.formatCentsValue(3000L, 2) } returns "30.00"
            every { formattingHelper.formatCentsWithCurrency(3000L, "EUR") } returns "€30.00"

            // isInsufficientCash = false (default) — preview was showing available cash
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.handleFailure(
                error = InsufficientCashException(requiredCents = 5000L, availableCents = 3000L),
                uiState = uiState,
                actionsFlow = actionsFlow,
                currentState = uiState.value // isInsufficientCash = false
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertEquals("30.00", action.availableAmountForInput)
            assertEquals("€30.00", action.availableAmountDisplay)
        }

        @Test
        fun `InsufficientCashException emits formatted error when preview already flagged insufficient cash`() =
            runTest {
                every { formattingHelper.formatCentsWithCurrency(5000L, "EUR") } returns "€50.00"
                every { formattingHelper.formatCentsWithCurrency(3000L, "EUR") } returns "€30.00"

                // isInsufficientCash = true — preview already told the user there was not enough cash
                val state = uiState.value.copy(isInsufficientCash = true)
                val actions = mutableListOf<AddExpenseUiAction>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    actionsFlow.collect { actions.add(it) }
                }

                delegate.handleFailure(
                    error = InsufficientCashException(requiredCents = 5000L, availableCents = 3000L),
                    uiState = uiState,
                    actionsFlow = actionsFlow,
                    currentState = state
                )

                assertEquals(1, actions.size)
                val action = actions[0] as AddExpenseUiAction.ShowError
                val resource = action.message as UiText.StringResource
                assertEquals(R.string.expense_error_insufficient_cash, resource.resId)
            }

        @Test
        fun `CashConflictException emits conflict resolution with null amounts`() = runTest {
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.handleFailure(
                error = CashConflictException(),
                uiState = uiState,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertNull(action.availableAmountForInput)
            assertNull(action.availableAmountDisplay)
        }

        @Test
        fun `generic exception emits generic error`() = runTest {
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.handleFailure(
                error = RuntimeException("boom"),
                uiState = uiState,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowError
            val resource = action.message as UiText.StringResource
            assertEquals(R.string.expense_error_addition_failed, resource.resId)
        }

        @Test
        fun `clears loading and error state`() = runTest {
            uiState.value = uiState.value.copy(isLoading = true, error = UiText.DynamicString("old"))

            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect {}
            }

            delegate.handleFailure(
                error = RuntimeException("boom"),
                uiState = uiState,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertFalse(uiState.value.isLoading)
            assertEquals(null, uiState.value.error)
        }
    }

    // ── emitInsufficientCashError ────────────────────────────────────────

    @Nested
    inner class EmitInsufficientCashError {

        @Test
        fun `emits ShowError with formatted amounts when currency present`() = runTest {
            every { formattingHelper.formatCentsWithCurrency(5000L, "EUR") } returns "€50.00"
            every { formattingHelper.formatCentsWithCurrency(3000L, "EUR") } returns "€30.00"

            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitInsufficientCashError(
                error = InsufficientCashException(requiredCents = 5000L, availableCents = 3000L),
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val resource = (actions[0] as AddExpenseUiAction.ShowError).message as UiText.StringResource
            assertEquals(R.string.expense_error_insufficient_cash, resource.resId)
        }

        @Test
        fun `null currency emits generic error`() = runTest {
            val state = uiState.value.copy(selectedCurrency = null)
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitInsufficientCashError(
                error = InsufficientCashException(requiredCents = 5000L, availableCents = 3000L),
                actionsFlow = actionsFlow,
                currentState = state
            )

            assertEquals(1, actions.size)
            val resource = (actions[0] as AddExpenseUiAction.ShowError).message as UiText.StringResource
            assertEquals(R.string.expense_error_addition_failed, resource.resId)
        }
    }

    // ── emitCashConflictResolution ────────────────────────────────────────

    @Nested
    inner class EmitCashConflictResolution {

        @Test
        fun `emits ShowCashConflictResolution with formatted amounts when cents and currency provided`() = runTest {
            every { formattingHelper.formatCentsValue(3000L, 2) } returns "30.00"
            every { formattingHelper.formatCentsWithCurrency(3000L, "EUR") } returns "€30.00"

            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitCashConflictResolution(
                availableCents = 3000L,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertEquals("30.00", action.availableAmountForInput)
            assertEquals("€30.00", action.availableAmountDisplay)
        }

        @Test
        fun `emits null amounts when availableCents is null`() = runTest {
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitCashConflictResolution(
                availableCents = null,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertNull(action.availableAmountForInput)
            assertNull(action.availableAmountDisplay)
        }

        @Test
        fun `emits null amounts when availableCents is negative`() = runTest {
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitCashConflictResolution(
                availableCents = -1L,
                actionsFlow = actionsFlow,
                currentState = uiState.value
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertNull(action.availableAmountForInput)
            assertNull(action.availableAmountDisplay)
        }

        @Test
        fun `emits null amounts when selectedCurrency is null`() = runTest {
            val state = uiState.value.copy(selectedCurrency = null)
            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitCashConflictResolution(
                availableCents = 3000L,
                actionsFlow = actionsFlow,
                currentState = state
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertNull(action.availableAmountForInput)
            assertNull(action.availableAmountDisplay)
        }

        @Test
        fun `formatCentsValue uses currency decimalDigits`() = runTest {
            val yenCurrency = CurrencyUiModel(code = "JPY", displayText = "JPY (¥)", decimalDigits = 0)
            val state = uiState.value.copy(selectedCurrency = yenCurrency)

            every { formattingHelper.formatCentsValue(3000L, 0) } returns "3000"
            every { formattingHelper.formatCentsWithCurrency(3000L, "JPY") } returns "¥3,000"

            val actions = mutableListOf<AddExpenseUiAction>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                actionsFlow.collect { actions.add(it) }
            }

            delegate.emitCashConflictResolution(
                availableCents = 3000L,
                actionsFlow = actionsFlow,
                currentState = state
            )

            assertEquals(1, actions.size)
            val action = actions[0] as AddExpenseUiAction.ShowCashConflictResolution
            assertEquals("3000", action.availableAmountForInput)
            assertEquals("¥3,000", action.availableAmountDisplay)
        }
    }
}
