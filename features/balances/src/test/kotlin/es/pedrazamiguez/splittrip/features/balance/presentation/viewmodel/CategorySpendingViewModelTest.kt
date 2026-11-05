package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.features.balance.presentation.mapper.CategorySpendingUiMapper
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.action.CategorySpendingUiAction
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event.CategorySpendingUiEvent
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategorySpendingViewModelTest {

    private val getGroupExpensesFlowUseCase = mockk<GetGroupExpensesFlowUseCase>()
    private val getCashWithdrawalsFlowUseCase = mockk<GetCashWithdrawalsFlowUseCase>()
    private val observeGroupUseCase = mockk<ObserveGroupUseCase>()
    private val appConfigService = mockk<AppConfigService>()
    private val categorySpendingUiMapper = mockk<CategorySpendingUiMapper>()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CategorySpendingViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { appConfigService.defaultCurrencyCode } returns MutableStateFlow("EUR")

        viewModel = CategorySpendingViewModel(
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            observeGroupUseCase = observeGroupUseCase,
            appConfigService = appConfigService,
            categorySpendingUiMapper = categorySpendingUiMapper,
            computationDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when group is selected, loads group and expenses and updates state`() = runTest(testDispatcher) {
        val groupId = "group-1"
        val group = mockk<Group> {
            every { currency } returns "USD"
        }
        val expenses = listOf<Expense>(
            mockk {
                every { groupAmount } returns 1000L
            }
        )
        val withdrawals = emptyList<CashWithdrawal>()
        val mappedItems = persistentListOf<CategorySpendingUiModel>()

        every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
        every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(expenses)
        every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(withdrawals)
        every { categorySpendingUiMapper.mapExpenses(expenses, "USD", withdrawals) } returns mappedItems
        every { categorySpendingUiMapper.calculateTotalCents(expenses, withdrawals) } returns 1000L
        every { categorySpendingUiMapper.formatTotalAmount(1000L, "USD") } returns "$10.00"

        val job = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.setSelectedGroup(groupId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mappedItems, state.items)
        assertEquals("$10.00", state.totalFormattedAmount)

        job.cancel()
    }

    @Test
    fun `when group is null, uses default currency`() = runTest(testDispatcher) {
        val groupId = "group-2"
        val expenses = listOf<Expense>()
        val withdrawals = emptyList<CashWithdrawal>()

        every { observeGroupUseCase(groupId) } returns MutableStateFlow(null)
        every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(expenses)
        every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(withdrawals)
        every { categorySpendingUiMapper.mapExpenses(expenses, "EUR", withdrawals) } returns persistentListOf()
        every { categorySpendingUiMapper.calculateTotalCents(expenses, withdrawals) } returns 0L
        every { categorySpendingUiMapper.formatTotalAmount(0L, "EUR") } returns ""

        val job = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.setSelectedGroup(groupId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.totalFormattedAmount)
        assertEquals(0, state.items.size)

        job.cancel()
    }

    @Test
    fun `when group selected, combines expenses and withdrawals into uiState`() = runTest(testDispatcher) {
        val groupId = "group-3"
        val group = mockk<Group> {
            every { currency } returns "USD"
        }
        val expenses = listOf<Expense>(
            mockk {
                every { groupAmount } returns 1000L
            }
        )
        val withdrawals = listOf<CashWithdrawal>(
            mockk {
                every { amountWithdrawn } returns 5000L
            }
        )
        val mappedItems = persistentListOf<CategorySpendingUiModel>()

        every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
        every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(expenses)
        every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(withdrawals)
        every { categorySpendingUiMapper.mapExpenses(expenses, "USD", withdrawals) } returns mappedItems
        every { categorySpendingUiMapper.calculateTotalCents(expenses, withdrawals) } returns 1200L
        every { categorySpendingUiMapper.formatTotalAmount(1200L, "USD") } returns "$12.00"

        val job = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.setSelectedGroup(groupId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mappedItems, state.items)
        assertEquals("$12.00", state.totalFormattedAmount)

        job.cancel()
    }

    @Test
    fun `onEvent OnNavigateBack emits NavigateBack action`() = runTest(testDispatcher) {
        val actions = mutableListOf<CategorySpendingUiAction>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.actions.collect { actions.add(it) }
        }

        viewModel.onEvent(CategorySpendingUiEvent.OnNavigateBack)
        advanceUntilIdle()

        assertEquals(1, actions.size)
        assertEquals(CategorySpendingUiAction.NavigateBack, actions.first())

        job.cancel()
    }
}
