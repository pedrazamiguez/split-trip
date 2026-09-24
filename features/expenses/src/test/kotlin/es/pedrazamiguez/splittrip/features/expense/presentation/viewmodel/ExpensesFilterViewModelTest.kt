package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseFilterServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseSearchServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpensesFilterUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.action.ExpensesFilterUiAction
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesFilterUiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesFilterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase
    private lateinit var expenseFilterService: ExpenseFilterService
    private lateinit var observeGroupUseCase: ObserveGroupUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var authenticationService: AuthenticationService
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var expensesFilterUiMapper: ExpensesFilterUiMapper
    private lateinit var viewModel: ExpensesFilterViewModel

    private val testGroupId = "group-123"

    private val expense1 = Expense(
        id = "exp-1",
        groupId = testGroupId,
        title = "Dinner",
        category = ExpenseCategory.FOOD,
        subcategory = ExpenseSubcategory.RESTAURANT,
        sourceAmount = 5000L,
        groupAmount = 5000L,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        payerId = "user-1",
        createdBy = "user-1",
        splits = listOf(
            ExpenseSplit(userId = "user-1", amountCents = 2500L),
            ExpenseSplit(userId = "user-2", amountCents = 2500L)
        ),
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 30)
    )

    private val expense2 = Expense(
        id = "exp-2",
        groupId = testGroupId,
        title = "Taxi",
        category = ExpenseCategory.TRANSPORT,
        subcategory = ExpenseSubcategory.TAXI_RIDESHARE,
        sourceAmount = 2000L,
        groupAmount = 2000L,
        paymentMethod = PaymentMethod.CASH,
        payerId = "user-2",
        createdBy = "user-2",
        splits = listOf(
            ExpenseSplit(userId = "user-2", amountCents = 1000L),
            ExpenseSplit(userId = "user-3", amountCents = 1000L)
        ),
        createdAt = LocalDateTime.of(2024, 1, 16, 10, 0)
    )

    private val expense3 = Expense(
        id = "exp-3",
        groupId = testGroupId,
        title = "Groceries",
        category = ExpenseCategory.FOOD,
        subcategory = ExpenseSubcategory.GROCERIES_SUPERMARKET,
        sourceAmount = 3000L,
        groupAmount = 3000L,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        payerId = "user-3",
        createdBy = "user-1",
        splits = listOf(
            ExpenseSplit(userId = "user-3", amountCents = 3000L)
        ),
        createdAt = LocalDateTime.of(2024, 1, 17, 14, 0)
    )

    private val allExpenses = listOf(expense1, expense2, expense3)

    private val testGroup = Group(
        id = testGroupId,
        name = "Trip to Madrid",
        currency = "EUR",
        members = listOf("user-2", "user-1", "user-3")
    )

    private val user1Profile = User(userId = "user-1", email = "user1@test.com", displayName = "John Doe")
    private val user2Profile = User(userId = "user-2", email = "ana@test.com", displayName = "Ana")
    private val user3Profile = User(userId = "user-3", email = "carlos@test.com", displayName = "Carlos")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        getGroupExpensesFlowUseCase = mockk()
        observeGroupUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        authenticationService = mockk()
        userUiMapper = mockk()
        formattingHelper = mockk {
            every { formatShortDate(any<LocalDate>()) } answers {
                val date = firstArg<LocalDate>()
                "${date.dayOfMonth} ${date.month.name.take(3)}"
            }
            every { formatShortDate(null as LocalDate?) } returns ""
        }
        expensesFilterUiMapper = ExpensesFilterUiMapper(
            formattingHelper = formattingHelper,
            userUiMapper = userUiMapper
        )
        expenseFilterService = ExpenseFilterServiceImpl(expenseSearchService = ExpenseSearchServiceImpl())

        every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(allExpenses)
        every { observeGroupUseCase(testGroupId) } returns flowOf(testGroup)
        every { authenticationService.currentUserId() } returns "user-1"
        coEvery { getMemberProfilesUseCase(any()) } returns mapOf(
            "user-1" to user1Profile,
            "user-2" to user2Profile,
            "user-3" to user3Profile
        )

        every {
            userUiMapper.mapToDisplayName(
                user = any(),
                fallbackUserId = any(),
                currentUserId = any(),
                youLabel = any(),
                selfIdentificationContext = any(),
                gender = any()
            )
        } answers {
            val user = firstArg<User?>()
            val fallback = secondArg<String>()
            val current = thirdArg<String?>()
            if (fallback == current) "You" else user?.displayName ?: fallback
        }

        viewModel = ExpensesFilterViewModel(
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            expenseFilterService = expenseFilterService,
            observeGroupUseCase = observeGroupUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            expensesFilterUiMapper = expensesFilterUiMapper
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("State and Live Match Calculation")
    inner class StateAndMatching {

        @Test
        fun `initial state is loading`() = runTest(testDispatcher) {
            assertTrue(viewModel.uiState.value.isLoading)
        }

        @Test
        fun `setting group loads total count and matching count`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(3, state.totalExpensesCount)
            assertEquals(3, state.matchingExpensesCount)
            assertEquals(testGroupId, state.groupId)
            assertFalse(state.canReset)

            collectJob.cancel()
        }

        @Test
        fun `setting group loads available members with current user first and alphabetical order`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(3, state.availableMembers.size)

                // Current user first
                assertEquals("user-1", state.availableMembers[0].userId)
                assertEquals("You", state.availableMembers[0].displayName)
                assertTrue(state.availableMembers[0].isCurrentUser)

                // Remaining members sorted alphabetically
                assertEquals("user-2", state.availableMembers[1].userId)
                assertEquals("Ana", state.availableMembers[1].displayName)
                assertFalse(state.availableMembers[1].isCurrentUser)

                assertEquals("user-3", state.availableMembers[2].userId)
                assertEquals("Carlos", state.availableMembers[2].displayName)
                assertFalse(state.availableMembers[2].isCurrentUser)

                collectJob.cancel()
            }

        @Test
        fun `available members self-identification passes NOMINATIVE context`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                coVerify {
                    userUiMapper.mapToDisplayName(
                        user = user1Profile,
                        fallbackUserId = "user-1",
                        currentUserId = "user-1",
                        selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
                    )
                }

                collectJob.cancel()
            }

        @Test
        fun `initialize sets draft criteria and calculates match count`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val initialCriteria = ExpenseFilterCriteria(
                selectedCategories = setOf(ExpenseCategory.FOOD)
            )
            viewModel.onEvent(ExpensesFilterUiEvent.Initialize(initialCriteria))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(initialCriteria, state.draftCriteria)
            assertEquals(2, state.matchingExpensesCount)
            assertEquals(3, state.totalExpensesCount)
            assertTrue(state.canReset)

            collectJob.cancel()
        }

        @Test
        fun `subsequent initialize calls are ignored`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            val firstCriteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            val secondCriteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.TRANSPORT))

            viewModel.onEvent(ExpensesFilterUiEvent.Initialize(firstCriteria))
            viewModel.onEvent(ExpensesFilterUiEvent.Initialize(secondCriteria))
            advanceUntilIdle()

            assertEquals(firstCriteria, viewModel.uiState.value.draftCriteria)
            assertEquals(2, viewModel.uiState.value.matchingExpensesCount)

            collectJob.cancel()
        }

        @Test
        fun `updateDraft recalculates matching expenses dynamically`() = runTest(testDispatcher) {
            val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
            viewModel.setSelectedGroup(testGroupId)
            advanceUntilIdle()

            viewModel.onEvent(
                ExpensesFilterUiEvent.UpdateDraft(
                    ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.TRANSPORT))
                )
            )
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.matchingExpensesCount)
            assertTrue(state.canReset)

            collectJob.cancel()
        }

        @Test
        fun `updating draft with category selection recalculates matching count in real time`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                viewModel.onEvent(
                    ExpensesFilterUiEvent.UpdateDraft(
                        ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
                    )
                )
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(2, state.matchingExpensesCount)
                assertTrue(state.canReset)

                collectJob.cancel()
            }

        @Test
        fun `selecting subcategory refines match count in real time`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                viewModel.onEvent(
                    ExpensesFilterUiEvent.UpdateDraft(
                        ExpenseFilterCriteria(
                            selectedCategories = setOf(ExpenseCategory.FOOD),
                            selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT)
                        )
                    )
                )
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(1, state.matchingExpensesCount)
                assertTrue(state.canReset)

                collectJob.cancel()
            }

        @Test
        fun `selecting single member filter recalculates match count dynamically`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                viewModel.onEvent(
                    ExpensesFilterUiEvent.UpdateDraft(
                        ExpenseFilterCriteria(selectedMemberIds = setOf("user-1"))
                    )
                )
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(1, state.matchingExpensesCount)
                assertTrue(state.canReset)

                collectJob.cancel()
            }

        @Test
        fun `selecting multi-member filter performs union matching`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                viewModel.onEvent(
                    ExpensesFilterUiEvent.UpdateDraft(
                        ExpenseFilterCriteria(selectedMemberIds = setOf("user-1", "user-2"))
                    )
                )
                advanceUntilIdle()

                val state = viewModel.uiState.value
                // expense1 matches user-1 and user-2, expense2 matches user-2 -> 2 expenses
                assertEquals(2, state.matchingExpensesCount)
                assertTrue(state.canReset)

                collectJob.cancel()
            }

        @Test
        fun `graceful fallback when member profiles are missing`() =
            runTest(testDispatcher) {
                coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }

                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(3, state.availableMembers.size)
                assertEquals("user-1", state.availableMembers[0].userId)
                assertTrue(state.availableMembers[0].isCurrentUser)

                collectJob.cancel()
            }

        @Test
        fun `resetDraft clears non-search filter dimensions including selected members`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val initialCriteria = ExpenseFilterCriteria(
                    searchQuery = "Din",
                    selectedCategories = setOf(ExpenseCategory.FOOD),
                    selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT),
                    selectedMemberIds = setOf("user-1", "user-2")
                )
                viewModel.onEvent(ExpensesFilterUiEvent.Initialize(initialCriteria))
                advanceUntilIdle()

                viewModel.onEvent(ExpensesFilterUiEvent.ResetDraft)
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals("Din", state.draftCriteria.searchQuery)
                assertTrue(state.draftCriteria.selectedCategories.isEmpty())
                assertTrue(state.draftCriteria.selectedSubcategories.isEmpty())
                assertTrue(state.draftCriteria.selectedMemberIds.isEmpty())
                assertEquals(0, state.draftCriteria.activeFilterCount)
                assertFalse(state.canReset)

                collectJob.cancel()
            }
    }

    @Nested
    @DisplayName("Action Emissions")
    inner class ActionEmissions {

        @Test
        fun `applyFilters emits ApplyAndNavigateBack action with current draft criteria`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                val actions = mutableListOf<ExpensesFilterUiAction>()
                val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.actions.collect { actions.add(it) }
                }

                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val filterCriteria = ExpenseFilterCriteria(
                    selectedCategories = setOf(ExpenseCategory.FOOD),
                    selectedMemberIds = setOf("user-1")
                )
                viewModel.onEvent(ExpensesFilterUiEvent.UpdateDraft(filterCriteria))
                advanceUntilIdle()
                viewModel.onEvent(ExpensesFilterUiEvent.ApplyFilters)
                advanceUntilIdle()

                assertEquals(1, actions.size)
                val action = actions.first() as ExpensesFilterUiAction.ApplyAndNavigateBack
                assertEquals(filterCriteria, action.appliedCriteria)

                actionsJob.cancel()
                collectJob.cancel()
            }

        @Test
        fun `resetDraft emits FiltersReset action with cleared criteria`() =
            runTest(testDispatcher) {
                val collectJob = backgroundScope.launch { viewModel.uiState.collect {} }
                val actions = mutableListOf<ExpensesFilterUiAction>()
                val actionsJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.actions.collect { actions.add(it) }
                }
                viewModel.setSelectedGroup(testGroupId)
                advanceUntilIdle()

                val initialCriteria = ExpenseFilterCriteria(
                    searchQuery = "Din",
                    selectedCategories = setOf(ExpenseCategory.FOOD),
                    selectedSubcategories = setOf(ExpenseSubcategory.RESTAURANT),
                    selectedMemberIds = setOf("user-1"),
                    startDate = LocalDate.of(2024, 1, 15),
                    endDate = LocalDate.of(2024, 1, 17)
                )
                viewModel.onEvent(ExpensesFilterUiEvent.Initialize(initialCriteria))
                advanceUntilIdle()
                viewModel.onEvent(ExpensesFilterUiEvent.ResetDraft)
                advanceUntilIdle()

                assertEquals(1, actions.size)
                val action = actions.first() as ExpensesFilterUiAction.FiltersReset
                assertEquals("Din", action.clearedCriteria.searchQuery)
                assertTrue(action.clearedCriteria.selectedCategories.isEmpty())
                assertTrue(action.clearedCriteria.selectedSubcategories.isEmpty())
                assertTrue(action.clearedCriteria.selectedMemberIds.isEmpty())
                assertNull(action.clearedCriteria.startDate)
                assertNull(action.clearedCriteria.endDate)
                assertFalse(viewModel.uiState.value.canReset)
                actionsJob.cancel()
                collectJob.cancel()
            }
    }

    @Nested
    @DisplayName("SavedStateHandle Integration")
    inner class SavedStateHandleIntegration {
        @Test
        fun `expenseFilterCriteria can be stored and retrieved from SavedStateHandle without error`() {
            val criteria = ExpenseFilterCriteria(
                searchQuery = "groceries",
                selectedCategories = setOf(ExpenseCategory.FOOD),
                selectedMemberIds = setOf("user-1")
            )
            val handle = SavedStateHandle()
            handle["initialFilterCriteria"] = criteria
            val retrieved = handle.get<ExpenseFilterCriteria>("initialFilterCriteria")
            assertEquals(criteria, retrieved)
        }
    }

    @Nested
    @DisplayName("Date Range Filtering and Bounds")
    inner class DateRangeFilteringAndBounds {

        @Test
        fun `populates oldestExpenseDate and newestExpenseDate from group expenses`() =
            runTest(testDispatcher) {
                viewModel.setSelectedGroup(testGroupId)
                val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect {}
                }
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals(LocalDate.of(2024, 1, 15), state.oldestExpenseDate)
                assertEquals(LocalDate.of(2024, 1, 17), state.newestExpenseDate)

                collectJob.cancel()
            }

        @Test
        fun `formats startDate and endDate in state when present in draftCriteria`() =
            runTest(testDispatcher) {
                viewModel.setSelectedGroup(testGroupId)
                val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
                    viewModel.uiState.collect {}
                }
                advanceUntilIdle()

                val criteria = ExpenseFilterCriteria(
                    startDate = LocalDate.of(2024, 1, 16),
                    endDate = LocalDate.of(2024, 1, 17)
                )
                viewModel.onEvent(ExpensesFilterUiEvent.UpdateDraft(criteria))
                advanceUntilIdle()

                val state = viewModel.uiState.value
                assertEquals("16 JAN", state.formattedStartDate)
                assertEquals("17 JAN", state.formattedEndDate)
                assertEquals(2, state.matchingExpensesCount)

                collectJob.cancel()
            }
    }
}
