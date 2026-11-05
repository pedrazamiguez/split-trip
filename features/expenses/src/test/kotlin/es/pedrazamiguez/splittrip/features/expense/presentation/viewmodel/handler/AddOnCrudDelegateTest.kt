package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseOptionsUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [AddOnCrudDelegate].
 */
class AddOnCrudDelegateTest {

    private lateinit var delegate: AddOnCrudDelegate
    private lateinit var optionsMapper: AddExpenseOptionsUiMapper
    private lateinit var exchangeRateDelegate: AddOnExchangeRateDelegate

    private val eurCurrency = CurrencyUiModel(code = "EUR", displayText = "EUR (€)", decimalDigits = 2)
    private val usdCurrency = CurrencyUiModel(code = "USD", displayText = "USD ($)", decimalDigits = 2)
    private val cashMethod = PaymentMethodUiModel(id = "CASH", displayText = "Cash")
    private val cardMethod = PaymentMethodUiModel(id = "CARD", displayText = "Card")

    @BeforeEach
    fun setUp() {
        optionsMapper = mockk(relaxed = true)
        exchangeRateDelegate = mockk(relaxed = true)

        every { exchangeRateDelegate.isCashMethod("CASH") } returns true
        every { exchangeRateDelegate.isCashMethod("CARD") } returns false
        every { exchangeRateDelegate.isCashMethod(null) } returns false

        delegate = AddOnCrudDelegate(
            addExpenseOptionsMapper = optionsMapper,
            exchangeRateDelegate = exchangeRateDelegate
        )
    }

    // ── buildNewAddOn ────────────────────────────────────────────────────

    @Nested
    inner class BuildNewAddOn {

        @Test
        fun `domestic currency hides exchange rate section`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cardMethod
            )

            val result = delegate.buildNewAddOn(AddOnType.FEE, state)

            assertFalse(result.showExchangeRateSection)
            assertEquals("1.0", result.displayExchangeRate)
            assertFalse(result.isExchangeRateLocked)
            assertNull(result.exchangeRateLockedHint)
        }

        @Test
        fun `foreign currency shows exchange rate section`() {
            every { optionsMapper.buildExchangeRateLabel(eurCurrency, usdCurrency) } returns "EUR → USD"
            every { optionsMapper.buildGroupAmountLabel(eurCurrency) } returns "EUR amount"

            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cardMethod,
                displayExchangeRate = "1.10"
            )

            val result = delegate.buildNewAddOn(AddOnType.FEE, state)

            assertTrue(result.showExchangeRateSection)
            assertEquals("1.10", result.displayExchangeRate)
            assertFalse(result.isExchangeRateLocked)
        }

        @Test
        fun `foreign plus CASH locks exchange rate`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cashMethod,
                displayExchangeRate = "1.10"
            )

            val result = delegate.buildNewAddOn(AddOnType.FEE, state)

            assertTrue(result.isExchangeRateLocked)
            assertNotNull(result.exchangeRateLockedHint)
        }

        @Test
        fun `sets correct type and mode defaults`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cardMethod
            )

            val result = delegate.buildNewAddOn(AddOnType.DISCOUNT, state)

            assertEquals(AddOnType.DISCOUNT, result.type)
            assertEquals(AddOnMode.ON_TOP, result.mode)
        }

        @Test
        fun `assigns a non-empty UUID id`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cardMethod
            )

            val result = delegate.buildNewAddOn(AddOnType.FEE, state)

            assertTrue(result.id.isNotEmpty())
        }
    }

    // ── resolveAddOnCurrencyContext ───────────────────────────────────────

    @Nested
    inner class ResolveAddOnCurrencyContext {

        @Test
        fun `domestic card returns all false`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cardMethod
            )

            val (isForeign, isCash, shouldLock) = delegate.resolveAddOnCurrencyContext(state)

            assertFalse(isForeign)
            assertFalse(isCash)
            assertFalse(shouldLock)
        }

        @Test
        fun `foreign card returns isForeign true only`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cardMethod
            )

            val (isForeign, isCash, shouldLock) = delegate.resolveAddOnCurrencyContext(state)

            assertTrue(isForeign)
            assertFalse(isCash)
            assertFalse(shouldLock)
        }

        @Test
        fun `foreign cash returns all true`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = usdCurrency,
                selectedPaymentMethod = cashMethod
            )

            val (isForeign, isCash, shouldLock) = delegate.resolveAddOnCurrencyContext(state)

            assertTrue(isForeign)
            assertTrue(isCash)
            assertTrue(shouldLock)
        }

        @Test
        fun `domestic cash returns isCash true only`() {
            val state = AddExpenseUiState(
                groupCurrency = eurCurrency,
                selectedCurrency = eurCurrency,
                selectedPaymentMethod = cashMethod
            )

            val (isForeign, isCash, shouldLock) = delegate.resolveAddOnCurrencyContext(state)

            assertFalse(isForeign)
            assertTrue(isCash)
            assertFalse(shouldLock)
        }
    }

    // ── applyPaymentMethodSwitch ─────────────────────────────────────────

    @Nested
    inner class ApplyPaymentMethodSwitch {

        private val baseAddOn = AddOnUiModel(
            id = "addon-1",
            displayExchangeRate = "1.10",
            preCashExchangeRate = null
        )

        @Test
        fun `cash plus foreign locks rate and saves pre-cash rate`() {
            val result = delegate.applyPaymentMethodSwitch(
                addOn = baseAddOn,
                isCash = true,
                isForeign = true,
                wasCashLocked = false
            )

            assertTrue(result.isExchangeRateLocked)
            assertEquals("1.10", result.preCashExchangeRate)
            assertFalse(result.isInsufficientCash)
            assertNotNull(result.exchangeRateLockedHint)
        }

        @Test
        fun `non-cash plus foreign plus wasCashLocked restores saved rate`() {
            val addOn = baseAddOn.copy(
                preCashExchangeRate = "1.05",
                displayExchangeRate = "1.20"
            )

            val result = delegate.applyPaymentMethodSwitch(
                addOn = addOn,
                isCash = false,
                isForeign = true,
                wasCashLocked = true
            )

            assertFalse(result.isExchangeRateLocked)
            assertEquals("1.05", result.displayExchangeRate)
            assertNull(result.preCashExchangeRate)
            assertNull(result.exchangeRateLockedHint)
        }

        @Test
        fun `non-cash plus wasCashLocked clears lock (domestic)`() {
            val addOn = baseAddOn.copy(isExchangeRateLocked = true)

            val result = delegate.applyPaymentMethodSwitch(
                addOn = addOn,
                isCash = false,
                isForeign = false,
                wasCashLocked = true
            )

            assertFalse(result.isExchangeRateLocked)
            assertNull(result.exchangeRateLockedHint)
        }

        @Test
        fun `no-op when not cash and was not locked`() {
            val result = delegate.applyPaymentMethodSwitch(
                addOn = baseAddOn,
                isCash = false,
                isForeign = false,
                wasCashLocked = false
            )

            assertEquals(baseAddOn, result)
        }
    }

    // ── hasSavedPreCashRate ──────────────────────────────────────────────

    @Nested
    inner class HasSavedPreCashRate {

        @Test
        fun `returns true when preCashExchangeRate is set`() {
            val addOn = AddOnUiModel(id = "a1", preCashExchangeRate = "1.05")
            assertTrue(delegate.hasSavedPreCashRate(addOn))
        }

        @Test
        fun `returns false when preCashExchangeRate is null`() {
            val addOn = AddOnUiModel(id = "a1", preCashExchangeRate = null)
            assertFalse(delegate.hasSavedPreCashRate(addOn))
        }
    }

    // ── label builders ───────────────────────────────────────────────────

    @Nested
    inner class LabelBuilders {

        @Test
        fun `buildExchangeRateLabel returns empty for domestic`() {
            val result = delegate.buildExchangeRateLabel(false, eurCurrency, eurCurrency)
            assertEquals("", result)
        }

        @Test
        fun `buildExchangeRateLabel delegates for foreign`() {
            every { optionsMapper.buildExchangeRateLabel(eurCurrency, usdCurrency) } returns "EUR → USD"

            val result = delegate.buildExchangeRateLabel(true, eurCurrency, usdCurrency)

            assertEquals("EUR → USD", result)
        }

        @Test
        fun `buildGroupAmountLabel returns empty for domestic`() {
            val result = delegate.buildGroupAmountLabel(false, eurCurrency)
            assertEquals("", result)
        }

        @Test
        fun `buildGroupAmountLabel delegates for foreign`() {
            every { optionsMapper.buildGroupAmountLabel(eurCurrency) } returns "EUR amount"

            val result = delegate.buildGroupAmountLabel(true, eurCurrency)

            assertEquals("EUR amount", result)
        }
    }

    // ── Expansion helpers ────────────────────────────────────────────────

    @Nested
    inner class ExpansionHelpers {

        @Test
        fun `buildNewAddOn sets isExpanded to true`() {
            val state = AddExpenseUiState(groupCurrency = eurCurrency, selectedCurrency = eurCurrency)
            val result = delegate.buildNewAddOn(AddOnType.FEE, state)
            assertTrue(result.isExpanded)
        }

        @Test
        fun `collapseAllExcept expands specified item and collapses all others`() {
            val list = listOf(
                AddOnUiModel(id = "1", isExpanded = true),
                AddOnUiModel(id = "2", isExpanded = false),
                AddOnUiModel(id = "3", isExpanded = true)
            )

            val result = delegate.collapseAllExcept(list, "2")

            assertFalse(result[0].isExpanded)
            assertTrue(result[1].isExpanded)
            assertFalse(result[2].isExpanded)
        }

        @Test
        fun `toggleItemExpansion toggles target item and collapses others`() {
            val list = listOf(
                AddOnUiModel(id = "1", isExpanded = true),
                AddOnUiModel(id = "2", isExpanded = false)
            )

            val result = delegate.toggleItemExpansion(list, "2")

            assertFalse(result[0].isExpanded)
            assertTrue(result[1].isExpanded)

            val collapsedAgain = delegate.toggleItemExpansion(result, "2")
            assertFalse(collapsedAgain[0].isExpanded)
            assertFalse(collapsedAgain[1].isExpanded)
        }
    }
}
