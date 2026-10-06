package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.logging.TelemetryTracker
import es.pedrazamiguez.splittrip.domain.enums.GroupStatus
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.ExpenseSearchService
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseFilterServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseSearchServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDateGroupUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesUiEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDateTime
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var expenseUiMapper: ExpenseUiMapper
    private lateinit var getGroupByIdUseCase: GetGroupByIdUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase
    private lateinit var getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase
    private lateinit var getExpenseByIdFlowUseCase: GetExpenseByIdFlowUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var observeGroupUseCase: ObserveGroupUseCase
    private lateinit var telemetryTracker: TelemetryTracker
    private var expenseSearchService: ExpenseSearchService = ExpenseSearchServiceImpl()
    private var expenseFilterService: ExpenseFilterService =
        ExpenseFilterServiceImpl(expenseSearchService = expenseSearchService)
    private lateinit var viewModel: ExpensesViewModel

    private val testGroupId = "group-123"
    private val testExpense1 = Expense(
        id = "expense-1",
        groupId = testGroupId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        createdBy = "user-1",
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 30)
    )

    private val testExpense2 = Expense(
        id = "expense-2",
        groupId = testGroupId,
        title = "Taxi",
        sourceAmount = 2000L,
        sourceCurrency = "EUR",
        groupAmount = 2000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CASH,
        createdBy = "user-2",
        createdAt = LocalDateTime.of(2024, 1, 16, 10, 0)
    )

    private val testExpense3 = Expense(
        id = "expense-3",
        groupId = testGroupId,
        title = "Groceries",
        notes = "Supermarket snacks and drinks",
        sourceAmount = 1500L,
        sourceCurrency = "EUR",
        groupAmount = 1500L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        createdBy = "user-1",
        createdAt = LocalDateTime.of(2024, 1, 17, 14, 0)
    )

    /** Helper to flatten all expenses from grouped state for easy assertion. */
    private fun allExpenses() = viewModel.uiState.value.expenseGroups.flatMap { it.expenses }

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupExpensesFlowUseCase = mockk()
        deleteExpenseUseCase = mockk()
        expenseUiMapper = mockk()
        getGroupByIdUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        getGroupContributionsFlowUseCase = mockk()
        getGroupSubunitsFlowUseCase = mockk()
        getExpenseByIdFlowUseCase = mockk()
        updateExpenseUseCase = mockk()
        authenticationService = mockk()
        observeGroupUseCase = mockk()
        telemetryTracker = mockk(relaxed = true)

        stubDefaultUseCases()
        stubExpenseUiMapper()
    }

    private fun stubDefaultUseCases() {
        coEvery { getGroupByIdUseCase(any()) } returns Group(
            id = testGroupId,
            name = "Test Group",
            currency = "EUR"
        )
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase(any()) } returns flowOf(emptyList())
        every { observeGroupUseCase(any()) } returns flowOf(
            Group(
                id = testGroupId,
                name = "Test Group",
                currency = "EUR",
                status = GroupStatus.ACTIVE
            )
        )
        every { authenticationService.currentUserId() } returns "current-user-id"
    }

    private fun stubExpenseUiMapper() {
        every { expenseUiMapper.mapGroupedByDate(any(), any(), any(), any(), any()) } answers {
            val expenses = firstArg<List<Expense>>()
            expenses.groupBy { it.createdAt?.toLocalDate() }
                .map { (date, dayExpenses) ->
                    ExpenseDateGroupUiModel(
                        dateText = date?.toString() ?: "",
                        formattedDayTotal = "${dayExpenses.sumOf {
                            it.groupAmount
                        }} ${dayExpenses.first().groupCurrency}",
                        expenses = dayExpenses.map { expense ->
                            ExpenseUiModel(
                                id = expense.id,
                                title = expense.title,
                                formattedAmount = "${expense.groupAmount} ${expense.groupCurrency}",
                                paidByText = "Paid by ${expense.createdBy}",
                                dateText = expense.createdAt?.toString() ?: ""
                            )
                        }.toImmutableList()
                    )
                }.toImmutableList()
        }

        every { expenseUiMapper.formatTotalSpent(any(), any()) } answers {
            val amount = firstArg<Long>()
            val currency = secondArg<String>()
            "$amount $currency"
        }

        every { expenseUiMapper.formatScheduledAmount(any(), any()) } answers {
            val amount = firstArg<Long>()
            val currency = secondArg<String>()
            "$amount $currency"
        }
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class StateManagement {

        @Test
        fun `initial state is loading`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(any()) } returns flowOf(emptyList())

            // When
            viewModel = createViewModel()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertTrue(state.isEmpty)
            assertNull(state.groupId)
        }

        @Test
        fun `setSelectedGroup updates state with expenses`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()

            // Start collecting to activate the WhileSubscribed flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, allExpenses().size)
            assertEquals(testGroupId, state.groupId)
            assertEquals("Dinner", allExpenses().find { it.id == "expense-1" }?.title)
            assertEquals("Taxi", allExpenses().find { it.id == "expense-2" }?.title)

            collectJob.cancel()
        }

        @Test
        fun `changing group triggers new data load`() = runTest(testDispatcher) {
            // Given
            val group1Id = "group-1"
            val group2Id = "group-2"
            every { getGroupExpensesFlowUseCase(group1Id) } returns flowOf(listOf(testExpense1))
            every { getGroupExpensesFlowUseCase(group2Id) } returns flowOf(listOf(testExpense2))

            viewModel = createViewModel()

            // Start collecting to activate the WhileSubscribed flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Load first group
            viewModel.setSelectedGroup(group1Id)
            advanceUntilIdle()

            // Then - Verify first group loaded
            assertEquals(group1Id, viewModel.uiState.value.groupId)
            assertEquals(1, allExpenses().size)
            assertEquals("Dinner", allExpenses()[0].title)

            // When - Switch to second group
            viewModel.setSelectedGroup(group2Id)
            advanceUntilIdle()

            // Then - Verify second group loaded
            assertEquals(group2Id, viewModel.uiState.value.groupId)
            assertEquals(1, allExpenses().size)
            assertEquals("Taxi", allExpenses()[0].title)

            collectJob.cancel()
        }

        @Test
        fun `rapid setSelectedGroup and LoadExpenses does not cancel fetch`() = runTest(testDispatcher) {
            // Given - Simulates the race condition: select group + immediate LoadExpenses
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Set group AND trigger LoadExpenses back-to-back (race condition scenario)
            viewModel.setSelectedGroup(testGroupId)
            viewModel.onEvent(ExpensesUiEvent.LoadExpenses)
            advanceUntilIdle()

            // Then - Expenses should still be loaded, not dropped
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, allExpenses().size)
            assertEquals(testGroupId, state.groupId)

            collectJob.cancel()
        }

        @Test
        fun `setSelectedGroup with same groupId does not reload`() = runTest(testDispatcher) {
            // Given
            var callCount = 0
            every { getGroupExpensesFlowUseCase(testGroupId) } answers {
                callCount++
                flowOf(listOf(testExpense1))
            }
            viewModel = createViewModel()

            // When - Set same group twice
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            val initialCallCount = callCount

            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then - Should not trigger additional calls
            assertEquals(initialCallCount, callCount)
        }
    }

    @Nested
    inner class GracePeriodLogic {

        @Test
        fun `empty list shows loading state during grace period`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceTimeBy(50) // Advance less than grace period (400ms)

            // Then - Should still be in loading state during grace period
            assertTrue(viewModel.uiState.value.isLoading)
            collectJob.cancel()
        }

        @Test
        fun `empty list shows empty state after grace period`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceTimeBy(450) // Advance past grace period (400ms)

            // Then - Should show empty state
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.isEmpty)
            assertEquals(testGroupId, viewModel.uiState.value.groupId)
            collectJob.cancel()
        }

        @Test
        fun `non-empty list bypasses grace period`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceTimeBy(50) // Advance minimal time

            // Then - Should immediately show data without grace period delay
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(1, allExpenses().size)
            collectJob.cancel()
        }

        @Test
        fun `grace period prevents flicker when switching from loading to empty`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Set selected group
            viewModel.setSelectedGroup(testGroupId)

            // Then - Initial loading state
            advanceTimeBy(10)
            var state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertEquals(testGroupId, state.groupId)

            // Then - Still loading during grace period (no empty state flicker)
            advanceTimeBy(200)
            state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertEquals(testGroupId, state.groupId)

            // Then - Finally shows empty state after grace period
            advanceTimeBy(400)
            state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertEquals(testGroupId, state.groupId)
            collectJob.cancel()
        }
    }

    @Nested
    inner class ErrorHandling {

        @Test
        fun `error in flow emits ShowLoadError action`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flow {
                throw IOException("Network error")
            }
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // Collect actions in background
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isEmpty)
            assertTrue(
                actions.any { it is ExpensesUiAction.ShowLoadError },
                "Expected ShowLoadError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }
    }

    @Nested
    inner class RefreshLogic {

        @Test
        fun `LoadExpenses event triggers refresh`() = runTest(testDispatcher) {
            // Given
            var emissionCount = 0
            every { getGroupExpensesFlowUseCase(testGroupId) } answers {
                emissionCount++
                flowOf(listOf(testExpense1))
            }
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()
            val initialEmissions = emissionCount

            // When - Trigger refresh
            viewModel.onEvent(ExpensesUiEvent.LoadExpenses)
            advanceUntilIdle()

            // Then - Should have triggered new emissions
            assertTrue(emissionCount > initialEmissions, "Expected more emissions after refresh")
            collectJob.cancel()
        }

        @Test
        fun `refresh does not change selected group`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(listOf(testExpense1))
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.LoadExpenses)
            advanceUntilIdle()

            // Then
            assertEquals(testGroupId, viewModel.uiState.value.groupId)
            collectJob.cancel()
        }
    }

    @Nested
    inner class ScrollPositionTracking {

        @Test
        fun `ScrollPositionChanged updates scroll state`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(any()) } returns flowOf(emptyList())
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // Need to set a group first to activate the combined flow
            viewModel.setSelectedGroup(testGroupId)
            advanceTimeBy(450) // Wait for grace period

            // When
            viewModel.onEvent(
                ExpensesUiEvent.ScrollPositionChanged(
                    index = 5,
                    offset = 100
                )
            )
            advanceUntilIdle()

            // Then
            assertEquals(5, viewModel.uiState.value.scrollPosition)
            assertEquals(100, viewModel.uiState.value.scrollOffset)
            collectJob.cancel()
        }

        @Test
        fun `scroll position persists across group changes`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(any()) } returns flowOf(listOf(testExpense1))
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When - Set scroll position and change group
            viewModel.onEvent(
                ExpensesUiEvent.ScrollPositionChanged(
                    index = 3,
                    offset = 50
                )
            )
            viewModel.setSelectedGroup("group-1")
            viewModel.setSelectedGroup("group-2")
            advanceUntilIdle()

            // Then - Scroll position should persist
            assertEquals(3, viewModel.uiState.value.scrollPosition)
            assertEquals(50, viewModel.uiState.value.scrollOffset)
            collectJob.cancel()
        }
    }

    @Nested
    inner class DeleteExpenseEvent {

        @Test
        fun `DeleteExpense event calls use case with correct params`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1)
            )
            coEvery { deleteExpenseUseCase(any(), any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, "expense-1") }
            coVerify(exactly = 1) {
                telemetryTracker.trackEvent(
                    "expense_deleted",
                    mapOf("expense_id" to "expense-1", "group_id" to testGroupId)
                )
            }

            collectJob.cancel()
        }

        @Test
        fun `DeleteExpense event emits success action when deletion succeeds`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1)
            )
            coEvery { deleteExpenseUseCase(any(), any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Collect actions in background
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, "expense-1") }
            assertTrue(
                actions.any { it is ExpensesUiAction.ShowDeleteSuccess },
                "Expected ShowDeleteSuccess action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteExpense event emits error action when deletion fails`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1)
            )
            val exception = RuntimeException("Database error")
            coEvery { deleteExpenseUseCase(any(), any()) } throws exception
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Collect actions in background
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, "expense-1") }
            assertTrue(
                actions.any { it is ExpensesUiAction.ShowDeleteError },
                "Expected ShowDeleteError action"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `multiple delete events are handled independently`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            coEvery { deleteExpenseUseCase(any(), any()) } just Runs
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-2"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, "expense-1") }
            coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, "expense-2") }

            collectJob.cancel()
        }

        @Test
        fun `DeleteExpense does nothing when no group is selected`() = runTest(testDispatcher) {
            // Given - No group selected
            every { getGroupExpensesFlowUseCase(any()) } returns flowOf(emptyList())
            viewModel = createViewModel()

            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            // Note: NOT calling setSelectedGroup

            // When
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            advanceUntilIdle()

            // Then - UseCase should NOT be called
            coVerify(exactly = 0) { deleteExpenseUseCase(any(), any()) }

            collectJob.cancel()
        }
    }

    @Nested
    inner class CancelExpenseEvent {

        @BeforeEach
        fun setUpNested() {
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(listOf(testExpense1))
        }

        @Test
        fun `CancelExpense event calls updateExpenseUseCase with cancelled status`() = runTest(testDispatcher) {
            // Given
            every { getExpenseByIdFlowUseCase("expense-1") } returns flowOf(testExpense1)
            coEvery {
                updateExpenseUseCase(
                    groupId = testGroupId,
                    expense = any(),
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                )
            } returns Result.success(Unit)

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.CancelExpense("expense-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) {
                updateExpenseUseCase(
                    groupId = testGroupId,
                    expense = match { it.id == "expense-1" && it.paymentStatus == PaymentStatus.CANCELLED },
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                )
            }
            collectJob.cancel()
        }

        @Test
        fun `CancelExpense event emits success action when update succeeds`() = runTest(testDispatcher) {
            // Given
            every { getExpenseByIdFlowUseCase("expense-1") } returns flowOf(testExpense1)
            coEvery {
                updateExpenseUseCase(
                    groupId = testGroupId,
                    expense = any(),
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                )
            } returns Result.success(Unit)

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Collect actions in background
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpensesUiEvent.CancelExpense("expense-1"))
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is ExpensesUiAction.ShowCancelSuccess },
                "Expected ShowCancelSuccess action"
            )
            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `CancelExpense event emits error action when update fails`() = runTest(testDispatcher) {
            // Given
            every { getExpenseByIdFlowUseCase("expense-1") } returns flowOf(testExpense1)
            coEvery {
                updateExpenseUseCase(
                    groupId = testGroupId,
                    expense = any(),
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                )
            } returns Result.failure(RuntimeException("Database error"))

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Collect actions in background
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpensesUiEvent.CancelExpense("expense-1"))
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is ExpensesUiAction.ShowCancelError },
                "Expected ShowCancelError action"
            )
            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `CancelExpense event does nothing when expense is not found`() = runTest(testDispatcher) {
            // Given
            every { getExpenseByIdFlowUseCase("expense-1") } returns flowOf(null)

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.CancelExpense("expense-1"))
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { updateExpenseUseCase(any(), any(), any(), any(), any(), any()) }
            collectJob.cancel()
        }

        @Test
        fun `onEvent ExpenseAdded emits ScrollToTop action`() = runTest(testDispatcher) {
            // Given
            viewModel = createViewModel()
            val actions = mutableListOf<ExpensesUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpensesUiEvent.ExpenseAdded)
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is ExpensesUiAction.ScrollToTop },
                "Expected ScrollToTop action"
            )
            actionsJob.cancel()
        }
    }

    @Nested
    inner class SearchExpenses {

        @Test
        fun `searchQuery updates immediately in uiState`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("Din"))
            testScheduler.runCurrent()

            // Then - Query is updated synchronously in state without waiting for debounce
            assertEquals("Din", viewModel.uiState.value.searchQuery)
            // But list is not filtered yet (debounce hasn't passed)
            assertEquals(2, allExpenses().size)

            collectJob.cancel()
        }

        @Test
        fun `debounced search filters expenses by title case-insensitively`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Before search - all 2 expenses visible
            assertEquals(2, allExpenses().size)
            assertEquals(2, viewModel.uiState.value.totalExpensesCount)

            // When - Type "din" (lowercase for "Dinner")
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("din"))
            advanceTimeBy(150) // Half debounce time

            // Still not filtered yet due to 300ms debounce
            assertEquals(2, allExpenses().size)

            // Advance past debounce (300ms total)
            advanceTimeBy(150)
            advanceUntilIdle()

            // Then - Only Dinner is in filtered list, total count is still 2
            assertEquals(1, allExpenses().size)
            assertEquals("Dinner", allExpenses()[0].title)
            assertEquals(2, viewModel.uiState.value.totalExpensesCount)
            assertFalse(viewModel.uiState.value.isSearchResultEmpty)

            collectJob.cancel()
        }

        @Test
        fun `debounced search filters expenses by notes case-insensitively`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2, testExpense3)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            assertEquals(3, allExpenses().size)

            // When - Search for "snacks" (in notes of testExpense3 "Groceries")
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("SNACKS"))
            advanceTimeBy(300)
            advanceUntilIdle()

            // Then - Groceries matches via notes
            assertEquals(1, allExpenses().size)
            assertEquals("Groceries", allExpenses()[0].title)
            assertEquals(3, viewModel.uiState.value.totalExpensesCount)

            collectJob.cancel()
        }

        @Test
        fun `debounced search filters expenses by vendor case-insensitively`() = runTest(testDispatcher) {
            // Given
            val expenseWithVendor = testExpense1.copy(vendor = "Mercadona")
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(expenseWithVendor, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            assertEquals(2, allExpenses().size)

            // When - Search for "mercadona"
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("MERCADONA"))
            advanceTimeBy(300)
            advanceUntilIdle()

            // Then - Matches via vendor
            assertEquals(1, allExpenses().size)
            assertEquals("Dinner", allExpenses()[0].title)
            assertEquals(2, viewModel.uiState.value.totalExpensesCount)

            collectJob.cancel()
        }

        @Test
        fun `debounced search matches diacritics, punctuation, and multiple spaces`() = runTest(testDispatcher) {
            // Given
            val expedition = testExpense1.copy(title = "Expedición a la selva")
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(expedition, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            assertEquals(2, allExpenses().size)

            // When - Search with unaccented, dot-separated, multi-spaced query
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("expedicion    a.la. selva"))
            advanceTimeBy(300)
            advanceUntilIdle()

            // Then - Matches Expedición a la selva
            assertEquals(1, allExpenses().size)
            assertEquals("Expedición a la selva", allExpenses()[0].title)

            collectJob.cancel()
        }

        @Test
        fun `empty search results sets isSearchResultEmpty to true`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // When - Search for non-matching query
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("NonExistentQuery"))
            advanceTimeBy(300)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(0, allExpenses().size)
            assertEquals(2, state.totalExpensesCount)
            assertFalse(state.isGroupEmpty)
            assertTrue(state.isSearchResultEmpty)

            collectJob.cancel()
        }

        @Test
        fun `clearing search query restores full list immediately with 0ms debounce`() = runTest(testDispatcher) {
            // Given
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Search first
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("Dinner"))
            advanceTimeBy(300)
            advanceUntilIdle()
            assertEquals(1, allExpenses().size)

            // When - Clear search
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged(""))
            // No debounce delay needed for blank/cleared query
            advanceUntilIdle()

            // Then - Instantly restored
            assertEquals("", viewModel.uiState.value.searchQuery)
            assertEquals(2, allExpenses().size)

            collectJob.cancel()
        }

        @Test
        fun `changing selected group resets search query to empty`() = runTest(testDispatcher) {
            // Given
            val group1Id = "group-1"
            val group2Id = "group-2"
            every { getGroupExpensesFlowUseCase(group1Id) } returns flowOf(listOf(testExpense1))
            every { getGroupExpensesFlowUseCase(group2Id) } returns flowOf(listOf(testExpense2))

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setSelectedGroup(group1Id)
            advanceUntilIdle()

            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("Dinner"))
            advanceTimeBy(300)
            advanceUntilIdle()
            assertEquals("Dinner", viewModel.uiState.value.searchQuery)

            // When - Switch to group 2
            viewModel.setSelectedGroup(group2Id)
            advanceUntilIdle()

            // Then - Search query is reset
            assertEquals("", viewModel.uiState.value.searchQuery)
            assertEquals(1, allExpenses().size)
            assertEquals("Taxi", allExpenses()[0].title)

            collectJob.cancel()
        }

        @Test
        fun `deleting expense while search active updates filtered list and total count`() = runTest(testDispatcher) {
            // Given
            val expensesFlow = MutableStateFlow(listOf(testExpense1, testExpense2, testExpense3))
            every { getGroupExpensesFlowUseCase(testGroupId) } returns expensesFlow
            coEvery { deleteExpenseUseCase(testGroupId, "expense-1") } coAnswers {
                expensesFlow.value = listOf(testExpense2, testExpense3)
            }

            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            // Search for "Dinner" (matches expense-1)
            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("Dinner"))
            advanceTimeBy(300)
            advanceUntilIdle()
            assertEquals(1, allExpenses().size)
            assertEquals(3, viewModel.uiState.value.totalExpensesCount)

            // When - Delete expense-1
            viewModel.onEvent(ExpensesUiEvent.DeleteExpense("expense-1"))
            advanceUntilIdle()

            // Then - Filtered list is now empty for "Dinner", total count is 2
            assertEquals(0, allExpenses().size)
            assertEquals(2, viewModel.uiState.value.totalExpensesCount)
            assertTrue(viewModel.uiState.value.isSearchResultEmpty)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("TotalSpentSummary")
    inner class TotalSpentSummary {

        @Test
        fun `total spent summary matches sum of groupAmounts when unfiltered`() = runTest(testDispatcher) {
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("7000 EUR", state.formattedTotalSpent)
            assertEquals(2, state.visibleExpensesCount)
            assertFalse(state.isFiltered)

            collectJob.cancel()
        }

        @Test
        fun `cancelled expenses are excluded from total spent calculation`() = runTest(testDispatcher) {
            val cancelledExpense = testExpense2.copy(paymentStatus = PaymentStatus.CANCELLED)
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, cancelledExpense)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("5000 EUR", state.formattedTotalSpent)
            assertEquals(2, state.visibleExpensesCount)
            assertFalse(state.isFiltered)

            collectJob.cancel()
        }

        @Test
        fun `future scheduled expenses are excluded from total spent and set in formattedTotalScheduled`() =
            runTest(testDispatcher) {
                val futureScheduledExpense = testExpense2.copy(
                    paymentStatus = PaymentStatus.SCHEDULED,
                    dueDate = LocalDateTime.now().plusDays(5)
                )
                every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                    listOf(testExpense1, futureScheduledExpense)
                )
                viewModel = createViewModel()
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals("5000 EUR", state.formattedTotalSpent)
                assertEquals("2000 EUR", state.formattedTotalScheduled)
                assertEquals(2, state.visibleExpensesCount)

                collectJob.cancel()
            }

        @Test
        fun `past and today scheduled expenses are in total spent and formattedTotalScheduled is null`() =
            runTest(testDispatcher) {
                val todayScheduledExpense = testExpense2.copy(
                    sourceAmount = 3000L,
                    groupAmount = 3000L,
                    paymentStatus = PaymentStatus.SCHEDULED,
                    dueDate = LocalDateTime.now()
                )
                every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                    listOf(testExpense1, todayScheduledExpense)
                )
                viewModel = createViewModel()
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals("8000 EUR", state.formattedTotalSpent)
                assertNull(state.formattedTotalScheduled)
                assertEquals(2, state.visibleExpensesCount)

                collectJob.cancel()
            }

        @Test
        fun `multi-currency expenses aggregate base groupAmount`() = runTest(testDispatcher) {
            val usdExpense = Expense(
                id = "expense-usd",
                groupId = testGroupId,
                title = "USD Expense",
                sourceAmount = 5000L,
                sourceCurrency = "USD",
                groupAmount = 4600L,
                groupCurrency = "EUR",
                paymentMethod = PaymentMethod.CREDIT_CARD,
                createdBy = "user-1",
                createdAt = LocalDateTime.of(2024, 1, 18, 12, 0)
            )
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, usdExpense)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("9600 EUR", state.formattedTotalSpent)
            assertEquals(2, state.visibleExpensesCount)

            collectJob.cancel()
        }

        @Test
        fun `searching updates total spent and visible count dynamically with isFiltered true`() = runTest(
            testDispatcher
        ) {
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(ExpensesUiEvent.SearchQueryChanged("Dinner"))
            advanceTimeBy(300)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isFiltered)
            assertEquals(1, state.visibleExpensesCount)
            assertEquals("5000 EUR", state.formattedTotalSpent)

            collectJob.cancel()
        }
    }

    @Nested
    @DisplayName("FilterCriteriaEvents")
    inner class FilterCriteriaEvents {

        @Test
        fun `FilterCriteriaChanged updates filterCriteria and filters list`() = runTest(testDispatcher) {
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2, testExpense3)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            assertEquals(3, allExpenses().size)
            assertEquals(0, viewModel.uiState.value.activeFilterCount)
            assertFalse(viewModel.uiState.value.isFiltered)

            // When
            val criteria = ExpenseFilterCriteria(
                selectedMemberIds = setOf("user-2")
            )
            viewModel.onEvent(ExpensesUiEvent.FilterCriteriaChanged(criteria))
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(criteria, state.filterCriteria)
            assertEquals(1, state.activeFilterCount)
            assertTrue(state.isFiltered)

            collectJob.cancel()
        }

        @Test
        fun `ClearFilters resets all filter criteria while preserving search query`() = runTest(testDispatcher) {
            every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(
                listOf(testExpense1, testExpense2)
            )
            viewModel = createViewModel()
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val criteria = ExpenseFilterCriteria(
                searchQuery = "Din",
                selectedMemberIds = setOf("user-1")
            )
            viewModel.onEvent(ExpensesUiEvent.FilterCriteriaChanged(criteria))
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isFiltered)

            // When
            viewModel.onEvent(ExpensesUiEvent.ClearFilters)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertEquals(ExpenseFilterCriteria(searchQuery = "Din"), state.filterCriteria)
            assertEquals("Din", state.searchQuery)
            assertEquals(0, state.activeFilterCount)
            assertTrue(state.isFiltered)
            assertEquals(1, allExpenses().size)

            collectJob.cancel()
        }
    }

    private fun createViewModel() = ExpensesViewModel(
        useCases = ExpensesUseCases(
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            deleteExpenseUseCase = deleteExpenseUseCase,
            getGroupByIdUseCase = getGroupByIdUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getGroupContributionsFlowUseCase = getGroupContributionsFlowUseCase,
            getGroupSubunitsFlowUseCase = getGroupSubunitsFlowUseCase,
            getExpenseByIdFlowUseCase = getExpenseByIdFlowUseCase,
            updateExpenseUseCase = updateExpenseUseCase
        ),
        expenseUiMapper = expenseUiMapper,
        authenticationService = authenticationService,
        observeGroupUseCase = observeGroupUseCase,
        expenseFilterService = expenseFilterService,
        telemetryTracker = telemetryTracker
    )
}
