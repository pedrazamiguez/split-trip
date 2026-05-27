package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseValidationService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.ExpenseSplitCalculatorFactory
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentStatusUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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

/**
 * Unit tests for [SubmitEventHandler.adjustForIncludedAddOns].
 *
 * These tests were originally in AddExpenseUiMapperTest but the INCLUDED
 * base-cost extraction was moved here (to the handler) as part of the
 * architecture correction that removes [ExpenseCalculatorService] from the
 * mapper (which is a presentation concern and must not hold domain services).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SubmitEventHandler")
class SubmitEventHandlerTest {

    private lateinit var handler: SubmitEventHandler
    private lateinit var strategy:
        es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy.ExpenseFlowStrategy
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper

    /** A minimal [Expense] stub — only the fields used by adjustForIncludedAddOns matter. */
    private fun makeExpense(
        sourceAmount: Long,
        groupAmount: Long,
        addOns: List<AddOn> = emptyList(),
        splits: List<ExpenseSplit> = emptyList()
    ) = Expense(
        groupId = "group-1",
        title = "Test",
        sourceAmount = sourceAmount,
        sourceCurrency = "EUR",
        groupAmount = groupAmount,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        addOns = addOns,
        splits = splits,
        splitType = SplitType.EQUAL,
        paymentMethod = PaymentMethod.CASH,
        createdAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        strategy = mockk(relaxed = true)
        addExpenseUiMapper = mockk(relaxed = true)
        val splitCalculatorFactory = ExpenseSplitCalculatorFactory(ExpenseCalculatorService())
        val saveLastUsedPreferences = SaveLastUsedPreferencesBundle(
            setGroupLastUsedCurrencyUseCase = mockk(relaxed = true),
            setGroupLastUsedPaymentMethodUseCase = mockk(relaxed = true),
            setGroupLastUsedCategoryUseCase = mockk(relaxed = true)
        )
        val formattingHelper = mockk<FormattingHelper>(relaxed = true)

        handler = SubmitEventHandler(
            expenseValidationService = ExpenseValidationService(splitCalculatorFactory),
            addOnCalculationService = AddOnCalculationService(),
            expenseCalculatorService = ExpenseCalculatorService(),
            remainderDistributionService = RemainderDistributionService(),
            addExpenseUiMapper = addExpenseUiMapper,
            submitResultDelegate = SubmitResultDelegate(
                saveLastUsedPreferences = saveLastUsedPreferences,
                formattingHelper = formattingHelper
            )
        ).apply {
            setStrategy(strategy)
        }
    }

    // ── No INCLUDED add-ons ──────────────────────────────────────────────────

    @Nested
    inner class NoIncludedAddOns {

        @Test
        fun `ON_TOP only - expense is returned unchanged`() {
            val addOn = AddOn(
                id = "fee-1",
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 500,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 500
            )
            val expense = makeExpense(sourceAmount = 5000L, groupAmount = 5000L, addOns = listOf(addOn))

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertEquals(5000L, result.sourceAmount)
            assertEquals(5000L, result.groupAmount)
            assertEquals(500L, result.addOns[0].groupAmountCents)
        }

        @Test
        fun `no add-ons - expense is returned unchanged`() {
            val expense = makeExpense(sourceAmount = 8000L, groupAmount = 8000L)

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertEquals(8000L, result.sourceAmount)
            assertEquals(8000L, result.groupAmount)
        }
    }

    // ── EXACT INCLUDED ──────────────────────────────────────────────────────

    @Nested
    inner class ExactIncluded {

        @Test
        fun `EXACT INCLUDED tip subtracts from sourceAmount and groupAmount`() {
            // 68.31 EUR total with 6.21 EUR EXACT INCLUDED tip
            // Expected base: 68.31 - 6.21 = 62.10 EUR (6210 cents)
            val domainAddOn = AddOn(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 621,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 621
            )
            val expense = makeExpense(
                sourceAmount = 6831L,
                groupAmount = 6831L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertEquals(6210L, result.sourceAmount)
            assertEquals(6210L, result.groupAmount)
            // EXACT add-on amount stays the same
            assertEquals(621L, result.addOns[0].groupAmountCents)
        }

        @Test
        fun `INCLUDED DISCOUNT is informational and does not adjust amounts`() {
            // INCLUDED discounts don't trigger base cost extraction — the user
            // already entered the discounted price (50 EUR).
            val domainAddOn = AddOn(
                id = "disc-1",
                type = AddOnType.DISCOUNT,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 500,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 500
            )
            val expense = makeExpense(
                sourceAmount = 5000L,
                groupAmount = 5000L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertEquals(5000L, result.sourceAmount)
            assertEquals(5000L, result.groupAmount)
        }
    }

    // ── ON_TOP DISCOUNT ──────────────────────────────────────────────────────

    @Nested
    inner class OnTopDiscount {

        @Test
        fun `ON_TOP EXACT DISCOUNT reduces groupAmount and sourceAmount`() {
            // Scenario D: 90 EUR with 8 EUR ON_TOP discount → logged as 82 EUR
            val domainAddOn = AddOn(
                id = "disc-1",
                type = AddOnType.DISCOUNT,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 800,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 800
            )
            val expense = makeExpense(
                sourceAmount = 9000L,
                groupAmount = 9000L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForOnTopDiscounts(expense)

            assertEquals(8200L, result.sourceAmount)
            assertEquals(8200L, result.groupAmount)
            // groupAmountCents zeroed to prevent double-subtraction in effective calc
            assertEquals(0L, result.addOns[0].groupAmountCents)
        }

        @Test
        fun `ON_TOP PERCENTAGE DISCOUNT reduces groupAmount and zeroes add-on`() {
            // Scenario C: 90 EUR with 10% ON_TOP discount (900 cents) → logged as 81 EUR
            val domainAddOn = AddOn(
                id = "disc-1",
                type = AddOnType.DISCOUNT,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.PERCENTAGE,
                amountCents = 900,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 900
            )
            val expense = makeExpense(
                sourceAmount = 9000L,
                groupAmount = 9000L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForOnTopDiscounts(expense)

            assertEquals(8100L, result.sourceAmount)
            assertEquals(8100L, result.groupAmount)
            assertEquals(0L, result.addOns[0].groupAmountCents)
        }

        @Test
        fun `ON_TOP discount rescales splits`() {
            // 90 EUR with 9 EUR ON_TOP discount, 2 equal splits
            val domainAddOn = AddOn(
                id = "disc-1",
                type = AddOnType.DISCOUNT,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 900,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 900
            )
            val splits = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 4500),
                ExpenseSplit(userId = "user-2", amountCents = 4500)
            )
            val expense = makeExpense(
                sourceAmount = 9000L,
                groupAmount = 9000L,
                addOns = listOf(domainAddOn),
                splits = splits
            )

            val result = handler.adjustForOnTopDiscounts(expense)

            assertEquals(8100L, result.groupAmount)
            assertEquals(8100L, result.splits.sumOf { it.amountCents })
            assertEquals(4050L, result.splits[0].amountCents)
            assertEquals(4050L, result.splits[1].amountCents)
        }

        @Test
        fun `no-op when no ON_TOP discounts exist`() {
            val domainAddOn = AddOn(
                id = "fee-1",
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 500,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 500
            )
            val expense = makeExpense(
                sourceAmount = 9000L,
                groupAmount = 9000L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForOnTopDiscounts(expense)

            assertEquals(9000L, result.sourceAmount)
            assertEquals(9000L, result.groupAmount)
            assertEquals(500L, result.addOns[0].groupAmountCents)
        }
    }

    // ── PERCENTAGE INCLUDED ─────────────────────────────────────────────────

    @Nested
    inner class PercentageIncluded {

        @Test
        fun `PERCENTAGE INCLUDED tip extracts base and recomputes add-on amount`() {
            // 68.31 EUR total with 10% INCLUDED tip
            // Base: 6831 / 1.10 = 6210 cents (floor)
            // Recomputed tip: 6210 * 10 / 100 = 621 cents
            val domainAddOn = AddOn(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.PERCENTAGE,
                amountCents = 683,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 683 // pre-adjustment; handler will recompute
            )
            val uiAddOn = AddOnUiModel(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.PERCENTAGE,
                amountInput = "10",
                resolvedAmountCents = 683,
                groupAmountCents = 683
            )
            val expense = makeExpense(
                sourceAmount = 6831L,
                groupAmount = 6831L,
                addOns = listOf(domainAddOn)
            )

            val result = handler.adjustForIncludedAddOns(expense, listOf(uiAddOn).toImmutableList())

            assertEquals(6210L, result.sourceAmount)
            assertEquals(6210L, result.groupAmount)
            assertEquals(621L, result.addOns[0].groupAmountCents)
        }
    }

    // ── Split rescaling ──────────────────────────────────────────────────────

    @Nested
    inner class SplitRescaling {

        @Test
        fun `splits are rescaled proportionally for EXACT INCLUDED add-on`() {
            // 100.00 EUR total with 10 EUR EXACT INCLUDED tip, 2 equal splits (5000 each)
            // Base: 9000 cents → splits should be 4500 each
            val domainAddOn = AddOn(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 1000,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 1000
            )
            val splits = listOf(
                ExpenseSplit(userId = "user-1", amountCents = 5000),
                ExpenseSplit(userId = "user-2", amountCents = 5000)
            )
            val expense = makeExpense(
                sourceAmount = 10000L,
                groupAmount = 10000L,
                addOns = listOf(domainAddOn),
                splits = splits
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertEquals(9000L, result.sourceAmount)
            assertEquals(9000L, result.groupAmount)
            // Sum of splits equals the new base
            assertEquals(9000L, result.splits.sumOf { it.amountCents })
            assertEquals(4500L, result.splits[0].amountCents)
            assertEquals(4500L, result.splits[1].amountCents)
        }

        @Test
        fun `split remainder is distributed to preserve total`() {
            // 10 EUR total with 1 EUR EXACT INCLUDED tip → base 9 EUR, 3 splits
            // 9000 / 3 = 3000 each exactly — no remainder
            val domainAddOn = AddOn(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 1000,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 1000
            )
            val splits = listOf(
                ExpenseSplit(userId = "a", amountCents = 334),
                ExpenseSplit(userId = "b", amountCents = 333),
                ExpenseSplit(userId = "c", amountCents = 333)
            )
            val expense = makeExpense(
                sourceAmount = 1000L,
                groupAmount = 1000L,
                addOns = listOf(domainAddOn),
                splits = splits
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            // Sum of adjusted splits must equal the new base exactly
            assertEquals(result.groupAmount, result.splits.sumOf { it.amountCents })
        }
    }

    // ── rescaleSplits early-return branches ─────────────────────────────

    @Nested
    inner class RescaleSplitsEarlyReturns {

        @Test
        fun `no-op when only INCLUDED discount exists`() {
            // INCLUDED discounts are informational, so adjustForIncludedAddOns
            // returns the expense unchanged — splits stay as-is.
            val domainAddOn = AddOn(
                id = "disc-1",
                type = AddOnType.DISCOUNT,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 0,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 0
            )
            val splits = listOf(
                ExpenseSplit(userId = "a", amountCents = 5000),
                ExpenseSplit(userId = "b", amountCents = 5000)
            )
            val expense = makeExpense(
                sourceAmount = 10000L,
                groupAmount = 10000L,
                addOns = listOf(domainAddOn),
                splits = splits
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            // INCLUDED discount is informational → no adjustment → splits unchanged
            assertEquals(5000L, result.splits[0].amountCents)
            assertEquals(5000L, result.splits[1].amountCents)
        }

        @Test
        fun `no-op when splits list is empty`() {
            // EXACT INCLUDED tip with no splits → rescaleSplits early-return (empty list)
            val domainAddOn = AddOn(
                id = "tip-1",
                type = AddOnType.TIP,
                mode = AddOnMode.INCLUDED,
                valueType = AddOnValueType.EXACT,
                amountCents = 500,
                currency = "EUR",
                exchangeRate = BigDecimal.ONE,
                groupAmountCents = 500
            )
            val expense = makeExpense(
                sourceAmount = 5000L,
                groupAmount = 5000L,
                addOns = listOf(domainAddOn),
                splits = emptyList() // empty splits
            )

            val result = handler.adjustForIncludedAddOns(expense, persistentListOf())

            assertTrue(result.splits.isEmpty())
            // But the amounts should still be adjusted
            assertEquals(4500L, result.sourceAmount)
        }
    }

    // ── submitExpense() validation & submission pipeline ────────────────

    @Nested
    @DisplayName("submitExpense")
    inner class SubmitExpense {

        private val testDispatcher = StandardTestDispatcher()
        private lateinit var stateFlow: MutableStateFlow<AddExpenseUiState>
        private lateinit var actionsFlow: MutableSharedFlow<AddExpenseUiAction>

        @BeforeEach
        fun bindHandler() {
            stateFlow = MutableStateFlow(AddExpenseUiState())
            actionsFlow = MutableSharedFlow()
        }

        @Test
        fun `null groupId is a no-op`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)

            handler.submitExpense(null) {}
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLoading)
            assertNull(stateFlow.value.error)
        }

        @Test
        fun `empty title sets isTitleValid false and error`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(expenseTitle = "", sourceAmount = "50")

            handler.submitExpense("group-1") {}

            assertFalse(stateFlow.value.isTitleValid)
            assertNotNull(stateFlow.value.error)
        }

        @Test
        fun `invalid amount sets isAmountValid false and error`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(expenseTitle = "Dinner", sourceAmount = "")

            handler.submitExpense("group-1") {}

            assertFalse(stateFlow.value.isAmountValid)
            assertNotNull(stateFlow.value.error)
        }

        @Test
        fun `SCHEDULED with no dueDate sets isDueDateValid false`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50",
                selectedPaymentStatus = PaymentStatusUiModel(
                    id = PaymentStatus.SCHEDULED.name,
                    displayText = "Scheduled"
                ),
                dueDateMillis = null
            )

            handler.submitExpense("group-1") {}

            assertFalse(stateFlow.value.isDueDateValid)
            assertNotNull(stateFlow.value.error)
        }

        @Test
        fun `future expense date sets isExpenseDateValid false and error`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50",
                expenseDateMillis = System.currentTimeMillis() + 1000000L
            )

            handler.submitExpense("group-1") {}

            assertFalse(stateFlow.value.isExpenseDateValid)
            assertNotNull(stateFlow.value.error)
        }

        @Test
        fun `add-on with zero resolved amount sets addOnError`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50",
                addOns = persistentListOf(
                    AddOnUiModel(
                        id = "addon-1",
                        type = AddOnType.TIP,
                        mode = AddOnMode.ON_TOP,
                        valueType = AddOnValueType.EXACT,
                        amountInput = "5",
                        resolvedAmountCents = 0,
                        groupAmountCents = 0
                    )
                )
            )

            handler.submitExpense("group-1") {}

            assertNotNull(stateFlow.value.addOnError)
        }

        @Test
        fun `mapper failure sets error and clears loading`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50"
            )
            every {
                addExpenseUiMapper.mapToDomain(any(), any())
            } returns Result.failure(RuntimeException("Mapping error"))

            handler.submitExpense("group-1") {}
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLoading)
            assertNotNull(stateFlow.value.error)
        }

        @Test
        fun `happy path calls use case and invokes onSuccess`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50"
            )
            val expense = makeExpense(sourceAmount = 5000L, groupAmount = 5000L)
            every { addExpenseUiMapper.mapToDomain(any(), any()) } returns Result.success(expense)
            coEvery { strategy.saveExpense(any(), any(), any()) } returns Result.success(Unit)

            var successCalled = false
            handler.submitExpense("group-1") { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertFalse(stateFlow.value.isLoading)
        }

        @Test
        fun `submit with null groupId parameter uses loadedGroupId from state`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50",
                loadedGroupId = "group-loaded"
            )
            val expense = makeExpense(sourceAmount = 5000L, groupAmount = 5000L)
            every { addExpenseUiMapper.mapToDomain(any(), "group-loaded") } returns Result.success(expense)
            coEvery { strategy.saveExpense("group-loaded", any(), any()) } returns Result.success(Unit)

            var successCalled = false
            handler.submitExpense(null) { successCalled = true }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertFalse(stateFlow.value.isLoading)
        }

        @Test
        fun `use case failure clears loading without inline error`() = runTest(testDispatcher) {
            handler.bind(stateFlow, actionsFlow, this)
            stateFlow.value = AddExpenseUiState(
                expenseTitle = "Dinner",
                sourceAmount = "50"
            )
            val expense = makeExpense(sourceAmount = 5000L, groupAmount = 5000L)
            every { addExpenseUiMapper.mapToDomain(any(), any()) } returns Result.success(expense)
            coEvery { strategy.saveExpense(any(), any(), any()) } returns Result.failure(
                RuntimeException("Network error")
            )

            handler.submitExpense("group-1") {}
            advanceUntilIdle()

            assertFalse(stateFlow.value.isLoading)
            // Error is shown via UiAction (Snackbar), not inline
            assertNull(stateFlow.value.error)
        }
    }
}
