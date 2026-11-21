package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpendingInsightsServiceImplTest {

    private lateinit var service: SpendingInsightsServiceImpl

    private val today = LocalDate.of(2026, 11, 20)

    @BeforeEach
    fun setUp() {
        service = SpendingInsightsServiceImpl(
            addOnCalculationService = AddOnCalculationServiceImpl(),
            remainderDistributionService = RemainderDistributionServiceImpl()
        )
    }

    @Test
    fun `empty expense list returns zeroed insights with hasAnyGroupExpenses false`() {
        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = emptyList(),
            subunits = emptyList(),
            groupMemberIds = listOf("u1", "u2"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertFalse(result.hasAnyGroupExpenses)
        assertNull(result.oldestExpenseDate)
        assertNull(result.newestExpenseDate)
        assertEquals(0L, result.totalSpentCents)
        assertEquals(0L, result.totalBaseSpentCents)
        assertEquals(0L, result.totalExtrasCents)
        assertEquals(0, result.expenseCount)
        assertEquals(0, result.burnRate.activeDaysCount)
        assertEquals(0L, result.burnRate.averageDailySpendCents)
        assertNull(result.burnRate.projectedMonthlySpendCents)
        assertTrue(result.burnRate.dailySeries.isEmpty())
        assertTrue(result.categoryBreakdown.isEmpty())
        assertEquals(2, result.memberBreakdown.size)
        assertEquals(0L, result.memberBreakdown[0].effectiveSpentCents)
        assertEquals(BreakdownPercentage.Zero, result.memberBreakdown[0].breakdownPercentage)
    }

    @Test
    fun `excludes cancelled and future scheduled expenses while including due scheduled and refundable`() {
        val expenses = listOf(
            createExpense(
                id = "finished",
                groupAmount = 1000L,
                paymentStatus = PaymentStatus.FINISHED,
                date = today.minusDays(2)
            ),
            createExpense(
                id = "cancelled",
                groupAmount = 5000L,
                paymentStatus = PaymentStatus.CANCELLED,
                date = today.minusDays(1)
            ),
            createExpense(
                id = "future-scheduled",
                groupAmount = 4000L,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = today.plusDays(3).atStartOfDay(),
                date = today.minusDays(1)
            ),
            createExpense(
                id = "due-scheduled",
                groupAmount = 2000L,
                paymentStatus = PaymentStatus.SCHEDULED,
                dueDate = today.atStartOfDay(),
                date = today
            ),
            createExpense(
                id = "refundable",
                groupAmount = 3000L,
                paymentStatus = PaymentStatus.REFUNDABLE,
                dueDate = today.plusDays(5).atStartOfDay(),
                date = today.minusDays(1)
            )
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertTrue(result.hasAnyGroupExpenses)
        assertEquals(3, result.expenseCount)
        assertEquals(6000L, result.totalSpentCents)
    }

    @Test
    fun `effective group amount incorporates on-top and included add-ons and excludes discounts`() {
        val expense = createExpense(
            id = "e1",
            groupAmount = 10000L,
            date = today,
            addOns = listOf(
                AddOn(id = "a1", type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 500L),
                AddOn(id = "a2", type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 1000L),
                AddOn(id = "a3", type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, groupAmountCents = 300L),
                AddOn(id = "a4", type = AddOnType.DISCOUNT, mode = AddOnMode.INCLUDED, groupAmountCents = 200L)
            )
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = listOf(expense),
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        // effective = 10000 + 500 + 1000 - 300 = 11200
        // extras = non-discount add-ons = 500 + 1000 = 1500
        assertEquals(11200L, result.totalSpentCents)
        assertEquals(10000L, result.totalBaseSpentCents)
        assertEquals(1500L, result.totalExtrasCents)
    }

    @Test
    fun `category breakdown aggregates by category sorted descending with percentages summing to 100`() {
        val expenses = listOf(
            createExpense(id = "e1", groupAmount = 1000L, category = ExpenseCategory.FOOD, date = today),
            createExpense(id = "e2", groupAmount = 2000L, category = ExpenseCategory.FOOD, date = today),
            createExpense(id = "e3", groupAmount = 5000L, category = ExpenseCategory.LODGING, date = today),
            createExpense(id = "e4", groupAmount = 1000L, category = ExpenseCategory.TRANSPORT, date = today)
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(3, result.categoryBreakdown.size)
        assertEquals(ExpenseCategory.LODGING, result.categoryBreakdown[0].category)
        assertEquals(5000L, result.categoryBreakdown[0].totalAmountCents)
        assertEquals(1, result.categoryBreakdown[0].expenseCount)
        assertEquals(ExpenseCategory.FOOD, result.categoryBreakdown[1].category)
        assertEquals(3000L, result.categoryBreakdown[1].totalAmountCents)
        assertEquals(2, result.categoryBreakdown[1].expenseCount)
        assertEquals(ExpenseCategory.TRANSPORT, result.categoryBreakdown[2].category)
        assertEquals(1000L, result.categoryBreakdown[2].totalAmountCents)

        val sumPct = result.categoryBreakdown.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.percentage) }
        assertEquals(0, BigDecimal("100.00").compareTo(sumPct))
    }

    @Test
    fun `period filtering for ThisMonth includes only current month expenses and sets elapsedDays to dayOfMonth`() {
        val expenses = listOf(
            createExpense(id = "prev", groupAmount = 9000L, date = LocalDate.of(2026, 10, 28)),
            createExpense(id = "curr1", groupAmount = 2000L, date = LocalDate.of(2026, 11, 2)),
            createExpense(id = "curr2", groupAmount = 4000L, date = LocalDate.of(2026, 11, 20))
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.ThisMonth,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(6000L, result.totalSpentCents)
        assertEquals(2, result.expenseCount)
        assertEquals(20, result.burnRate.elapsedDays)
        assertEquals(30, result.burnRate.totalWindowDays)
        assertEquals(LocalDate.of(2026, 11, 1), result.burnRate.windowStartDate)
        assertEquals(LocalDate.of(2026, 11, 30), result.burnRate.windowEndDate)
    }

    @Test
    fun `period filtering for LastMonth includes previous month expenses and sets monthly pace to total`() {
        val expenses = listOf(
            createExpense(id = "oct1", groupAmount = 3500L, date = LocalDate.of(2026, 10, 5)),
            createExpense(id = "oct2", groupAmount = 1500L, date = LocalDate.of(2026, 10, 31)),
            createExpense(id = "nov1", groupAmount = 8000L, date = LocalDate.of(2026, 11, 1))
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.LastMonth,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(5000L, result.totalSpentCents)
        assertEquals(2, result.expenseCount)
        assertEquals(31, result.burnRate.elapsedDays)
        assertEquals(31, result.burnRate.totalWindowDays)
        assertEquals(5000L, result.burnRate.projectedMonthlySpendCents)
    }

    @Test
    fun `period filtering for CustomRange normalizes inverted start and end dates and filters inclusively`() {
        val expenses = listOf(
            createExpense(id = "before", groupAmount = 1000L, date = LocalDate.of(2026, 11, 9)),
            createExpense(id = "start", groupAmount = 2000L, date = LocalDate.of(2026, 11, 10)),
            createExpense(id = "mid", groupAmount = 3000L, date = LocalDate.of(2026, 11, 12)),
            createExpense(id = "end", groupAmount = 4000L, date = LocalDate.of(2026, 11, 15)),
            createExpense(id = "after", groupAmount = 5000L, date = LocalDate.of(2026, 11, 16))
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.CustomRange(
                startDate = LocalDate.of(2026, 11, 15),
                endDate = LocalDate.of(2026, 11, 10)
            ),
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(9000L, result.totalSpentCents)
        assertEquals(3, result.expenseCount)
        assertEquals(LocalDate.of(2026, 11, 10), result.burnRate.windowStartDate)
        assertEquals(LocalDate.of(2026, 11, 15), result.burnRate.windowEndDate)
        assertEquals(6, result.burnRate.elapsedDays)
    }

    @Test
    fun `period filtering for AllTime includes undated expenses in totals while omitting them from daily series`() {
        val expenses = listOf(
            createExpense(id = "dated", groupAmount = 4000L, date = LocalDate.of(2026, 11, 10)),
            Expense(
                id = "undated",
                groupId = "group-1",
                groupAmount = 2500L,
                category = ExpenseCategory.FOOD,
                paymentStatus = PaymentStatus.FINISHED,
                operationDate = null,
                createdAt = null
            )
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(6500L, result.totalSpentCents)
        assertEquals(2, result.expenseCount)
        assertEquals(1, result.burnRate.dailySeries.size)
        assertEquals(LocalDate.of(2026, 11, 10), result.burnRate.dailySeries[0].date)
    }

    @Test
    fun `burn rate uses BigDecimal HALF_UP daily average and suppresses monthly pace under 7 days`() {
        val expenses = listOf(
            createExpense(id = "d1", groupAmount = 1000L, date = LocalDate.of(2026, 11, 10)),
            createExpense(id = "d2", groupAmount = 500L, date = LocalDate.of(2026, 11, 12))
        )

        // Window Nov 10..Nov 12 = 3 days, total = 1500 -> avg = 500/day, active = 2 -> avg/active = 750
        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(3, result.burnRate.elapsedDays)
        assertEquals(2, result.burnRate.activeDaysCount)
        assertEquals(500L, result.burnRate.averageDailySpendCents)
        assertEquals(750L, result.burnRate.averagePerActiveDayCents)
        assertNull(result.burnRate.projectedMonthlySpendCents)
    }

    @Test
    fun `burn rate calculates projected monthly pace for ThisMonth and 30-day pace for multi-week AllTime windows`() {
        val thisMonthExpenses = listOf(
            createExpense(id = "m1", groupAmount = 2000L, date = LocalDate.of(2026, 11, 1)),
            createExpense(id = "m2", groupAmount = 2000L, date = LocalDate.of(2026, 11, 20))
        )
        // ThisMonth: total = 4000, elapsed = 20, totalDays = 30 -> 4000 * 30 / 20 = 6000
        val thisMonthResult = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = thisMonthExpenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.ThisMonth,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertEquals(6000L, thisMonthResult.burnRate.projectedMonthlySpendCents)

        val multiWeekExpenses = listOf(
            createExpense(id = "w1", groupAmount = 1400L, date = LocalDate.of(2026, 11, 1)),
            createExpense(id = "w2", groupAmount = 1400L, date = LocalDate.of(2026, 11, 14))
        )
        // AllTime: Nov 1..Nov 14 = 14 days, total = 2800 -> 2800 * 30 / 14 = 6000
        val allTimeResult = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = multiWeekExpenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertEquals(6000L, allTimeResult.burnRate.projectedMonthlySpendCents)
    }

    @Test
    fun `daily series fills zero-spend days for windows up to 31 days and uses active days for spans over 31 days`() {
        val compactExpenses = listOf(
            createExpense(id = "c1", groupAmount = 1000L, date = LocalDate.of(2026, 11, 1)),
            createExpense(id = "c2", groupAmount = 2000L, date = LocalDate.of(2026, 11, 5))
        )
        val compactResult = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = compactExpenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertEquals(5, compactResult.burnRate.dailySeries.size)
        assertEquals(0L, compactResult.burnRate.dailySeries[1].amountCents)
        assertEquals(0, BigDecimal.ZERO.compareTo(compactResult.burnRate.dailySeries[1].heightRatio))
        assertEquals(0, BigDecimal("1.0000").compareTo(compactResult.burnRate.dailySeries[4].heightRatio))

        val longSpanExpenses = listOf(
            createExpense(id = "l1", groupAmount = 1000L, date = LocalDate.of(2026, 9, 1)),
            createExpense(id = "l2", groupAmount = 2000L, date = LocalDate.of(2026, 11, 15))
        )
        val longSpanResult = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = longSpanExpenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertEquals(2, longSpanResult.burnRate.dailySeries.size)
    }

    @Test
    fun `peak day selects highest spending date and breaks ties by most recent date`() {
        val expenses = listOf(
            createExpense(id = "p1", groupAmount = 3000L, date = LocalDate.of(2026, 11, 10)),
            createExpense(id = "p2", groupAmount = 1500L, date = LocalDate.of(2026, 11, 14)),
            createExpense(id = "p3", groupAmount = 1500L, date = LocalDate.of(2026, 11, 14)),
            createExpense(id = "p4", groupAmount = 1000L, date = LocalDate.of(2026, 11, 15))
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = expenses,
            subunits = emptyList(),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        assertEquals(LocalDate.of(2026, 11, 14), result.burnRate.peakDayDate)
        assertEquals(3000L, result.burnRate.peakDayAmountCents)
    }

    @Test
    fun `member breakdown rescales foreign-currency and add-on splits with exact cent conservation`() {
        // Source splits sum to 10000 JPY, while effective group EUR amount is 1000 + 25 = 1025 cents
        val expense = Expense(
            id = "fx-1",
            groupId = "group-1",
            sourceAmount = 10000L,
            sourceCurrency = "JPY",
            groupAmount = 1000L,
            groupCurrency = "EUR",
            addOns = listOf(
                AddOn(id = "fee", type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 25L)
            ),
            splits = listOf(
                ExpenseSplit(userId = "u1", amountCents = 3334L),
                ExpenseSplit(userId = "u2", amountCents = 3333L),
                ExpenseSplit(userId = "u3", amountCents = 3333L),
                ExpenseSplit(userId = "u4", amountCents = 9999L, isExcluded = true)
            ),
            paymentStatus = PaymentStatus.FINISHED,
            operationDate = today.atStartOfDay()
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = listOf(expense),
            subunits = emptyList(),
            groupMemberIds = listOf("u1", "u2", "u3", "u4"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )

        val attributedSum = result.memberBreakdown.sumOf { it.effectiveSpentCents }
        assertEquals(1025L, attributedSum)
        val u4 = result.memberBreakdown.first { it.userId == "u4" }
        assertEquals(0L, u4.effectiveSpentCents)
        assertEquals(0, u4.expenseCount)
    }

    @Test
    fun `entity breakdown groups subunit members into SUBUNIT entities and leaves unassigned members as SOLO_MEMBER`() {
        val expense = createExpense(
            id = "e1",
            groupAmount = 10000L,
            date = today,
            splits = listOf(
                ExpenseSplit(userId = "u1", amountCents = 3000L),
                ExpenseSplit(userId = "u2", amountCents = 3000L),
                ExpenseSplit(userId = "u3", amountCents = 4000L)
            )
        )
        val subunit = Subunit(
            id = "sub-1",
            groupId = "group-1",
            name = "Alex & Sam",
            memberIds = listOf("u1", "u2")
        )
        val profiles = mapOf(
            "u1" to User(userId = "u1", email = "u1@test.com", displayName = "Alex"),
            "u2" to User(userId = "u2", email = "u2@test.com", displayName = "Sam"),
            "u3" to User(userId = "u3", email = "u3@test.com", displayName = "Carlos")
        )

        val result = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = listOf(expense),
            subunits = listOf(subunit),
            groupMemberIds = listOf("u1", "u2", "u3"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = profiles,
            currentUserId = "u1",
            today = today
        )

        assertTrue(result.hasSubunits)
        assertEquals(2, result.entityBreakdown.size)
        val subEntity = result.entityBreakdown[0]
        assertEquals("sub-1", subEntity.entityId)
        assertEquals(EntitySpendingType.SUBUNIT, subEntity.type)
        assertEquals("Alex & Sam", subEntity.subunitName)
        assertEquals(6000L, subEntity.effectiveSpentCents)
        assertEquals(1, subEntity.expenseCount)

        val soloEntity = result.entityBreakdown[1]
        assertEquals("u3", soloEntity.entityId)
        assertEquals(EntitySpendingType.SOLO_MEMBER, soloEntity.type)
        assertEquals(4000L, soloEntity.effectiveSpentCents)
    }

    @Test
    fun `hasSubunits is false when no subunits exist and true when non-empty subunits exist`() {
        val expense = createExpense(id = "e1", groupAmount = 1000L, date = today)

        val withoutSubunits = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = listOf(expense),
            subunits = listOf(Subunit(id = "empty-sub", memberIds = emptyList())),
            groupMemberIds = listOf("u1"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertFalse(withoutSubunits.hasSubunits)

        val withSubunits = service.calculate(
            groupId = "group-1",
            currency = "EUR",
            expenses = listOf(expense),
            subunits = listOf(Subunit(id = "sub-1", name = "Duo", memberIds = listOf("u1", "u2"))),
            groupMemberIds = listOf("u1", "u2"),
            period = SpendingInsightsPeriod.AllTime,
            memberProfiles = emptyMap(),
            currentUserId = "u1",
            today = today
        )
        assertTrue(withSubunits.hasSubunits)
    }

    private fun createExpense(
        id: String,
        groupAmount: Long,
        category: ExpenseCategory = ExpenseCategory.FOOD,
        paymentStatus: PaymentStatus = PaymentStatus.FINISHED,
        date: LocalDate = today,
        dueDate: LocalDateTime? = null,
        addOns: List<AddOn> = emptyList(),
        splits: List<ExpenseSplit> = listOf(ExpenseSplit(userId = "u1", amountCents = groupAmount))
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        sourceAmount = groupAmount,
        groupAmount = groupAmount,
        category = category,
        paymentStatus = paymentStatus,
        operationDate = date.atTime(12, 0),
        dueDate = dueDate,
        addOns = addOns,
        splits = splits
    )
}
