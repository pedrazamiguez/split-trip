package es.pedrazamiguez.splittrip.features.balance.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.R as CommonR
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.common.provider.ResourceProvider
import es.pedrazamiguez.splittrip.core.designsystem.R as DesignSystemR
import es.pedrazamiguez.splittrip.core.designsystem.foundation.ChartColors
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toIconVector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.FormattingHelper
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.CategorySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.DailySpendingPoint
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingType
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.MemberSpendingInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingBurnRateInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SpendingInsightsUiMapperTest {

    private class FakeLocaleProvider(var currentLocale: Locale = Locale.US) : LocaleProvider {
        override fun getCurrentLocale(): Locale = currentLocale
    }

    private lateinit var localeProvider: FakeLocaleProvider
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var formattingHelper: FormattingHelper
    private lateinit var userUiMapper: UserUiMapper
    private lateinit var mapper: SpendingInsightsUiMapper

    private val stringMap by lazy {
        mapOf(
            R.string.balances_insights_period_all_time to "All time",
            R.string.balances_insights_monthly_pace_projected_label to "Projected monthly pace",
            R.string.balances_insights_monthly_pace_completed_label to "Monthly total",
            R.string.balances_insights_monthly_pace_30d_label to "30-day pace",
            DesignSystemR.string.self_identification_nominative to "You",
            DesignSystemR.string.user_pending_fallback to "Pending",
            CommonR.string.expense_category_food to "Food",
            CommonR.string.expense_category_lodging to "Lodging",
            CommonR.string.expense_category_transport to "Transport"
        )
    }

    @BeforeEach
    fun setUp() {
        localeProvider = FakeLocaleProvider(Locale.US)
        resourceProvider = mockk()
        stubStringResources()
        stubPluralResources()

        formattingHelper = FormattingHelper(localeProvider = localeProvider)
        userUiMapper = UserUiMapper(resourceProvider = resourceProvider, localeProvider = localeProvider)
        mapper = SpendingInsightsUiMapper(
            formattingHelper = formattingHelper,
            resourceProvider = resourceProvider,
            userUiMapper = userUiMapper
        )
    }

    private fun stubStringResources() {
        every { resourceProvider.getString(any()) } answers {
            stringMap[firstArg<Int>()] ?: "str"
        }
        every { resourceProvider.getString(any(), *anyVararg()) } answers {
            val resId = firstArg<Int>()
            val args = secondArg<Array<Any>>()
            formatStringResource(resId, args)
        }
    }

    private fun formatStringResource(resId: Int, args: Array<Any>): String = when (resId) {
        R.string.balances_insights_period_range -> "${args[0]} – ${args[1]}"
        R.string.balances_insights_period_summary -> "${args[0]} · ${args[1]}"
        R.string.balances_insights_extras_included -> "Includes ${args[0]} in extras"
        R.string.balances_insights_daily_average_value -> "${args[0]} / day"
        R.string.balances_insights_monthly_pace_value -> "${args[0]} / mo"
        R.string.balances_insights_daily_bar_cd -> "${args[0]}, ${args[1]}"
        else -> args.joinToString(" ")
    }

    private fun stubPluralResources() {
        every { resourceProvider.getQuantityString(any(), any(), *anyVararg()) } answers {
            val resId = firstArg<Int>()
            val qty = secondArg<Int>()
            when (resId) {
                R.plurals.balances_insights_expense_count -> if (qty == 1) "$qty expense" else "$qty expenses"
                R.plurals.balances_insights_active_days -> if (qty == 1) "$qty active day" else "$qty active days"
                R.plurals.balances_insights_member_count -> if (qty == 1) "$qty member" else "$qty members"
                else -> qty.toString()
            }
        }
    }

    @Test
    fun `maps empty group insights with hasAnyGroupExpenses false and null burnRate`() {
        val insights = baseInsights(hasAnyGroupExpenses = false, totalSpentCents = 0L, expenseCount = 0)

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.ALL_TIME,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = null,
            referenceYear = 2026
        )

        assertFalse(state.isLoading)
        assertFalse(state.hasAnyGroupExpenses)
        assertFalse(state.hasPeriodExpenses)
        assertNull(state.burnRate)
        assertNull(state.selectedDayBarId)
        assertTrue(state.categoryItems.isEmpty())
        assertTrue(state.entityItems.isEmpty())
        assertTrue(state.memberItems.isEmpty())
        assertEquals("All time", state.periodSummaryLabel)
    }

    @Test
    fun `maps empty period insights preserving hasAnyGroupExpenses true and formatted custom dates`() {
        val start = LocalDate.of(2026, 10, 1)
        val end = LocalDate.of(2026, 10, 15)
        val insights = baseInsights(
            period = SpendingInsightsPeriod.CustomRange(start, end),
            hasAnyGroupExpenses = true,
            totalSpentCents = 0L,
            expenseCount = 0,
            windowStartDate = start,
            windowEndDate = end
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.CUSTOM,
            customStartDate = start,
            customEndDate = end,
            entityBreakdownMode = EntityBreakdownMode.BY_MEMBER,
            selectedDayBarId = "2026-10-05",
            referenceYear = 2026
        )

        assertTrue(state.hasAnyGroupExpenses)
        assertFalse(state.hasPeriodExpenses)
        assertNull(state.burnRate)
        assertEquals(formattingHelper.formatShortDate(start, 2026), state.formattedCustomStartDate)
        assertEquals(formattingHelper.formatShortDate(end, 2026), state.formattedCustomEndDate)
        assertEquals(
            "${formattingHelper.formatShortDate(start, 2026)} – ${formattingHelper.formatShortDate(end, 2026)}",
            state.periodSummaryLabel
        )
    }

    @Test
    fun `formats total spent daily average projected monthly pace and peak day with locale currency`() {
        localeProvider.currentLocale = Locale("es", "ES")
        val peakDate = LocalDate.of(2026, 11, 14)
        val insights = baseInsights(
            currency = "EUR",
            period = SpendingInsightsPeriod.ThisMonth,
            totalSpentCents = 12500L,
            totalExtrasCents = 500L,
            expenseCount = 3,
            windowStartDate = LocalDate.of(2026, 11, 1),
            windowEndDate = LocalDate.of(2026, 11, 30),
            averageDailySpendCents = 625L,
            projectedMonthlySpendCents = 18750L,
            activeDaysCount = 2,
            peakDayDate = peakDate,
            peakDayAmountCents = 9000L
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.THIS_MONTH,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = null,
            referenceYear = 2026
        )

        val burnRate = state.burnRate!!
        assertEquals(formattingHelper.formatCentsWithCurrency(12500L, "EUR"), burnRate.formattedTotalSpent)
        assertEquals(
            "Includes ${formattingHelper.formatCentsWithCurrency(500L, "EUR")} in extras",
            burnRate.formattedExtrasCaption
        )
        assertEquals(
            "${formattingHelper.formatCentsWithCurrency(625L, "EUR")} / day",
            burnRate.formattedDailyAverage
        )
        assertEquals("2 active days", burnRate.activeDaysLabel)
        assertEquals("Projected monthly pace", burnRate.monthlyPaceLabel)
        assertEquals(
            "${formattingHelper.formatCentsWithCurrency(18750L, "EUR")} / mo",
            burnRate.formattedMonthlyPace
        )
        assertEquals(formattingHelper.formatCentsWithCurrency(9000L, "EUR"), burnRate.formattedPeakDayAmount)
        assertEquals(formattingHelper.formatShortDate(peakDate, 2026), burnRate.formattedPeakDayDate)
    }

    @Test
    fun `maps category insights with ExpenseCategory labels icons ChartColors and BreakdownPercentage strings`() {
        val insights = baseInsights(
            currency = "USD",
            totalSpentCents = 20000L,
            expenseCount = 4,
            categories = listOf(
                CategorySpendingInsight(
                    category = ExpenseCategory.FOOD,
                    totalAmountCents = 15000L,
                    expenseCount = 3,
                    percentage = BigDecimal("75.00"),
                    shareRatio = BigDecimal("0.7500"),
                    breakdownPercentage = BreakdownPercentage.Whole(75)
                ),
                CategorySpendingInsight(
                    category = ExpenseCategory.LODGING,
                    totalAmountCents = 50L,
                    expenseCount = 1,
                    percentage = BigDecimal("0.25"),
                    shareRatio = BigDecimal("0.0025"),
                    breakdownPercentage = BreakdownPercentage.SubOnePercent
                ),
                CategorySpendingInsight(
                    category = ExpenseCategory.TRANSPORT,
                    totalAmountCents = 0L,
                    expenseCount = 0,
                    percentage = BigDecimal.ZERO,
                    shareRatio = BigDecimal.ZERO,
                    breakdownPercentage = BreakdownPercentage.Zero
                )
            )
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.ALL_TIME,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = null,
            referenceYear = 2026
        )

        assertEquals(3, state.categoryItems.size)
        assertEquals("Food", state.categoryItems[0].categoryName)
        assertEquals(ExpenseCategory.FOOD.toIconVector(), state.categoryItems[0].categoryIcon)
        assertEquals(ChartColors[0], state.categoryItems[0].color)
        assertEquals("75%", state.categoryItems[0].formattedPercentage)
        assertEquals("3 expenses", state.categoryItems[0].expenseCountLabel)
        assertEquals(0.75f, state.categoryItems[0].progress)

        assertEquals("<1%", state.categoryItems[1].formattedPercentage)
        assertEquals(ChartColors[1], state.categoryItems[1].color)
        assertEquals("0%", state.categoryItems[2].formattedPercentage)
        assertEquals(ChartColors[2], state.categoryItems[2].color)
    }

    @Test
    fun `maps daily series bars with peak day highlight and preserves valid selectedDayBarId`() {
        val d1 = LocalDate.of(2026, 11, 10)
        val d2 = LocalDate.of(2026, 11, 11)
        val insights = baseInsights(
            totalSpentCents = 5000L,
            expenseCount = 2,
            peakDayDate = d2,
            peakDayAmountCents = 4000L,
            dailySeries = listOf(
                DailySpendingPoint(d1, 1000L, 1, BigDecimal("0.2500")),
                DailySpendingPoint(d2, 4000L, 1, BigDecimal("1.0000"))
            )
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.ALL_TIME,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = "2026-11-10",
            referenceYear = 2026
        )

        assertEquals("2026-11-10", state.selectedDayBarId)
        val bars = state.burnRate!!.dailyBars
        assertEquals(2, bars.size)
        assertFalse(bars[0].isPeakDay)
        assertTrue(bars[1].isPeakDay)
        assertEquals("11", bars[1].axisLabel)
    }

    @Test
    fun `falls back to peak day bar id when selectedDayBarId is absent from new period`() {
        val d1 = LocalDate.of(2026, 11, 10)
        val d2 = LocalDate.of(2026, 11, 11)
        val insights = baseInsights(
            totalSpentCents = 5000L,
            expenseCount = 2,
            peakDayDate = d2,
            peakDayAmountCents = 4000L,
            dailySeries = listOf(
                DailySpendingPoint(d1, 1000L, 1, BigDecimal("0.2500")),
                DailySpendingPoint(d2, 4000L, 1, BigDecimal("1.0000"))
            )
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.THIS_MONTH,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = "2026-10-01",
            referenceYear = 2026
        )

        assertEquals("2026-11-11", state.selectedDayBarId)
    }

    @Test
    fun `maps subunit and member breakdowns with current user self-identification and member count subtitle`() {
        val insights = baseInsights(
            totalSpentCents = 10000L,
            expenseCount = 2,
            hasSubunits = true,
            currentUserId = "u1",
            memberProfiles = mapOf(
                "u1" to User(userId = "u1", email = "u1@test.com", displayName = "Alex"),
                "u2" to User(userId = "u2", email = "u2@test.com", displayName = "Sam")
            ),
            members = listOf(
                memberInsight("u1", 6000L, 2, 60),
                memberInsight("u2", 4000L, 1, 40)
            ),
            entities = listOf(
                EntitySpendingInsight(
                    entityId = "sub-1",
                    type = EntitySpendingType.SUBUNIT,
                    subunitName = "Alex & Sam",
                    memberIds = listOf("u1", "u2"),
                    effectiveSpentCents = 10000L,
                    expenseCount = 2,
                    percentage = BigDecimal("100.00"),
                    shareRatio = BigDecimal.ONE,
                    breakdownPercentage = BreakdownPercentage.Whole(100)
                )
            )
        )

        val state = mapper.mapToUiState(
            insights = insights,
            selectedPreset = SpendingPeriodPreset.ALL_TIME,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_SUBUNIT,
            selectedDayBarId = null,
            referenceYear = 2026
        )

        assertTrue(state.hasSubunits)
        assertEquals("Alex & Sam", state.entityItems[0].displayName)
        assertEquals("2 members", state.entityItems[0].subtitle)
        assertTrue(state.entityItems[0].isSubunit)
        assertTrue(state.entityItems[0].isCurrentUser)

        assertEquals("You", state.memberItems[0].displayName)
        assertEquals("2 expenses", state.memberItems[0].subtitle)
        assertTrue(state.memberItems[0].isCurrentUser)
        assertEquals("Sam", state.memberItems[1].displayName)
        assertFalse(state.memberItems[1].isCurrentUser)
    }

    @Test
    fun `formats monthly pace label differently for ThisMonth LastMonth and multi-week AllTime periods`() {
        val thisMonthState = mapper.mapToUiState(
            insights = baseInsights(
                period = SpendingInsightsPeriod.ThisMonth,
                totalSpentCents = 5000L,
                expenseCount = 1,
                projectedMonthlySpendCents = 10000L
            ),
            selectedPreset = SpendingPeriodPreset.THIS_MONTH,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_MEMBER,
            selectedDayBarId = null,
            referenceYear = 2026
        )
        assertEquals("Projected monthly pace", thisMonthState.burnRate!!.monthlyPaceLabel)
        assertTrue(thisMonthState.burnRate!!.formattedMonthlyPace!!.endsWith("/ mo"))

        val lastMonthState = mapper.mapToUiState(
            insights = baseInsights(
                period = SpendingInsightsPeriod.LastMonth,
                totalSpentCents = 5000L,
                expenseCount = 1,
                projectedMonthlySpendCents = 5000L
            ),
            selectedPreset = SpendingPeriodPreset.LAST_MONTH,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_MEMBER,
            selectedDayBarId = null,
            referenceYear = 2026
        )
        assertEquals("Monthly total", lastMonthState.burnRate!!.monthlyPaceLabel)
        assertFalse(lastMonthState.burnRate!!.formattedMonthlyPace!!.endsWith("/ mo"))

        val allTimeState = mapper.mapToUiState(
            insights = baseInsights(
                period = SpendingInsightsPeriod.AllTime,
                totalSpentCents = 5000L,
                expenseCount = 1,
                projectedMonthlySpendCents = 7500L
            ),
            selectedPreset = SpendingPeriodPreset.ALL_TIME,
            customStartDate = null,
            customEndDate = null,
            entityBreakdownMode = EntityBreakdownMode.BY_MEMBER,
            selectedDayBarId = null,
            referenceYear = 2026
        )
        assertEquals("30-day pace", allTimeState.burnRate!!.monthlyPaceLabel)
        assertTrue(allTimeState.burnRate!!.formattedMonthlyPace!!.endsWith("/ mo"))
    }

    private fun memberInsight(
        userId: String,
        spentCents: Long,
        expenseCount: Int,
        percent: Int
    ): MemberSpendingInsight = MemberSpendingInsight(
        userId = userId,
        effectiveSpentCents = spentCents,
        expenseCount = expenseCount,
        percentage = BigDecimal("$percent.00"),
        shareRatio = BigDecimal.valueOf(percent.toLong()).divide(BigDecimal(100)),
        breakdownPercentage = BreakdownPercentage.Whole(percent)
    )

    @Suppress("LongParameterList")
    private fun baseInsights(
        groupId: String = "group-1",
        currency: String = "EUR",
        period: SpendingInsightsPeriod = SpendingInsightsPeriod.AllTime,
        hasAnyGroupExpenses: Boolean = true,
        totalSpentCents: Long = 0L,
        totalExtrasCents: Long = 0L,
        expenseCount: Int = 0,
        windowStartDate: LocalDate? = null,
        windowEndDate: LocalDate? = null,
        averageDailySpendCents: Long = 0L,
        projectedMonthlySpendCents: Long? = null,
        activeDaysCount: Int = 0,
        peakDayDate: LocalDate? = null,
        peakDayAmountCents: Long = 0L,
        dailySeries: List<DailySpendingPoint> = emptyList(),
        categories: List<CategorySpendingInsight> = emptyList(),
        members: List<MemberSpendingInsight> = emptyList(),
        entities: List<EntitySpendingInsight> = emptyList(),
        hasSubunits: Boolean = false,
        memberProfiles: Map<String, User> = emptyMap(),
        currentUserId: String? = "u1"
    ): GroupSpendingInsights = GroupSpendingInsights(
        groupId = groupId,
        currency = currency,
        period = period,
        hasAnyGroupExpenses = hasAnyGroupExpenses,
        oldestExpenseDate = windowStartDate,
        newestExpenseDate = windowEndDate,
        totalSpentCents = totalSpentCents,
        totalBaseSpentCents = totalSpentCents - totalExtrasCents,
        totalExtrasCents = totalExtrasCents,
        expenseCount = expenseCount,
        burnRate = SpendingBurnRateInsight(
            windowStartDate = windowStartDate,
            windowEndDate = windowEndDate,
            elapsedDays = 10,
            totalWindowDays = 30,
            activeDaysCount = activeDaysCount,
            averageDailySpendCents = averageDailySpendCents,
            averagePerActiveDayCents = averageDailySpendCents,
            projectedMonthlySpendCents = projectedMonthlySpendCents,
            peakDayDate = peakDayDate,
            peakDayAmountCents = peakDayAmountCents,
            dailySeries = dailySeries
        ),
        categoryBreakdown = categories,
        memberBreakdown = members,
        entityBreakdown = entities,
        hasSubunits = hasSubunits,
        memberProfiles = memberProfiles,
        currentUserId = currentUserId
    )
}
