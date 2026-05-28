package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.strategy

import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetExpenseByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.UpdateExpenseUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.AddExpenseUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.WithdrawalPoolOptionUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.AddExpenseUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler.ConfigEventHandler
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseStep
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.state.AddExpenseUiState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditExpenseFlowStrategyTest {

    private val expenseId = "expense-42"
    private val groupId = "group-1"

    private lateinit var configEventHandler: ConfigEventHandler
    private lateinit var getExpenseByIdUseCase: GetExpenseByIdUseCase
    private lateinit var getContributionByExpenseIdUseCase: GetContributionByExpenseIdUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var addExpenseUiMapper: AddExpenseUiMapper
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var getGroupSubunitsUseCase: GetGroupSubunitsUseCase
    private lateinit var strategy: EditExpenseFlowStrategy

    private lateinit var uiState: MutableStateFlow<AddExpenseUiState>
    private lateinit var actions: MutableSharedFlow<AddExpenseUiAction>

    private val sampleExpense = Expense(
        id = expenseId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 5000L,
        groupCurrency = "EUR",
        paymentMethod = PaymentMethod.CREDIT_CARD
    )

    @BeforeEach
    fun setUp() {
        configEventHandler = mockk(relaxed = true)
        getExpenseByIdUseCase = mockk()
        getContributionByExpenseIdUseCase = mockk()
        updateExpenseUseCase = mockk()
        addExpenseUiMapper = mockk()
        getMemberProfilesUseCase = mockk()
        getGroupSubunitsUseCase = mockk()

        strategy = EditExpenseFlowStrategy(
            expenseId = expenseId,
            configEventHandler = configEventHandler,
            getExpenseByIdUseCase = getExpenseByIdUseCase,
            getContributionByExpenseIdUseCase = getContributionByExpenseIdUseCase,
            updateExpenseUseCase = updateExpenseUseCase,
            addExpenseUiMapper = addExpenseUiMapper,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            getGroupSubunitsUseCase = getGroupSubunitsUseCase
        )

        uiState = MutableStateFlow(AddExpenseUiState())
        actions = MutableSharedFlow(extraBufferCapacity = 8)
    }

    // ── Metadata contract ─────────────────────────────────────────────────────

    @Test
    fun `strategy exposes edit-mode metadata`() {
        assertTrue(strategy.isEditMode)
        assertEquals(AddExpenseStep.TITLE, strategy.startStep)
        assertTrue(strategy.screenTitleRes != 0)
        assertTrue(strategy.submitLabelRes != 0)
    }

    // ── loadInitialData ───────────────────────────────────────────────────────

    @Nested
    inner class LoadInitialData {

        @Test
        fun `does nothing when groupId is null`() = runTest {
            var onLoadedCalled = false
            strategy.loadInitialData(
                groupId = null,
                uiState = uiState,
                actions = actions,
                scope = this,
                onConfigLoaded = { onLoadedCalled = true }
            )
            advanceUntilIdle()

            assertFalse(onLoadedCalled)
            coVerify(exactly = 0) { configEventHandler.suspendLoadGroupConfig(any(), any()) }
            coVerify(exactly = 0) { getExpenseByIdUseCase(any()) }
        }

        @Test
        fun `flags configLoadFailed when expense is not found`() = runTest {
            coEvery { configEventHandler.suspendLoadGroupConfig(groupId, false) } just Runs
            coEvery { getExpenseByIdUseCase(expenseId) } returns null

            var onLoadedCalled = false
            strategy.loadInitialData(
                groupId = groupId,
                uiState = uiState,
                actions = actions,
                scope = this,
                onConfigLoaded = { onLoadedCalled = true }
            )
            advanceUntilIdle()

            assertFalse(onLoadedCalled)
            assertTrue(uiState.value.configLoadFailed)
            assertFalse(uiState.value.isLoading)
            // Should NOT invoke downstream mappers/use cases when expense missing
            coVerify(exactly = 0) { getMemberProfilesUseCase(any()) }
            coVerify(exactly = 0) { addExpenseUiMapper.mapExpenseToState(any(), any(), any(), any(), any()) }
        }

        @Test
        fun `loads config, expense, contribution, members and subunits on success`() = runTest {
            val contribution = mockk<Contribution>(relaxed = true)
            val memberProfiles = emptyMap<String, User>()
            val subunits = listOf<Subunit>()
            val mappedState = AddExpenseUiState(currentStep = AddExpenseStep.REVIEW)

            coEvery { configEventHandler.suspendLoadGroupConfig(groupId, false) } just Runs
            coEvery { getExpenseByIdUseCase(expenseId) } returns sampleExpense
            coEvery { getContributionByExpenseIdUseCase(groupId, expenseId) } returns contribution
            coEvery { getMemberProfilesUseCase(any()) } returns memberProfiles
            coEvery { getGroupSubunitsUseCase(groupId) } returns subunits
            every {
                addExpenseUiMapper.mapExpenseToState(
                    expense = any(),
                    contribution = any(),
                    currentState = any(),
                    memberProfiles = any(),
                    subunits = any()
                )
            } returns mappedState

            var onLoadedCalled = false
            strategy.loadInitialData(
                groupId = groupId,
                uiState = uiState,
                actions = actions,
                scope = this,
                onConfigLoaded = { onLoadedCalled = true }
            )
            advanceUntilIdle()

            assertTrue(onLoadedCalled)
            assertTrue(uiState.value.isConfigLoaded)
            assertFalse(uiState.value.isLoading)
            assertTrue(uiState.value.isEditMode)
            assertFalse(uiState.value.configLoadFailed)
            assertEquals(AddExpenseStep.TITLE, uiState.value.currentStep)
            assertFalse(uiState.value.isAiModeActive)

            coVerify(exactly = 1) { configEventHandler.suspendLoadGroupConfig(groupId, false) }
            coVerify(exactly = 1) { getExpenseByIdUseCase(expenseId) }
            coVerify(exactly = 1) { getContributionByExpenseIdUseCase(groupId, expenseId) }
            coVerify(exactly = 1) { getMemberProfilesUseCase(any()) }
            coVerify(exactly = 1) { getGroupSubunitsUseCase(groupId) }
        }

        @Test
        fun `propagates forceRefresh to ConfigEventHandler`() = runTest {
            coEvery { configEventHandler.suspendLoadGroupConfig(groupId, true) } just Runs
            coEvery { getExpenseByIdUseCase(expenseId) } returns null

            strategy.loadInitialData(
                groupId = groupId,
                uiState = uiState,
                actions = actions,
                scope = this,
                forceRefresh = true,
                onConfigLoaded = { }
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { configEventHandler.suspendLoadGroupConfig(groupId, true) }
        }

        @Test
        fun `flags configLoadFailed when an exception is thrown`() = runTest {
            coEvery {
                configEventHandler.suspendLoadGroupConfig(any(), any())
            } throws RuntimeException("boom")

            var onLoadedCalled = false
            strategy.loadInitialData(
                groupId = groupId,
                uiState = uiState,
                actions = actions,
                scope = this,
                onConfigLoaded = { onLoadedCalled = true }
            )
            advanceUntilIdle()

            assertFalse(onLoadedCalled)
            assertTrue(uiState.value.configLoadFailed)
            assertFalse(uiState.value.isLoading)
        }
    }

    // ── saveExpense ───────────────────────────────────────────────────────────

    @Nested
    inner class SaveExpense {

        @Test
        fun `forwards expense with strategy expenseId to UpdateExpenseUseCase`() = runTest {
            val expenseFromUi = sampleExpense.copy(id = "stale-id-from-ui")
            val state = AddExpenseUiState(contributionScope = PayerType.USER)
            val expenseSlot = slot<Expense>()
            coEvery {
                updateExpenseUseCase(
                    groupId = any(),
                    expense = capture(expenseSlot),
                    pairedContributionScope = any(),
                    pairedSubunitId = any(),
                    preferredWithdrawalScope = any(),
                    preferredWithdrawalOwnerId = any()
                )
            } returns Result.success(Unit)

            val result = strategy.saveExpense(groupId, expenseFromUi, state)

            assertTrue(result.isSuccess)
            assertEquals(expenseId, expenseSlot.captured.id)
        }

        @Test
        fun `forwards SUBUNIT contribution scope with selected subunit id`() = runTest {
            val state = AddExpenseUiState(
                contributionScope = PayerType.SUBUNIT,
                selectedContributionSubunitId = "subunit-7"
            )
            coEvery {
                updateExpenseUseCase(any(), any(), any(), any(), any(), any())
            } returns Result.success(Unit)

            strategy.saveExpense(groupId, sampleExpense, state)

            coVerify(exactly = 1) {
                updateExpenseUseCase(
                    groupId = groupId,
                    expense = any(),
                    pairedContributionScope = PayerType.SUBUNIT,
                    pairedSubunitId = "subunit-7",
                    preferredWithdrawalScope = any(),
                    preferredWithdrawalOwnerId = any()
                )
            }
        }

        @Test
        fun `nullifies pairedSubunitId when scope is USER`() = runTest {
            val state = AddExpenseUiState(
                contributionScope = PayerType.USER,
                selectedContributionSubunitId = "stale-subunit"
            )
            coEvery {
                updateExpenseUseCase(any(), any(), any(), any(), any(), any())
            } returns Result.success(Unit)

            strategy.saveExpense(groupId, sampleExpense, state)

            coVerify(exactly = 1) {
                updateExpenseUseCase(
                    groupId = groupId,
                    expense = any(),
                    pairedContributionScope = PayerType.USER,
                    pairedSubunitId = null,
                    preferredWithdrawalScope = any(),
                    preferredWithdrawalOwnerId = any()
                )
            }
        }

        @Test
        fun `forwards selected withdrawal pool scope and owner`() = runTest {
            val pool = WithdrawalPoolOptionUiModel(
                scope = PayerType.USER,
                ownerId = "user-9",
                displayLabel = "User pool"
            )
            val state = AddExpenseUiState(
                contributionScope = PayerType.USER,
                selectedWithdrawalPool = pool
            )
            coEvery {
                updateExpenseUseCase(any(), any(), any(), any(), any(), any())
            } returns Result.success(Unit)

            strategy.saveExpense(groupId, sampleExpense, state)

            coVerify(exactly = 1) {
                updateExpenseUseCase(
                    groupId = groupId,
                    expense = any(),
                    pairedContributionScope = any(),
                    pairedSubunitId = any(),
                    preferredWithdrawalScope = PayerType.USER,
                    preferredWithdrawalOwnerId = "user-9"
                )
            }
        }

        @Test
        fun `passes null withdrawal pool scope and owner when none selected`() = runTest {
            val state = AddExpenseUiState(selectedWithdrawalPool = null)
            coEvery {
                updateExpenseUseCase(any(), any(), any(), any(), any(), any())
            } returns Result.success(Unit)

            strategy.saveExpense(groupId, sampleExpense, state)

            coVerify(exactly = 1) {
                updateExpenseUseCase(
                    groupId = groupId,
                    expense = any(),
                    pairedContributionScope = any(),
                    pairedSubunitId = any(),
                    preferredWithdrawalScope = null,
                    preferredWithdrawalOwnerId = null
                )
            }
        }

        @Test
        fun `propagates failure result from UpdateExpenseUseCase`() = runTest {
            val state = AddExpenseUiState()
            val failure = Result.failure<Unit>(IllegalStateException("nope"))
            coEvery {
                updateExpenseUseCase(any(), any(), any(), any(), any(), any())
            } returns failure

            val result = strategy.saveExpense(groupId, sampleExpense, state)

            assertTrue(result.isFailure)
            assertEquals("nope", result.exceptionOrNull()?.message)
        }
    }
}
