package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
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

        // Default mock for group and member profiles
        coEvery { getGroupByIdUseCase(any()) } returns Group(
            id = testGroupId,
            name = "Test Group",
            currency = "EUR"
        )
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { getGroupContributionsFlowUseCase(any()) } returns flowOf(emptyList())
        every { getGroupSubunitsFlowUseCase(any()) } returns flowOf(emptyList())
        every { authenticationService.currentUserId() } returns "current-user-id"

        // Mock the mapper to return predictable grouped UI models
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
        authenticationService = authenticationService
    )
}
