package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.exception.TerminalDownloadException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DeleteExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.DownloadReceiptUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
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
import kotlinx.coroutines.flow.flowOf
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

    private lateinit var getExpenseByIdFlowUseCase: GetExpenseByIdFlowUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase
    private lateinit var getGroupSubunitsUseCase: GetGroupSubunitsUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase
    private lateinit var downloadReceiptUseCase: DownloadReceiptUseCase
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
        getExpenseByIdFlowUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        getCashWithdrawalsFlowUseCase = mockk()
        getGroupSubunitsUseCase = mockk()
        deleteExpenseUseCase = mockk()
        downloadReceiptUseCase = mockk(relaxed = true)
        authenticationService = mockk()
        expenseDetailUiMapper = mockk()

        every { getCashWithdrawalsFlowUseCase(any()) } returns flowOf(emptyList())
        coEvery { getGroupSubunitsUseCase(any()) } returns emptyList()

        every { authenticationService.currentUserId() } returns testUserId
        coEvery { getMemberProfilesUseCase(any()) } returns mapOf(
            testUserId to User(userId = testUserId, displayName = "Alice", email = "alice@example.com")
        )
        every { expenseDetailUiMapper.map(any(), any(), any(), any(), any()) } returns testUiModel

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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then — mapper was called once with the loaded data
            io.mockk.verify(exactly = 1) { expenseDetailUiMapper.map(testExpense, any(), testUserId, any(), any()) }

            collectJob.cancel()
        }

        @Test
        fun `shows error state when expense not found`() = runTest(testDispatcher) {
            // Given
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(null)
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } throws RuntimeException("DB error")
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()
            viewModel.setExpenseId(testExpenseId) // Same id again
            advanceUntilIdle()

            // Then — use case called only once
            io.mockk.verify(exactly = 1) { getExpenseByIdFlowUseCase(testExpenseId) }

            collectJob.cancel()
        }

        @Test
        fun `setExpenseId with different id reloads`() = runTest(testDispatcher) {
            // Given
            val otherId = "expense-999"
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
            every { getExpenseByIdFlowUseCase(otherId) } returns flowOf(testExpense.copy(id = otherId))
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()
            viewModel.setExpenseId(otherId)
            advanceUntilIdle()

            // Then — each id was loaded once
            io.mockk.verify(exactly = 1) { getExpenseByIdFlowUseCase(testExpenseId) }
            io.mockk.verify(exactly = 1) { getExpenseByIdFlowUseCase(otherId) }

            collectJob.cancel()
        }
    }

    @Nested
    inner class DeleteConfirmed {

        @Test
        fun `DeleteConfirmed calls deleteExpenseUseCase with correct groupId and expenseId`() =
            runTest(testDispatcher) {
                // Given
                every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
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
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(testExpense)
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

    @Nested
    inner class ReceiptDownload {

        @Test
        fun `triggers downloadReceiptUseCase when PDF receipt has remoteUrl and blank localUri`() = runTest(
            testDispatcher
        ) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.pdf"
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(expenseWithPdf)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { downloadReceiptUseCase(testExpenseId, "https://example.com/receipt.pdf") }

            collectJob.cancel()
        }

        @Test
        fun `does not trigger downloadReceiptUseCase when PDF receipt already has localUri`() = runTest(
            testDispatcher
        ) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "file:///local/receipt.pdf",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.pdf"
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(expenseWithPdf)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { downloadReceiptUseCase(any(), any()) }

            collectJob.cancel()
        }

        @Test
        fun `does not trigger downloadReceiptUseCase when PDF receipt has no remoteUrl`() = runTest(testDispatcher) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = null
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(expenseWithPdf)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { downloadReceiptUseCase(any(), any()) }

            collectJob.cancel()
        }

        @Test
        fun `does not trigger downloadReceiptUseCase when receipt is not PDF`() = runTest(testDispatcher) {
            // Given
            val imageAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "image/webp",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.webp"
            )
            val expenseWithImage = testExpense.copy(receiptAttachment = imageAttachment)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flowOf(expenseWithImage)
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            // When
            viewModel.setExpenseId(testExpenseId)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { downloadReceiptUseCase(any(), any()) }

            collectJob.cancel()
        }

        @Test
        fun `does not retry download when previous download failed with terminal error`() = runTest(testDispatcher) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.pdf"
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            val exception = TerminalDownloadException(404, "Not Found")
            coEvery { downloadReceiptUseCase(testExpenseId, any()) } returns Result.failure(exception)

            val flow = kotlinx.coroutines.flow.MutableSharedFlow<Expense?>(replay = 1)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setExpenseId(testExpenseId)

            // First emission
            flow.emit(expenseWithPdf)
            advanceUntilIdle()

            // Second emission (re-trigger)
            flow.emit(expenseWithPdf)
            advanceUntilIdle()

            // Then - download should only be triggered once (blacklisted)
            coVerify(exactly = 1) { downloadReceiptUseCase(testExpenseId, "https://example.com/receipt.pdf") }

            collectJob.cancel()
        }

        @Test
        fun `retries download when previous download failed with recoverable error`() = runTest(testDispatcher) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.pdf"
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            // Transient 503 Service Unavailable or general exception is recoverable
            val exception = TerminalDownloadException(503, "Service Unavailable")
            coEvery { downloadReceiptUseCase(testExpenseId, any()) } returns Result.failure(exception)

            val flow = kotlinx.coroutines.flow.MutableSharedFlow<Expense?>(replay = 1)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setExpenseId(testExpenseId)

            // First emission
            flow.emit(expenseWithPdf)
            advanceUntilIdle()

            // Second emission (re-trigger)
            flow.emit(expenseWithPdf)
            advanceUntilIdle()

            // Then - download should be retried (not blacklisted)
            coVerify(exactly = 2) { downloadReceiptUseCase(testExpenseId, "https://example.com/receipt.pdf") }

            collectJob.cancel()
        }

        @Test
        fun `retries download when RetryReceiptDownload event is received`() = runTest(testDispatcher) {
            // Given
            val pdfAttachment = ReceiptAttachment(
                localUri = "",
                mimeType = "application/pdf",
                capturedAtMillis = 1000L,
                remoteUrl = "https://example.com/receipt.pdf"
            )
            val expenseWithPdf = testExpense.copy(receiptAttachment = pdfAttachment)
            val exception = TerminalDownloadException(404, "Not Found")
            coEvery { downloadReceiptUseCase(testExpenseId, any()) } returns Result.failure(exception)

            val flow = kotlinx.coroutines.flow.MutableSharedFlow<Expense?>(replay = 1)
            every { getExpenseByIdFlowUseCase(testExpenseId) } returns flow
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setExpenseId(testExpenseId)

            // First emission triggers first download and blacklists it
            flow.emit(expenseWithPdf)
            advanceUntilIdle()

            // Trigger retry event
            viewModel.onEvent(ExpenseDetailUiEvent.RetryReceiptDownload)
            advanceUntilIdle()

            // Then - download should be triggered twice
            coVerify(exactly = 2) { downloadReceiptUseCase(testExpenseId, "https://example.com/receipt.pdf") }

            collectJob.cancel()
        }
    }

    private fun createViewModel() = ExpenseDetailViewModel(
        getExpenseByIdFlowUseCase = getExpenseByIdFlowUseCase,
        getMemberProfilesUseCase = getMemberProfilesUseCase,
        getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
        getGroupSubunitsUseCase = getGroupSubunitsUseCase,
        deleteExpenseUseCase = deleteExpenseUseCase,
        downloadReceiptUseCase = downloadReceiptUseCase,
        authenticationService = authenticationService,
        expenseDetailUiMapper = expenseDetailUiMapper
    )
}
