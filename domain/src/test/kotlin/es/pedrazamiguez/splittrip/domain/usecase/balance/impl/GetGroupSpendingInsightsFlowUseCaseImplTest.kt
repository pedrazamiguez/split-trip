package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingBurnRateInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ExpenseFilterService
import es.pedrazamiguez.splittrip.domain.service.SpendingInsightsService
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSpendingInsightsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetGroupSpendingInsightsFlowUseCaseImplTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var groupRepository: GroupRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var subunitRepository: SubunitRepository
    private lateinit var cashWithdrawalRepository: CashWithdrawalRepository
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var spendingInsightsService: SpendingInsightsService
    private lateinit var expenseFilterService: ExpenseFilterService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigService: AppConfigService

    private lateinit var useCase: GetGroupSpendingInsightsFlowUseCaseImpl

    private val fixedToday = LocalDate.of(2026, 11, 20)

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        expenseRepository = mockk()
        subunitRepository = mockk()
        cashWithdrawalRepository = mockk()
        getMemberProfilesUseCase = mockk()
        spendingInsightsService = mockk()
        expenseFilterService = mockk()
        authenticationService = mockk()
        appConfigService = mockk {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
            every { balanceComputationDebounceMs } returns MutableStateFlow(300L)
        }

        every { authenticationService.currentUserId() } returns "u1"
        every { cashWithdrawalRepository.getGroupWithdrawalsFlow(any()) } returns flowOf(emptyList())
        every { expenseFilterService.filter(any(), any()) } answers { firstArg() }
        coEvery { getMemberProfilesUseCase(any()) } returns emptyMap()

        useCase = GetGroupSpendingInsightsFlowUseCaseImpl(
            groupRepository = groupRepository,
            expenseRepository = expenseRepository,
            subunitRepository = subunitRepository,
            cashWithdrawalRepository = cashWithdrawalRepository,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            spendingInsightsService = spendingInsightsService,
            expenseFilterService = expenseFilterService,
            authenticationService = authenticationService,
            appConfigService = appConfigService
        )
    }

    @Test
    fun `combines group expenses subunits and periodFlow and emits calculated insights`() = runTest(testDispatcher) {
        val group = Group(id = "g1", currency = "USD", members = listOf("u1", "u2"))
        val expenses = listOf(Expense(id = "e1", groupId = "g1", groupAmount = 2500L))
        val subunits = listOf(Subunit(id = "s1", groupId = "g1", memberIds = listOf("u1", "u2")))
        val expected = emptyInsights(groupId = "g1", currency = "USD", period = SpendingInsightsPeriod.AllTime)

        every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(group)
        every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(expenses)
        every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(subunits)
        every {
            spendingInsightsService.calculate(
                groupId = "g1",
                currency = "USD",
                expenses = expenses,
                subunits = subunits,
                groupMemberIds = listOf("u1", "u2"),
                period = SpendingInsightsPeriod.AllTime,
                memberProfiles = emptyMap(),
                currentUserId = "u1",
                today = fixedToday
            )
        } returns expected

        val emissions = mutableListOf<GroupSpendingInsights>()
        val job = launch {
            useCase("g1", flowOf(SpendingInsightsPeriod.AllTime)) { fixedToday }.collect { emissions.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, emissions.size)
        assertEquals(expected, emissions.first())
        job.cancel()
    }

    @Test
    fun `uses group currency when present and falls back to defaultCurrencyCode when group is null`() =
        runTest(testDispatcher) {
            every { appConfigService.defaultCurrencyCode } returns MutableStateFlow("GBP")
            every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(null)
            every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(emptyList())
            every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(emptyList())
            val expected = emptyInsights(groupId = "g1", currency = "GBP", period = SpendingInsightsPeriod.AllTime)
            every {
                spendingInsightsService.calculate(
                    groupId = "g1",
                    currency = "GBP",
                    expenses = emptyList(),
                    subunits = emptyList(),
                    groupMemberIds = emptyList(),
                    period = SpendingInsightsPeriod.AllTime,
                    memberProfiles = emptyMap(),
                    currentUserId = "u1",
                    today = fixedToday
                )
            } returns expected

            val emissions = mutableListOf<GroupSpendingInsights>()
            val job = launch {
                useCase("g1", flowOf(SpendingInsightsPeriod.AllTime)) { fixedToday }.collect { emissions.add(it) }
            }
            advanceUntilIdle()

            assertEquals("GBP", emissions.first().currency)
            job.cancel()
        }

    @Test
    fun `collects member profiles for group members subunit members and split participants`() =
        runTest(testDispatcher) {
            val group = Group(id = "g1", currency = "EUR", members = listOf("u1"))
            val subunits = listOf(Subunit(id = "s1", groupId = "g1", memberIds = listOf("u2")))
            val expenses = listOf(
                Expense(
                    id = "e1",
                    groupId = "g1",
                    groupAmount = 1000L,
                    splits = listOf(ExpenseSplit(userId = "u3", amountCents = 1000L))
                )
            )
            val profiles = mapOf("u1" to User(userId = "u1", email = "u1@test.com"))
            coEvery { getMemberProfilesUseCase(any()) } returns profiles
            every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(group)
            every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(expenses)
            every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(subunits)
            every {
                spendingInsightsService.calculate(
                    groupId = "g1",
                    currency = "EUR",
                    expenses = expenses,
                    subunits = subunits,
                    groupMemberIds = listOf("u1"),
                    period = SpendingInsightsPeriod.AllTime,
                    memberProfiles = profiles,
                    currentUserId = "u1",
                    today = fixedToday
                )
            } returns emptyInsights(groupId = "g1", currency = "EUR", period = SpendingInsightsPeriod.AllTime)

            val job = launch {
                useCase("g1", flowOf(SpendingInsightsPeriod.AllTime)) { fixedToday }.collect {}
            }
            advanceUntilIdle()

            coVerify(exactly = 1) {
                getMemberProfilesUseCase(match { it.toSet() == setOf("u1", "u2", "u3") })
            }
            job.cancel()
        }

    @Test
    fun `reacts immediately to periodFlow emissions without re-subscribing upstream repositories`() =
        runTest(testDispatcher) {
            val periodFlow = MutableStateFlow<SpendingInsightsPeriod>(SpendingInsightsPeriod.AllTime)
            every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(Group(id = "g1", currency = "EUR"))
            every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(emptyList())
            every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(emptyList())
            every {
                spendingInsightsService.calculate(
                    groupId = "g1",
                    currency = "EUR",
                    expenses = emptyList(),
                    subunits = emptyList(),
                    groupMemberIds = emptyList(),
                    period = any(),
                    memberProfiles = emptyMap(),
                    currentUserId = "u1",
                    today = fixedToday
                )
            } answers {
                val period = arg<SpendingInsightsPeriod>(5)
                emptyInsights(groupId = "g1", currency = "EUR", period = period)
            }

            val emissions = mutableListOf<GroupSpendingInsights>()
            val job = launch {
                useCase("g1", periodFlow) { fixedToday }.collect { emissions.add(it) }
            }
            advanceUntilIdle()
            assertEquals(1, emissions.size)

            periodFlow.value = SpendingInsightsPeriod.ThisMonth
            runCurrent()

            assertEquals(2, emissions.size)
            assertEquals(SpendingInsightsPeriod.ThisMonth, emissions[1].period)
            verify(exactly = 1) { groupRepository.getGroupByIdFlow("g1") }
            verify(exactly = 1) { expenseRepository.getGroupExpensesFlow("g1") }
            verify(exactly = 1) { subunitRepository.getGroupSubunitsFlow("g1") }
            job.cancel()
        }

    @Test
    fun `reacts offline-first when expenseRepository emits added edited or deleted expenses`() =
        runTest(testDispatcher) {
            val expensesFlow = MutableStateFlow(emptyList<Expense>())
            every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(Group(id = "g1", currency = "EUR"))
            every { expenseRepository.getGroupExpensesFlow("g1") } returns expensesFlow
            every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(emptyList())
            every {
                spendingInsightsService.calculate(
                    groupId = "g1",
                    currency = "EUR",
                    expenses = any(),
                    subunits = emptyList(),
                    groupMemberIds = emptyList(),
                    period = SpendingInsightsPeriod.AllTime,
                    memberProfiles = emptyMap(),
                    currentUserId = "u1",
                    today = fixedToday
                )
            } answers {
                val list = thirdArg<List<Expense>>()
                emptyInsights(
                    groupId = "g1",
                    currency = "EUR",
                    period = SpendingInsightsPeriod.AllTime,
                    totalSpentCents = list.sumOf { it.groupAmount },
                    expenseCount = list.size
                )
            }

            val emissions = mutableListOf<GroupSpendingInsights>()
            val job = launch {
                useCase("g1", flowOf(SpendingInsightsPeriod.AllTime)) { fixedToday }.collect { emissions.add(it) }
            }
            advanceUntilIdle()
            assertEquals(0L, emissions.last().totalSpentCents)

            expensesFlow.value = listOf(Expense(id = "e1", groupAmount = 1200L))
            advanceUntilIdle()
            assertEquals(1200L, emissions.last().totalSpentCents)

            expensesFlow.value = listOf(Expense(id = "e1", groupAmount = 1800L))
            advanceUntilIdle()
            assertEquals(1800L, emissions.last().totalSpentCents)

            expensesFlow.value = emptyList()
            advanceUntilIdle()
            assertEquals(0L, emissions.last().totalSpentCents)
            assertEquals(0, emissions.last().expenseCount)
            job.cancel()
        }

    @Test
    fun `interface invoke uses default todayProvider when omitted`() = runTest(testDispatcher) {
        val interfaceUseCase: GetGroupSpendingInsightsFlowUseCase = useCase
        val expected = emptyInsights(groupId = "g1", currency = "EUR", period = SpendingInsightsPeriod.AllTime)
        every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(Group(id = "g1", currency = "EUR"))
        every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(emptyList())
        every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(emptyList())
        every {
            spendingInsightsService.calculate(
                groupId = "g1",
                currency = "EUR",
                expenses = emptyList(),
                subunits = emptyList(),
                groupMemberIds = emptyList(),
                period = SpendingInsightsPeriod.AllTime,
                memberProfiles = emptyMap(),
                currentUserId = "u1",
                today = any()
            )
        } returns expected

        val emissions = mutableListOf<GroupSpendingInsights>()
        val job = launch {
            interfaceUseCase("g1", flowOf(SpendingInsightsPeriod.AllTime)).collect { emissions.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, emissions.size)
        job.cancel()
    }

    @Test
    fun `applies expenseFilterService and excludes withdrawals when filterCriteria is active`() =
        runTest(testDispatcher) {
            val e1 = Expense(id = "e1", groupId = "g1", groupAmount = 1000L, category = ExpenseCategory.FOOD)
            val e2 = Expense(id = "e2", groupId = "g1", groupAmount = 2000L, category = ExpenseCategory.TRANSPORT)
            val withdrawal = CashWithdrawal(id = "w1", groupId = "g1")
            val activeCriteria = ExpenseFilterCriteria(selectedCategories = setOf(ExpenseCategory.FOOD))
            val expected = emptyInsights(groupId = "g1", currency = "EUR", period = SpendingInsightsPeriod.AllTime)
                .copy(isFiltered = true)

            every { groupRepository.getGroupByIdFlow("g1") } returns flowOf(Group(id = "g1", currency = "EUR"))
            every { expenseRepository.getGroupExpensesFlow("g1") } returns flowOf(listOf(e1, e2))
            every { subunitRepository.getGroupSubunitsFlow("g1") } returns flowOf(emptyList())
            every { cashWithdrawalRepository.getGroupWithdrawalsFlow("g1") } returns flowOf(listOf(withdrawal))
            every { expenseFilterService.filter(listOf(e1, e2), activeCriteria) } returns listOf(e1)
            every {
                spendingInsightsService.calculate(
                    groupId = "g1",
                    currency = "EUR",
                    expenses = listOf(e1),
                    subunits = emptyList(),
                    groupMemberIds = emptyList(),
                    period = SpendingInsightsPeriod.AllTime,
                    memberProfiles = emptyMap(),
                    currentUserId = "u1",
                    today = fixedToday,
                    withdrawals = emptyList(),
                    isFiltered = true
                )
            } returns expected

            val emissions = mutableListOf<GroupSpendingInsights>()
            val job = launch {
                useCase(
                    groupId = "g1",
                    periodFlow = flowOf(SpendingInsightsPeriod.AllTime),
                    filterCriteriaFlow = flowOf(activeCriteria)
                ) { fixedToday }.collect { emissions.add(it) }
            }
            advanceUntilIdle()

            assertEquals(1, emissions.size)
            assertEquals(true, emissions.first().isFiltered)
            verify(exactly = 1) { expenseFilterService.filter(listOf(e1, e2), activeCriteria) }
            job.cancel()
        }

    private fun emptyInsights(
        groupId: String,
        currency: String,
        period: SpendingInsightsPeriod,
        totalSpentCents: Long = 0L,
        expenseCount: Int = 0
    ): GroupSpendingInsights = GroupSpendingInsights(
        groupId = groupId,
        currency = currency,
        period = period,
        hasAnyGroupExpenses = expenseCount > 0,
        oldestExpenseDate = null,
        newestExpenseDate = null,
        totalSpentCents = totalSpentCents,
        totalBaseSpentCents = totalSpentCents,
        totalExtrasCents = 0L,
        expenseCount = expenseCount,
        burnRate = SpendingBurnRateInsight(
            windowStartDate = null,
            windowEndDate = null,
            elapsedDays = 0,
            totalWindowDays = 0,
            activeDaysCount = 0,
            averageDailySpendCents = 0L,
            averagePerActiveDayCents = 0L,
            projectedMonthlySpendCents = null,
            peakDayDate = null,
            peakDayAmountCents = 0L,
            dailySeries = emptyList()
        ),
        categoryBreakdown = emptyList(),
        memberBreakdown = emptyList(),
        entityBreakdown = emptyList(),
        hasSubunits = false,
        memberProfiles = emptyMap(),
        currentUserId = "u1"
    )
}
