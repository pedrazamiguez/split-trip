package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpenseDetailUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.ExpenseDetailUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpenseDetailUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpenseDetailUiEvent
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.math.BigDecimal
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var expenseDetailUiMapper: ExpenseDetailUiMapper
    private lateinit var viewModel: ExpenseDetailViewModel

    private val testExpenseId = "expense-123"
    private val testGroupId = "group-456"
    private val testUserId = "user-current"

    private val testExpense = Expense(
        id = testExpenseId,
        groupId = testGroupId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        createdBy = testUserId
    )

    private val testUiModel = ExpenseDetailUiModel(
        id = testExpenseId,
        groupId = testGroupId,
        title = "Dinner",
        formattedGroupAmount = "€50.00",
        groupCurrency = "EUR",
        sourceCurrency = "EUR",
        isForeignCurrency = false,
        paymentMethodText = "Credit Card",
        paymentStatusText = "Finished",
        paidByText = "Paid by Alice",
        dateText = "Jun 15, 2024",
        categoryText = "Food",
        splitTypeText = "Equal",
        splits = persistentListOf(),
        addOns = persistentListOf(),
        cashTranches = persistentListOf(),
        isOutOfPocket = false,
        hasAddOns = false,
        isScheduledPastDue = false,
        syncStatus = SyncStatus.SYNCED,
        createdByText = "Alice",
        createdAtText = "Jun 15, 2024"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getExpenseByIdUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        deleteExpenseUseCase = mockk()
        authenticationService = mockk()
        expenseDetailUiMapper = mockk()

        every { authenticationService.currentUserId() } returns testUserId
        coEvery { getMemberProfilesUseCase(any()) } returns mapOf(
            testUserId to User(userId = testUserId, displayName = "Alice", email = "alice@example.com")
        )
        every { expenseDetailUiMapper.map(any(), any(), any()) } returns testUiModel

        viewModel = createViewModel()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    inner class InitialState {

        @Test
        fun `initial state shows loading`() {
            // Then
            assertTrue(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.expense)
            assertFalse(viewModel.uiState.value.hasError)
        }

        @Test
        fun `state stays loading until expense ID is set`() = runTest(testDispatcher) {
            // Given — no setExpenseId call

            // Then
            assertTrue(viewModel.uiState.value.isLoading)
        }
    }

    @Nested
    inner class LoadingExpense {

        @Test
        fun `loads expense when setExpenseId is called`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
            assertNotNull(state.expense)
            assertEquals(testExpenseId, state.expense?.id)

            collectJob.cancel()
        }

        @Test
        fun `calls mapper with expense, member profiles and current user`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then — mapper was called once with the loaded data
            coVerify(exactly = 1) { expenseDetailUiMapper.map(testExpense, any(), testUserId) }

            collectJob.cancel()
        }

        @Test
        fun `shows error state when expense not found`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns null
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
            assertNull(state.expense)

            collectJob.cancel()
        }

        @Test
        fun `shows error state when getExpenseByIdUseCase throws`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } throws RuntimeException("DB error")
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.hasError)
            assertNull(state.expense)

            collectJob.cancel()
        }

        @Test
        fun `falls back to empty profiles when getMemberProfilesUseCase throws`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            coEvery { getMemberProfilesUseCase(any()) } throws RuntimeException("Network error")
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then — expense still mapped with empty profiles
            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.hasError)
            assertNotNull(state.expense)

            collectJob.cancel()
        }

        @Test
        fun `setExpenseId with same id does not reload`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()
            viewModel.setExpenseId(testExpenseId) // Same id again
            advanceUntilIdle()

            // Then — use case called only once
            coVerify(exactly = 1) { getExpenseByIdUseCase(testExpenseId) }

            collectJob.cancel()
        }

        @Test
        fun `setExpenseId with different id reloads`() = runTest(testDispatcher) {
            // Given
            val otherId = "expense-999"
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            coEvery { getExpenseByIdUseCase(otherId) } returns testExpense.copy(id = otherId)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()
            viewModel.setExpenseId(otherId)
            advanceUntilIdle()

            // Then — each id was loaded once
            coVerify(exactly = 1) { getExpenseByIdUseCase(testExpenseId) }
            coVerify(exactly = 1) { getExpenseByIdUseCase(otherId) }

            collectJob.cancel()
        }
    }

    @Nested
    inner class DeleteConfirmed {

        @Test
        fun `DeleteConfirmed calls deleteExpenseUseCase with correct groupId and expenseId`() =
            runTest(testDispatcher) {
                // Given
                coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
                coEvery { deleteExpenseUseCase(any(), any()) } just Runs
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setExpenseId(testExpenseId)
                advanceUntilIdle()

                // When
                viewModel.onEvent(ExpenseDetailUiEvent.DeleteConfirmed)
                advanceUntilIdle()

                // Then
                coVerify(exactly = 1) { deleteExpenseUseCase(testGroupId, testExpenseId) }

                collectJob.cancel()
            }

        @Test
        fun `DeleteConfirmed emits DeleteSuccess action on success`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            coEvery { deleteExpenseUseCase(any(), any()) } just Runs
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            val actions = mutableListOf<ExpenseDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpenseDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is ExpenseDetailUiAction.DeleteSuccess },
                "Expected DeleteSuccess action but got: $actions"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteConfirmed emits ShowError action when deletion fails`() = runTest(testDispatcher) {
            // Given
            coEvery { getExpenseByIdUseCase(testExpenseId) } returns testExpense
            coEvery { deleteExpenseUseCase(any(), any()) } throws RuntimeException("Delete failed")
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            val actions = mutableListOf<ExpenseDetailUiAction>()
            val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.actions.collect { actions.add(it) }
            }

            // When
            viewModel.onEvent(ExpenseDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            // Then
            assertTrue(
                actions.any { it is ExpenseDetailUiAction.ShowError },
                "Expected ShowError action but got: $actions"
            )

            actionsJob.cancel()
            collectJob.cancel()
        }

        @Test
        fun `DeleteConfirmed does nothing when expense is not yet loaded`() = runTest(testDispatcher) {
            // Given — no setExpenseId, so uiState.expense is null

            // When
            viewModel.onEvent(ExpenseDetailUiEvent.DeleteConfirmed)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { deleteExpenseUseCase(any(), any()) }
        }
    }

    private fun createViewModel() = ExpenseDetailViewModel(
        getExpenseByIdUseCase = getExpenseByIdUseCase,
        getMemberProfilesUseCase = getMemberProfilesUseCase,
        deleteExpenseUseCase = deleteExpenseUseCase,
        authenticationService = authenticationService,
        expenseDetailUiMapper = expenseDetailUiMapper
    )
}
