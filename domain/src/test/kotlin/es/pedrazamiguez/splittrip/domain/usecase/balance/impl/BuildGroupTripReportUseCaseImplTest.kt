package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.model.GroupDashboardReadModel
import es.pedrazamiguez.splittrip.domain.model.GroupPocketBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.impl.AddOnCalculationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.ExpenseCalculatorServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BuildGroupTripReportUseCaseImplTest {

    private lateinit var groupRepository: GroupRepository
    private lateinit var groupDashboardDataSource: GroupDashboardDataSource
    private lateinit var getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase
    private lateinit var getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase
    private lateinit var getSettlementSuggestionsUseCase: GetSettlementSuggestionsUseCase
    private lateinit var getMemberProfilesUseCase: GetMemberProfilesUseCase
    private lateinit var addOnCalculationService: AddOnCalculationService
    private lateinit var authenticationService: AuthenticationService
    private lateinit var appConfigService: AppConfigService
    private lateinit var useCase: BuildGroupTripReportUseCaseImpl

    private val testGroup = Group(
        id = "group-1",
        name = "Shared Flat",
        currency = "EUR",
        members = listOf("u1", "u2")
    )

    private val fixedToday = LocalDate.of(2026, 11, 20)
    private val fixedGeneratedAt = LocalDateTime.of(2026, 11, 20, 14, 30)

    @BeforeEach
    fun setUp() {
        groupRepository = mockk()
        groupDashboardDataSource = mockk()
        getGroupPocketBalanceFlowUseCase = mockk()
        getMemberBalancesFlowUseCase = mockk()
        getSettlementSuggestionsUseCase = mockk()
        getMemberProfilesUseCase = mockk()
        addOnCalculationService = AddOnCalculationServiceImpl()
        authenticationService = mockk()
        appConfigService = mockk {
            every { defaultCurrencyCode } returns MutableStateFlow("EUR")
        }

        every { authenticationService.currentUserId() } returns "u1"
        coEvery { groupRepository.getGroupById("group-1") } returns testGroup
        coEvery { getMemberProfilesUseCase(any()) } returns mapOf(
            "u1" to User(userId = "u1", email = "alice@test.com", displayName = "Alice"),
            "u2" to User(userId = "u2", email = "bob@test.com", displayName = "Bob")
        )

        useCase = BuildGroupTripReportUseCaseImpl(
            groupRepository = groupRepository,
            groupDashboardDataSource = groupDashboardDataSource,
            getGroupPocketBalanceFlowUseCase = getGroupPocketBalanceFlowUseCase,
            getMemberBalancesFlowUseCase = getMemberBalancesFlowUseCase,
            getSettlementSuggestionsUseCase = getSettlementSuggestionsUseCase,
            getMemberProfilesUseCase = getMemberProfilesUseCase,
            addOnCalculationService = addOnCalculationService,
            expenseCalculatorService = ExpenseCalculatorServiceImpl(),
            authenticationService = authenticationService,
            appConfigService = appConfigService
        )
    }

    @Test
    fun `invoke with AllTime aggregates full ledger and settlements with BigDecimal amounts`() =
        runTest {
            val snapshot = createFullSnapshot()
            val memberBalances = createSampleMemberBalances()
            val suggestedRecord = createSuggestedSettlement()

            every { groupDashboardDataSource.getDashboardSnapshotFlow("group-1") } returns flowOf(snapshot)
            every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
                GroupPocketBalance(
                    totalContributions = 20000L,
                    totalExpenses = 1950L,
                    virtualBalance = 15755L,
                    currency = "EUR",
                    totalCashEquivalent = 1701L,
                    totalExtras = 695L
                )
            every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns memberBalances
            every {
                getSettlementSuggestionsUseCase.getEphemeralSettlements(
                    groupId = "group-1",
                    memberBalances = memberBalances,
                    groupCurrency = "EUR",
                    persistedRecords = any()
                )
            } returns listOf(suggestedRecord)

            val report = useCase(
                groupId = "group-1",
                period = SpendingInsightsPeriod.AllTime,
                today = fixedToday,
                generatedAt = fixedGeneratedAt
            )

            assertEquals(BigDecimal("200.00"), report.totalContributedAmount)
            assertEquals(BigDecimal("19.50"), report.totalExpensesAmount)
            assertEquals(BigDecimal("157.55"), report.virtualPocketBalanceAmount)
            assertEquals(BigDecimal("17.01"), report.totalCashEquivalentAmount)
            assertEquals(BigDecimal("6.95"), report.totalExtrasAmount)
            assertEquals(LocalDate.of(2026, 11, 1), report.windowStartDate)
            assertEquals(LocalDate.of(2026, 11, 10), report.windowEndDate)

            assertEquals(listOf("exp-jpy", "exp-thb"), report.expenses.map { it.expense.id })
            assertEquals(BigDecimal("1500"), report.expenses[0].sourceAmount)
            assertEquals(BigDecimal("9.50"), report.expenses[0].baseGroupAmount)
            assertEquals(BigDecimal("370.00"), report.expenses[1].sourceAmount)
            assertEquals(BigDecimal("1.00"), report.expenses[1].addOnExtrasAmount)
            assertEquals(BigDecimal("11.00"), report.expenses[1].effectiveGroupAmount)

            val withdrawalItem = report.withdrawals.single()
            assertEquals(BigDecimal("5.95"), withdrawalItem.addOnFeesAmount)
            assertEquals(BigDecimal("32.95"), withdrawalItem.effectiveDeductedBaseAmount)

            val subunitItem = report.subunitBalances.single()
            assertEquals(BigDecimal("200.00"), subunitItem.contributedAmount)
            assertEquals(BigDecimal("173.55"), subunitItem.netBalanceAmount)

            assertEquals(2, report.settlements.size)
            assertTrue(report.settlements.any { it.record.id == "set-suggested" && it.amount == BigDecimal("12.50") })
            assertTrue(report.settlements.any { it.record.id == "set-resolved" && it.amount == BigDecimal("45.00") })
        }

    @Test
    fun `invoke excludes CANCELLED expenses from report ledger`() = runTest {
        val activeExpense = createExpense("exp-active", "Groceries", 2500L, LocalDateTime.of(2026, 11, 12, 10, 0))
        val cancelledExpense = Expense(
            id = "exp-cancelled",
            groupId = "group-1",
            title = "Cancelled booking",
            sourceAmount = 15000L,
            sourceCurrency = "EUR",
            groupAmount = 15000L,
            groupCurrency = "EUR",
            paymentStatus = PaymentStatus.CANCELLED,
            operationDate = LocalDateTime.of(2026, 11, 13, 10, 0)
        )

        stubSnapshotWithEmptyAggregates(
            GroupDashboardReadModel(
                group = testGroup,
                contributions = emptyList(),
                withdrawals = emptyList(),
                subunits = emptyList(),
                expenses = listOf(activeExpense, cancelledExpense),
                settlements = emptyList()
            )
        )

        val report = useCase("group-1", SpendingInsightsPeriod.AllTime, fixedToday, fixedGeneratedAt)

        assertEquals(listOf("exp-active"), report.expenses.map { it.expense.id })
    }

    @Test
    fun `invoke with ThisMonth filters transactions to current calendar month window`() = runTest {
        val snapshot = GroupDashboardReadModel(
            group = testGroup,
            contributions = listOf(
                createContribution("contrib-oct", 50000L, LocalDateTime.of(2026, 10, 15, 9, 0)),
                createContribution("contrib-nov", 10000L, LocalDateTime.of(2026, 11, 20, 9, 0))
            ),
            withdrawals = listOf(
                createSimpleWithdrawal("wd-oct", 5000L, LocalDateTime.of(2026, 10, 20, 15, 0)),
                createSimpleWithdrawal("wd-nov", 2000L, LocalDateTime.of(2026, 11, 14, 15, 0))
            ),
            subunits = emptyList(),
            expenses = listOf(
                createExpense("exp-oct", "October rent", 50000L, LocalDateTime.of(2026, 10, 31, 12, 0)),
                createExpense("exp-nov", "November internet", 3500L, LocalDateTime.of(2026, 11, 1, 9, 0))
            ),
            settlements = emptyList()
        )
        stubSnapshotWithEmptyAggregates(snapshot)

        val report = useCase("group-1", SpendingInsightsPeriod.ThisMonth, fixedToday, fixedGeneratedAt)

        assertEquals(LocalDate.of(2026, 11, 1), report.windowStartDate)
        assertEquals(LocalDate.of(2026, 11, 30), report.windowEndDate)
        assertEquals(listOf("exp-nov"), report.expenses.map { it.expense.id })
        assertEquals(listOf("contrib-nov"), report.contributions.map { it.contribution.id })
        assertEquals(listOf("wd-nov"), report.withdrawals.map { it.withdrawal.id })
    }

    @Test
    fun `invoke with CustomRange filters transactions to inclusive start and end dates`() = runTest {
        val snapshot = GroupDashboardReadModel(
            group = testGroup,
            contributions = emptyList(),
            withdrawals = emptyList(),
            subunits = emptyList(),
            expenses = listOf(
                createExpense("exp-before", "Before", 1000L, LocalDateTime.of(2026, 11, 4, 23, 59)),
                createExpense("exp-start", "On start", 2000L, LocalDateTime.of(2026, 11, 5, 0, 0)),
                createExpense("exp-end", "On end", 3000L, LocalDateTime.of(2026, 11, 10, 23, 59)),
                createExpense("exp-after", "After", 4000L, LocalDateTime.of(2026, 11, 11, 0, 0))
            ),
            settlements = emptyList()
        )
        stubSnapshotWithEmptyAggregates(snapshot)

        val report = useCase(
            groupId = "group-1",
            period = SpendingInsightsPeriod.CustomRange(
                startDate = LocalDate.of(2026, 11, 10),
                endDate = LocalDate.of(2026, 11, 5)
            ),
            today = fixedToday,
            generatedAt = fixedGeneratedAt
        )

        assertEquals(LocalDate.of(2026, 11, 5), report.windowStartDate)
        assertEquals(LocalDate.of(2026, 11, 10), report.windowEndDate)
        assertEquals(listOf("exp-end", "exp-start"), report.expenses.map { it.expense.id })
    }

    @Test
    fun `invoke throws IllegalArgumentException when group does not exist`() = runTest {
        coEvery { groupRepository.getGroupById("missing-group") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                useCase("missing-group")
            }
        }
    }

    private fun stubSnapshotWithEmptyAggregates(snapshot: GroupDashboardReadModel) {
        every { groupDashboardDataSource.getDashboardSnapshotFlow("group-1") } returns flowOf(snapshot)
        every { getGroupPocketBalanceFlowUseCase.calculate(any(), any(), any(), "EUR") } returns
            GroupPocketBalance()
        every { getMemberBalancesFlowUseCase.computeMemberBalances(any()) } returns emptyList()
        every {
            getSettlementSuggestionsUseCase.getEphemeralSettlements(
                groupId = any(),
                memberBalances = any(),
                groupCurrency = any(),
                persistedRecords = any()
            )
        } returns emptyList()
    }

    private fun createFullSnapshot(): GroupDashboardReadModel = GroupDashboardReadModel(
        group = testGroup,
        contributions = listOf(createContribution("contrib-1", 20000L, LocalDateTime.of(2026, 11, 1, 10, 0))),
        withdrawals = listOf(createWithdrawalWithFee()),
        subunits = listOf(
            Subunit(id = "sub-1", groupId = "group-1", name = "Team A", memberIds = listOf("u1", "u2"))
        ),
        expenses = listOf(createThbExpense(), createJpyExpense()),
        settlements = listOf(createResolvedSettlement())
    )

    private fun createThbExpense(): Expense = Expense(
        id = "exp-thb",
        groupId = "group-1",
        title = "Street food",
        sourceAmount = 37000L,
        sourceCurrency = "THB",
        groupAmount = 1000L,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal("0.027027"),
        addOns = listOf(
            AddOn(
                id = "ao-1",
                type = AddOnType.TIP,
                valueType = AddOnValueType.EXACT,
                amountCents = 3700L,
                currency = "THB",
                groupAmountCents = 100L
            )
        ),
        paymentMethod = PaymentMethod.CASH,
        paymentStatus = PaymentStatus.FINISHED,
        operationDate = LocalDateTime.of(2026, 11, 5, 19, 0),
        createdBy = "u1"
    )

    private fun createJpyExpense(): Expense = Expense(
        id = "exp-jpy",
        groupId = "group-1",
        title = "Metro pass",
        sourceAmount = 1500L,
        sourceCurrency = "JPY",
        groupAmount = 950L,
        groupCurrency = "EUR",
        exchangeRate = BigDecimal("0.006333"),
        paymentStatus = PaymentStatus.FINISHED,
        operationDate = LocalDateTime.of(2026, 11, 10, 9, 0),
        createdBy = "u2"
    )

    private fun createWithdrawalWithFee(): CashWithdrawal = CashWithdrawal(
        id = "wd-1",
        groupId = "group-1",
        withdrawnBy = "u1",
        createdBy = "u1",
        amountWithdrawn = 100000L,
        remainingAmount = 63000L,
        currency = "THB",
        deductedBaseAmount = 2700L,
        addOns = listOf(
            AddOn(
                id = "wd-fee",
                type = AddOnType.FEE,
                valueType = AddOnValueType.EXACT,
                amountCents = 22000L,
                currency = "THB",
                groupAmountCents = 595L
            )
        ),
        createdAt = LocalDateTime.of(2026, 11, 2, 12, 0)
    )

    private fun createResolvedSettlement(): SettlementRecord = SettlementRecord(
        id = "set-resolved",
        groupId = "group-1",
        settlement = Settlement(
            fromUserId = "u2",
            toUserId = "u1",
            amount = 4500L,
            currency = "EUR",
            sourcePocket = SettlementPocketType.NET
        ),
        status = SettlementStatus.RESOLVED,
        createdAt = LocalDateTime.of(2026, 11, 15, 11, 0)
    )

    private fun createSampleMemberBalances(): List<MemberBalance> = listOf(
        MemberBalance(
            userId = "u1",
            contributed = 20000L,
            withdrawn = 3295L,
            cashSpent = 1100L,
            nonCashSpent = 475L,
            totalSpent = 1575L,
            pocketBalance = 16230L,
            cashInHand = 1600L
        ),
        MemberBalance(
            userId = "u2",
            contributed = 0L,
            withdrawn = 0L,
            cashSpent = 0L,
            nonCashSpent = 475L,
            totalSpent = 475L,
            pocketBalance = -475L,
            cashInHand = 0L
        )
    )

    private fun createSuggestedSettlement(): SettlementRecord = SettlementRecord(
        id = "set-suggested",
        groupId = "group-1",
        settlement = Settlement(
            fromUserId = "u2",
            toUserId = "u1",
            amount = 1250L,
            currency = "EUR",
            sourcePocket = SettlementPocketType.POCKET
        ),
        status = SettlementStatus.SUGGESTED,
        createdAt = LocalDateTime.of(2026, 11, 20, 12, 0)
    )

    private fun createExpense(
        id: String,
        title: String,
        amountCents: Long,
        date: LocalDateTime
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        title = title,
        sourceAmount = amountCents,
        sourceCurrency = "EUR",
        groupAmount = amountCents,
        groupCurrency = "EUR",
        paymentStatus = PaymentStatus.FINISHED,
        operationDate = date
    )

    private fun createContribution(
        id: String,
        amountCents: Long,
        date: LocalDateTime
    ): Contribution = Contribution(
        id = id,
        groupId = "group-1",
        userId = "u1",
        createdBy = "u1",
        amount = amountCents,
        currency = "EUR",
        equivalentBaseAmount = amountCents,
        contributionDate = date
    )

    private fun createSimpleWithdrawal(
        id: String,
        amountCents: Long,
        date: LocalDateTime
    ): CashWithdrawal = CashWithdrawal(
        id = id,
        groupId = "group-1",
        withdrawnBy = "u1",
        amountWithdrawn = amountCents,
        currency = "EUR",
        deductedBaseAmount = amountCents,
        createdAt = date
    )
}
