package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.CategorySpendingUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.CategorySpendingUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.CategorySpendingUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.CategorySpendingUiEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategorySpendingViewModelTest {

    private val getGroupExpensesFlowUseCase = mockk<GetGroupExpensesFlowUseCase>()
    private val getCashWithdrawalsFlowUseCase = mockk<GetCashWithdrawalsFlowUseCase>()
    private val observeGroupUseCase = mockk<ObserveGroupUseCase>()
    private val appConfigService = mockk<AppConfigService>()
    private val expenseFilterService = mockk<ExpenseFilterService>()
    private val categorySpendingUiMapper = mockk<CategorySpendingUiMapper>()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: CategorySpendingViewModel

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { appConfigService.defaultCurrencyCode } returns MutableStateFlow("EUR")
        every { expenseFilterService.filter(any(), any()) } answers { firstArg() }

        viewModel = CategorySpendingViewModel(
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            getCashWithdrawalsFlowUseCase = getCashWithdrawalsFlowUseCase,
            observeGroupUseCase = observeGroupUseCase,
            appConfigService = appConfigService,
            expenseFilterService = expenseFilterService,
            categorySpendingUiMapper = categorySpendingUiMapper,
            computationDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when group is selected unfiltered, loads group, expenses, and withdrawals`() =
        runTest(testDispatcher) {
            val groupId = "group-1"
            val group = mockk<Group> { every { currency } returns "USD" }
            val expenses = listOf<Expense>(mockk { every { groupAmount } returns 1000L })
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
            assertFalse(state.isFiltered)
            assertEquals(mappedItems, state.items)
            assertEquals("$10.00", state.totalFormattedAmount)
            verify { expenseFilterService.filter(expenses, ExpenseFilterCriteria()) }
            verify { categorySpendingUiMapper.mapExpenses(expenses, "USD", withdrawals) }
            verify { categorySpendingUiMapper.calculateTotalCents(expenses, withdrawals) }

            job.cancel()
        }

    @Test
    fun `when group is null, uses default currency from AppConfigService`() = runTest(testDispatcher) {
        val groupId = "group-2"
        val expenses = emptyList<Expense>()
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
        assertFalse(state.isLoading)
        assertFalse(state.isFiltered)
        assertEquals("", state.totalFormattedAmount)
        assertTrue(state.items.isEmpty())

        job.cancel()
    }

    @Test
    fun `when group selected with no active filters, combines expenses and withdrawals into uiState`() =
        runTest(testDispatcher) {
            val groupId = "group-3"
            val group = mockk<Group> { every { currency } returns "USD" }
            val expenses = listOf<Expense>(mockk { every { groupAmount } returns 1000L })
            val withdrawals = listOf<CashWithdrawal>(mockk { every { amountWithdrawn } returns 5000L })
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
            assertFalse(state.isFiltered)
            assertEquals(mappedItems, state.items)
            assertEquals("$12.00", state.totalFormattedAmount)
            verify { categorySpendingUiMapper.mapExpenses(expenses, "USD", withdrawals) }

            job.cancel()
        }

    @Test
    fun `when active category filter is set, filters expenses and excludes withdrawals`() =
        runTest(testDispatcher) {
            val groupId = "group-4"
            val group = mockk<Group> { every { currency } returns "USD" }
            val foodExpense = Expense(id = "e1", groupAmount = 2000L, category = ExpenseCategory.FOOD)
            val transportExpense = Expense(id = "e2", groupAmount = 3000L, category = ExpenseCategory.TRANSPORT)
            val allExpenses = listOf(foodExpense, transportExpense)
            val filteredExpenses = listOf(foodExpense)
            val withdrawals = listOf(CashWithdrawal(id = "w1", amountWithdrawn = 5000L))
            val criteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            val mappedFilteredItems = persistentListOf(mockk<CategorySpendingUiModel>())

            every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
            every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(allExpenses)
            every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(withdrawals)
            every { expenseFilterService.filter(allExpenses, criteria) } returns filteredExpenses
            every {
                categorySpendingUiMapper.mapExpenses(filteredExpenses, "USD", emptyList())
            } returns mappedFilteredItems
            every { categorySpendingUiMapper.calculateTotalCents(filteredExpenses, emptyList()) } returns 2000L
            every { categorySpendingUiMapper.formatTotalAmount(2000L, "USD") } returns "$20.00"

            val job = backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.setSelectedGroup(groupId)
            viewModel.setFilterCriteria(criteria)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isFiltered)
            assertEquals(mappedFilteredItems, state.items)
            assertEquals("$20.00", state.totalFormattedAmount)
            verify { categorySpendingUiMapper.mapExpenses(filteredExpenses, "USD", emptyList()) }
            verify { categorySpendingUiMapper.calculateTotalCents(filteredExpenses, emptyList()) }

            job.cancel()
        }

    @Test
    fun `when date range and member filters are set, delegates criteria to ExpenseFilterService`() =
        runTest(testDispatcher) {
            val groupId = "group-5"
            val group = mockk<Group> { every { currency } returns "EUR" }
            val memberExpense = Expense(id = "e1", groupAmount = 1500L, payerId = "user-1")
            val otherExpense = Expense(id = "e2", groupAmount = 2500L, payerId = "user-2")
            val allExpenses = listOf(memberExpense, otherExpense)
            val filteredExpenses = listOf(memberExpense)
            val criteria = ExpenseFilterCriteria(
                selectedMemberIds = setOf("user-1"),
                startDate = LocalDate.of(2026, 11, 1),
                endDate = LocalDate.of(2026, 11, 15)
            )
            val mappedFilteredItems = persistentListOf(mockk<CategorySpendingUiModel>())

            every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
            every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(allExpenses)
            every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(emptyList())
            every { expenseFilterService.filter(allExpenses, criteria) } returns filteredExpenses
            every {
                categorySpendingUiMapper.mapExpenses(filteredExpenses, "EUR", emptyList())
            } returns mappedFilteredItems
            every { categorySpendingUiMapper.calculateTotalCents(filteredExpenses, emptyList()) } returns 1500L
            every { categorySpendingUiMapper.formatTotalAmount(1500L, "EUR") } returns "€15.00"

            val job = backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.setSelectedGroup(groupId)
            viewModel.setFilterCriteria(criteria)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isFiltered)
            assertEquals(mappedFilteredItems, state.items)
            assertEquals("€15.00", state.totalFormattedAmount)
            verify { expenseFilterService.filter(allExpenses, criteria) }

            job.cancel()
        }

    @Test
    fun `when search filter matches no expenses, emits empty filtered state without withdrawals`() =
        runTest(testDispatcher) {
            val groupId = "group-6"
            val group = mockk<Group> { every { currency } returns "USD" }
            val allExpenses = listOf(Expense(id = "e1", groupAmount = 1000L, title = "Dinner"))
            val withdrawals = listOf(CashWithdrawal(id = "w1", amountWithdrawn = 5000L))
            val criteria = ExpenseFilterCriteria(searchQuery = "nonexistent")

            every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
            every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(allExpenses)
            every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(withdrawals)
            every { expenseFilterService.filter(allExpenses, criteria) } returns emptyList()
            every {
                categorySpendingUiMapper.mapExpenses(emptyList(), "USD", emptyList())
            } returns persistentListOf()
            every { categorySpendingUiMapper.calculateTotalCents(emptyList(), emptyList()) } returns 0L
            every { categorySpendingUiMapper.formatTotalAmount(0L, "USD") } returns ""

            val job = backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.setSelectedGroup(groupId)
            viewModel.setFilterCriteria(criteria)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isFiltered)
            assertTrue(state.items.isEmpty())
            assertEquals("", state.totalFormattedAmount)
            verify { categorySpendingUiMapper.mapExpenses(emptyList(), "USD", emptyList()) }
            verify { categorySpendingUiMapper.calculateTotalCents(emptyList(), emptyList()) }

            job.cancel()
        }

    @Test
    fun `when setFilterCriteria is called before setSelectedGroup, initial emission uses provided filter criteria`() =
        runTest(testDispatcher) {
            val groupId = "group-7"
            val group = mockk<Group> { every { currency } returns "USD" }
            val allExpenses = listOf(Expense(id = "e1", groupAmount = 1000L))
            val criteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            val mappedItems = persistentListOf(mockk<CategorySpendingUiModel>())

            every { observeGroupUseCase(groupId) } returns MutableStateFlow(group)
            every { getGroupExpensesFlowUseCase(groupId) } returns MutableStateFlow(allExpenses)
            every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(emptyList())
            every { expenseFilterService.filter(allExpenses, criteria) } returns allExpenses
            every { categorySpendingUiMapper.mapExpenses(allExpenses, "USD", emptyList()) } returns mappedItems
            every { categorySpendingUiMapper.calculateTotalCents(allExpenses, emptyList()) } returns 1000L
            every { categorySpendingUiMapper.formatTotalAmount(1000L, "USD") } returns "$10.00"

            val job = backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.setFilterCriteria(criteria)
            viewModel.setSelectedGroup(groupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isFiltered)
            assertEquals(mappedItems, state.items)
            verify { expenseFilterService.filter(allExpenses, criteria) }

            job.cancel()
        }

    @Test
    fun `when switching to a different group, resets active filter criteria to unfiltered`() =
        runTest(testDispatcher) {
            val group1 = mockk<Group> { every { currency } returns "USD" }
            val group2 = mockk<Group> { every { currency } returns "EUR" }
            val group1Expenses = listOf(Expense(id = "e1", groupAmount = 1000L))
            val group2Expenses = listOf(Expense(id = "e2", groupAmount = 2000L))
            val group2Withdrawals = listOf(CashWithdrawal(id = "w2", amountWithdrawn = 3000L))
            val activeCriteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            val group2Items = persistentListOf(mockk<CategorySpendingUiModel>())

            every { observeGroupUseCase("group-1") } returns MutableStateFlow(group1)
            every { getGroupExpensesFlowUseCase("group-1") } returns MutableStateFlow(group1Expenses)
            every { getCashWithdrawalsFlowUseCase("group-1") } returns MutableStateFlow(emptyList())
            every { expenseFilterService.filter(group1Expenses, activeCriteria) } returns group1Expenses
            every {
                categorySpendingUiMapper.mapExpenses(group1Expenses, "USD", emptyList())
            } returns persistentListOf()
            every { categorySpendingUiMapper.calculateTotalCents(group1Expenses, emptyList()) } returns 1000L
            every { categorySpendingUiMapper.formatTotalAmount(1000L, "USD") } returns "$10.00"

            every { observeGroupUseCase("group-2") } returns MutableStateFlow(group2)
            every { getGroupExpensesFlowUseCase("group-2") } returns MutableStateFlow(group2Expenses)
            every { getCashWithdrawalsFlowUseCase("group-2") } returns MutableStateFlow(group2Withdrawals)
            every { expenseFilterService.filter(group2Expenses, ExpenseFilterCriteria()) } returns group2Expenses
            every {
                categorySpendingUiMapper.mapExpenses(group2Expenses, "EUR", group2Withdrawals)
            } returns group2Items
            every {
                categorySpendingUiMapper.calculateTotalCents(group2Expenses, group2Withdrawals)
            } returns 2000L
            every { categorySpendingUiMapper.formatTotalAmount(2000L, "EUR") } returns "€20.00"

            val job = backgroundScope.launch { viewModel.uiState.collect { } }

            viewModel.setSelectedGroup("group-1")
            viewModel.setFilterCriteria(activeCriteria)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isFiltered)

            viewModel.setSelectedGroup("group-2")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isFiltered)
            assertEquals(group2Items, state.items)
            assertEquals("€20.00", state.totalFormattedAmount)
            verify { expenseFilterService.filter(group2Expenses, ExpenseFilterCriteria()) }
            verify { categorySpendingUiMapper.mapExpenses(group2Expenses, "EUR", group2Withdrawals) }

            job.cancel()
        }

    @Test
    fun `when flow throws error, emits non-loading empty state`() = runTest(testDispatcher) {
        val groupId = "group-err"
        every { observeGroupUseCase(groupId) } returns MutableStateFlow(null)
        every { getGroupExpensesFlowUseCase(groupId) } returns flow { throw IllegalStateException("DB error") }
        every { getCashWithdrawalsFlowUseCase(groupId) } returns MutableStateFlow(emptyList())

        val job = backgroundScope.launch { viewModel.uiState.collect { } }

        viewModel.setSelectedGroup(groupId)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())

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
