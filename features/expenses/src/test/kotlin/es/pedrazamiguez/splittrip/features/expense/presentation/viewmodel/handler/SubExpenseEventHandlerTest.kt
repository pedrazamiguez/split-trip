package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.common.presentation.UiText
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.impl.ExchangeRateCalculationServiceImpl
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubExpenseEventHandler")
class SubExpenseEventHandlerTest {

    private lateinit var localeProvider: LocaleProvider
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var exchangeRateCalculationService: ExchangeRateCalculationService
    private lateinit var handler: SubExpenseEventHandler

    private val eurCurrency = CurrencyUiModel(
        code = "EUR",
        displayText = "EUR (€)",
        decimalDigits = 2
    )

    private val baseState = AddExpenseUiState(
        sourceAmount = "100.00",
        selectedCurrency = eurCurrency,
        groupCurrency = eurCurrency,
        displayExchangeRate = "1.0",
        selectedPaymentMethod = PaymentMethodUiModel("CARD", "Card"),
        currentUserId = "user-1"
    )

    @BeforeEach
    fun setUp() {
        localeProvider = mockk {
            every { getCurrentLocale() } returns Locale.US
        }
        formattingHelper = FormattingHelper(localeProvider)
        exchangeRateCalculationService = ExchangeRateCalculationServiceImpl()
        handler = SubExpenseEventHandler(
            formattingHelper = formattingHelper,
            exchangeRateCalculationService = exchangeRateCalculationService
        )
    }

    @Nested
    @DisplayName("SubExpensesToggled")
    inner class SubExpensesToggled {

        @Test
        fun `toggling on when empty creates 2 initial tranches with 50-50 split`() {
            val updated = handler.handleSubExpensesToggled(baseState)

            assertTrue(updated.isSubExpensesEnabled)
            assertEquals(2, updated.subExpenses.size)
            assertEquals("50.00", updated.subExpenses[0].amountInput)
            assertEquals("50.00", updated.subExpenses[1].amountInput)
            assertNull(updated.subExpensesError)
        }

        @Test
        fun `toggling off clears subExpenses error and formatted balances`() {
            val enabledState = handler.handleSubExpensesToggled(baseState)
            val toggledOff = handler.handleSubExpensesToggled(enabledState)

            assertFalse(toggledOff.isSubExpensesEnabled)
            assertNull(toggledOff.subExpensesError)
            assertEquals("", toggledOff.subExpensesAllocatedFormatted)
            assertEquals("", toggledOff.subExpensesRemainingFormatted)
        }
    }

    @Nested
    @DisplayName("Tranche CRUD")
    inner class TrancheCrud {

        @Test
        fun `handleSubExpenseAdded adds a new tranche`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val updated = handler.handleSubExpenseAdded(state)

            assertEquals(3, updated.subExpenses.size)
        }

        @Test
        fun `handleSubExpenseRemoved removes tranche by id and recalculates`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val trancheToRemove = state.subExpenses[0]
            val updated = handler.handleSubExpenseRemoved(state, trancheToRemove.id)

            assertEquals(1, updated.subExpenses.size)
            assertNotNull(updated.subExpensesError)
            val error = updated.subExpensesError as UiText.StringResource
            assertEquals(R.string.expense_error_sub_expenses_minimum, error.resId)
        }

        @Test
        fun `handleSubExpenseTitleChanged updates title`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val id = state.subExpenses[0].id
            val updated = handler.handleSubExpenseTitleChanged(state, id, "Deposit")

            assertEquals("Deposit", updated.subExpenses.find { it.id == id }?.title)
        }

        @Test
        fun `handleSubExpenseAmountChanged updates amount and recalculates`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val id = state.subExpenses[0].id
            val updated = handler.handleSubExpenseAmountChanged(state, id, "30.00")

            assertEquals("30.00", updated.subExpenses.find { it.id == id }?.amountInput)
            assertNotNull(updated.subExpensesError)
            val error = updated.subExpensesError as UiText.StringResource
            assertEquals(R.string.expense_error_sub_expenses_sum, error.resId)
        }

        @Test
        fun `handleSubExpensePaymentStatusSelected updates payment status`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val id = state.subExpenses[0].id
            val updated = handler.handleSubExpensePaymentStatusSelected(state, id, PaymentStatus.SCHEDULED.name)

            assertEquals(PaymentStatus.SCHEDULED, updated.subExpenses.find { it.id == id }?.paymentStatus)
        }

        @Test
        fun `handleSubExpenseNotesChanged updates notes`() {
            val state = handler.handleSubExpensesToggled(baseState)
            val id = state.subExpenses[0].id
            val updated = handler.handleSubExpenseNotesChanged(state, id, "Transfer receipt")

            assertEquals("Transfer receipt", updated.subExpenses.find { it.id == id }?.notes)
        }
    }
}
