package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseFilterServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseSearchServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.features.expense.presentation.mapper.ExpensesFilterUiMapper
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.ExpensesFilterUiEvent
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ExpensesFilterViewModel — Date Presets")
class ExpensesFilterPresetViewModelTest {

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
                firstArg<LocalDate>().toString()
            }
            every { formatShortDate(null as LocalDate?) } returns ""
        }

        every { authenticationService.currentUserId() } returns "user-1"
        every {
            userUiMapper.mapToDisplayName(
                user = any(),
                fallbackUserId = any(),
                currentUserId = any(),
                youLabel = any(),
                selfIdentificationContext = any(),
                gender = any()
            )
        } returns "User"
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()
        every { observeGroupUseCase(testGroupId) } returns flowOf(
            Group(id = testGroupId, name = "Trip", members = listOf("user-1", "user-2"))
        )
        every { getGroupExpensesFlowUseCase(testGroupId) } returns flowOf(emptyList())

        expenseFilterService = ExpenseFilterServiceImpl(expenseSearchService = ExpenseSearchServiceImpl())
        expensesFilterUiMapper = ExpensesFilterUiMapper(formattingHelper, userUiMapper)

        viewModel = ExpensesFilterViewModel(
            getGroupExpensesFlowUseCase = getGroupExpensesFlowUseCase,
            expenseFilterService = expenseFilterService,
            observeGroupUseCase = observeGroupUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            authenticationService = authenticationService,
            expensesFilterUiMapper = expensesFilterUiMapper,
            defaultDispatcher = testDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `DatePresetSelected applies preset date range when different preset selected`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        viewModel.setSelectedGroup(testGroupId)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(DateRangePreset.TODAY))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(today, state.draftCriteria.startDate)
        assertEquals(today, state.draftCriteria.endDate)
        assertEquals(DateRangePreset.TODAY, state.activePreset)

        collectJob.cancel()
    }

    @Test
    fun `DatePresetSelected clears date range when active preset is tapped again`() = runTest(testDispatcher) {
        viewModel.setSelectedGroup(testGroupId)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(DateRangePreset.TODAY))
        advanceUntilIdle()
        assertEquals(DateRangePreset.TODAY, viewModel.uiState.value.activePreset)

        viewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(DateRangePreset.TODAY))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.draftCriteria.startDate)
        assertNull(state.draftCriteria.endDate)
        assertNull(state.activePreset)

        collectJob.cancel()
    }

    @Test
    fun `DatePresetSelected switches date range when another preset is selected`() = runTest(testDispatcher) {
        val today = LocalDate.now()
        viewModel.setSelectedGroup(testGroupId)

        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(DateRangePreset.TODAY))
        advanceUntilIdle()
        assertEquals(DateRangePreset.TODAY, viewModel.uiState.value.activePreset)

        viewModel.onEvent(ExpensesFilterUiEvent.DatePresetSelected(DateRangePreset.YESTERDAY))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val yesterday = today.minusDays(1)
        assertEquals(yesterday, state.draftCriteria.startDate)
        assertEquals(yesterday, state.draftCriteria.endDate)
        assertEquals(DateRangePreset.YESTERDAY, state.activePreset)

        collectJob.cancel()
    }
}
