package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.CategorySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.DailySpendingPoint
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.GroupSpendingInsights
import es.pedrazamiguez.splittrip.domain.model.SpendingBurnRateInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.SpendingInsightsService
import es.pedrazamiguez.splittrip.domain.service.insights.EligibleInsightExpense
import es.pedrazamiguez.splittrip.domain.service.insights.SpendingInsightsEntityBreakdownCalculator
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class SpendingInsightsServiceImpl(
    private val addOnCalculationService: AddOnCalculationService,
    remainderDistributionService: RemainderDistributionService
) : SpendingInsightsService {

    private val breakdownCalculator = SpendingInsightsEntityBreakdownCalculator(remainderDistributionService)

    private data class DatedInsightExpense(
        val eligible: EligibleInsightExpense,
        val date: LocalDate?
    )

    private data class ResolvedWindow(
        val startDate: LocalDate?,
        val endDate: LocalDate?,
        val elapsedDays: Int,
        val totalWindowDays: Int,
        val seriesEndDate: LocalDate?
    )

    private data class CategoryAggregate(
        val category: ExpenseCategory,
        val totalAmountCents: Long,
        val expenseCount: Int
    )

    private data class DailyAggregate(
        val amountCents: Long,
        val expenseCount: Int
    )

    @Suppress("LongParameterList")
    override fun calculate(
        groupId: String,
        currency: String,
        expenses: List<Expense>,
        subunits: List<Subunit>,
        groupMemberIds: List<String>,
        period: SpendingInsightsPeriod,
        memberProfiles: Map<String, User>,
        currentUserId: String?,
        today: LocalDate
    ): GroupSpendingInsights {
        val eligibleExpenses = filterEligibleExpenses(expenses, today)
        val datedEligibleDates = eligibleExpenses.mapNotNull { it.date }
        val oldestExpenseDate = datedEligibleDates.minOrNull()
        val newestExpenseDate = datedEligibleDates.maxOrNull()

        val resolvedWindow = resolveWindow(
            period = period,
            today = today,
            oldestExpenseDate = oldestExpenseDate,
            newestExpenseDate = newestExpenseDate
        )
        val periodExpenses = filterExpensesForWindow(
            eligibleExpenses = eligibleExpenses,
            period = period,
            window = resolvedWindow
        )

        val totalSpentCents = periodExpenses.sumOf { it.eligible.effectiveAmountCents }
        val totalBaseSpentCents = periodExpenses.sumOf { it.eligible.expense.groupAmount }
        val totalExtrasCents = periodExpenses.sumOf { it.eligible.extrasCents }

        val burnRate = buildBurnRate(
            period = period,
            periodExpenses = periodExpenses,
            window = resolvedWindow,
            totalSpentCents = totalSpentCents
        )
        val categoryBreakdown = buildCategoryBreakdown(periodExpenses, totalSpentCents)
        val memberAndEntity = breakdownCalculator.calculate(
            periodExpenses = periodExpenses.map { it.eligible },
            subunits = subunits,
            groupMemberIds = groupMemberIds
        )

        return GroupSpendingInsights(
            groupId = groupId,
            currency = currency,
            period = period,
            hasAnyGroupExpenses = eligibleExpenses.isNotEmpty(),
            oldestExpenseDate = oldestExpenseDate,
            newestExpenseDate = newestExpenseDate,
            totalSpentCents = totalSpentCents,
            totalBaseSpentCents = totalBaseSpentCents,
            totalExtrasCents = totalExtrasCents,
            expenseCount = periodExpenses.size,
            burnRate = burnRate,
            categoryBreakdown = categoryBreakdown,
            memberBreakdown = memberAndEntity.memberBreakdown,
            entityBreakdown = memberAndEntity.entityBreakdown,
            hasSubunits = memberAndEntity.hasSubunits,
            memberProfiles = memberProfiles,
            currentUserId = currentUserId
        )
    }

    private fun filterEligibleExpenses(
        expenses: List<Expense>,
        today: LocalDate
    ): List<DatedInsightExpense> = expenses.mapNotNull { expense ->
        val isCancelled = expense.paymentStatus == PaymentStatus.CANCELLED
        val isFutureScheduled = expense.paymentStatus == PaymentStatus.SCHEDULED &&
            expense.dueDate?.toLocalDate()?.isAfter(today) == true
        if (isCancelled || isFutureScheduled) {
            return@mapNotNull null
        }

        val effectiveAmountCents = addOnCalculationService.calculateEffectiveGroupAmount(
            baseGroupAmount = expense.groupAmount,
            addOns = expense.addOns
        )
        if (effectiveAmountCents <= 0L) {
            return@mapNotNull null
        }

        val extrasCents = addOnCalculationService.calculateTotalAddOnExtras(expense.addOns)
        DatedInsightExpense(
            eligible = EligibleInsightExpense(
                expense = expense,
                effectiveAmountCents = effectiveAmountCents,
                extrasCents = extrasCents
            ),
            date = expense.effectiveDate?.toLocalDate()
        )
    }

    private fun resolveWindow(
        period: SpendingInsightsPeriod,
        today: LocalDate,
        oldestExpenseDate: LocalDate?,
        newestExpenseDate: LocalDate?
    ): ResolvedWindow = when (period) {
        SpendingInsightsPeriod.ThisMonth -> {
            val start = today.withDayOfMonth(1)
            val totalDays = today.lengthOfMonth()
            ResolvedWindow(
                startDate = start,
                endDate = today.withDayOfMonth(totalDays),
                elapsedDays = today.dayOfMonth.coerceAtLeast(1),
                totalWindowDays = totalDays,
                seriesEndDate = today
            )
        }

        SpendingInsightsPeriod.LastMonth -> {
            val lastMonth = YearMonth.from(today).minusMonths(1)
            val end = lastMonth.atEndOfMonth()
            val totalDays = lastMonth.lengthOfMonth()
            ResolvedWindow(
                startDate = lastMonth.atDay(1),
                endDate = end,
                elapsedDays = totalDays,
                totalWindowDays = totalDays,
                seriesEndDate = end
            )
        }

        is SpendingInsightsPeriod.CustomRange -> resolveCustomRangeWindow(period, today)

        SpendingInsightsPeriod.AllTime -> resolveAllTimeWindow(oldestExpenseDate, newestExpenseDate)
    }

    private fun resolveCustomRangeWindow(
        period: SpendingInsightsPeriod.CustomRange,
        today: LocalDate
    ): ResolvedWindow {
        val start = minOf(period.startDate, period.endDate)
        val end = maxOf(period.startDate, period.endDate)
        val totalDays = (ChronoUnit.DAYS.between(start, end).toInt() + 1).coerceAtLeast(1)
        val isTodayWithinRange = !today.isBefore(start) && today.isBefore(end)
        val elapsedDays = if (isTodayWithinRange) {
            (ChronoUnit.DAYS.between(start, today).toInt() + 1).coerceAtLeast(1)
        } else {
            totalDays
        }
        val seriesEnd = if (isTodayWithinRange) today else end
        return ResolvedWindow(
            startDate = start,
            endDate = end,
            elapsedDays = elapsedDays,
            totalWindowDays = totalDays,
            seriesEndDate = seriesEnd
        )
    }

    private fun resolveAllTimeWindow(
        oldestExpenseDate: LocalDate?,
        newestExpenseDate: LocalDate?
    ): ResolvedWindow {
        if (oldestExpenseDate == null || newestExpenseDate == null) {
            return ResolvedWindow(
                startDate = null,
                endDate = null,
                elapsedDays = 0,
                totalWindowDays = 0,
                seriesEndDate = null
            )
        }
        val totalDays = (ChronoUnit.DAYS.between(oldestExpenseDate, newestExpenseDate).toInt() + 1)
            .coerceAtLeast(1)
        return ResolvedWindow(
            startDate = oldestExpenseDate,
            endDate = newestExpenseDate,
            elapsedDays = totalDays,
            totalWindowDays = totalDays,
            seriesEndDate = newestExpenseDate
        )
    }

    private fun filterExpensesForWindow(
        eligibleExpenses: List<DatedInsightExpense>,
        period: SpendingInsightsPeriod,
        window: ResolvedWindow
    ): List<DatedInsightExpense> {
        if (period is SpendingInsightsPeriod.AllTime) {
            return eligibleExpenses
        }
        val start = window.startDate ?: return emptyList()
        val end = window.endDate ?: return emptyList()
        return eligibleExpenses.filter { item ->
            val date = item.date ?: return@filter false
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    private fun buildCategoryBreakdown(
        periodExpenses: List<DatedInsightExpense>,
        totalSpentCents: Long
    ): List<CategorySpendingInsight> {
        if (periodExpenses.isEmpty() || totalSpentCents <= 0L) return emptyList()

        val sorted = periodExpenses
            .groupBy { it.eligible.expense.category }
            .map { (category, items) ->
                CategoryAggregate(
                    category = category,
                    totalAmountCents = items.sumOf { it.eligible.effectiveAmountCents },
                    expenseCount = items.size
                )
            }
            .sortedWith(
                compareByDescending<CategoryAggregate> { it.totalAmountCents }
                    .thenBy { it.category.ordinal }
            )

        val metrics = breakdownCalculator.computeBreakdownMetrics(
            amounts = sorted.map { it.totalAmountCents },
            totalCents = totalSpentCents
        )
        return sorted.mapIndexed { index, item ->
            CategorySpendingInsight(
                category = item.category,
                totalAmountCents = item.totalAmountCents,
                expenseCount = item.expenseCount,
                percentage = metrics.percentages[index],
                shareRatio = breakdownCalculator.computeShareRatio(item.totalAmountCents, metrics.totalCents),
                breakdownPercentage = metrics.breakdownPercentages[index]
            )
        }
    }

    private fun buildBurnRate(
        period: SpendingInsightsPeriod,
        periodExpenses: List<DatedInsightExpense>,
        window: ResolvedWindow,
        totalSpentCents: Long
    ): SpendingBurnRateInsight {
        val groupedByDate = groupExpensesByDate(periodExpenses)
        val activeDaysCount = groupedByDate.size
        val averageDailySpendCents = divideToLong(totalSpentCents, window.elapsedDays.toLong())
        val averagePerActiveDayCents = divideToLong(totalSpentCents, activeDaysCount.toLong())
        val projectedMonthlySpendCents = resolveProjectedMonthlySpend(
            period = period,
            totalSpentCents = totalSpentCents,
            elapsedDays = window.elapsedDays,
            totalWindowDays = window.totalWindowDays
        )
        val peakEntry = groupedByDate.entries.maxWithOrNull(
            compareBy<Map.Entry<LocalDate, DailyAggregate>> { it.value.amountCents }
                .thenBy { it.key }
        )
        val dailySeries = buildDailySeries(groupedByDate, window)

        return SpendingBurnRateInsight(
            windowStartDate = window.startDate,
            windowEndDate = window.endDate,
            elapsedDays = window.elapsedDays,
            totalWindowDays = window.totalWindowDays,
            activeDaysCount = activeDaysCount,
            averageDailySpendCents = averageDailySpendCents,
            averagePerActiveDayCents = averagePerActiveDayCents,
            projectedMonthlySpendCents = projectedMonthlySpendCents,
            peakDayDate = peakEntry?.key,
            peakDayAmountCents = peakEntry?.value?.amountCents ?: 0L,
            dailySeries = dailySeries
        )
    }

    private fun groupExpensesByDate(
        periodExpenses: List<DatedInsightExpense>
    ): Map<LocalDate, DailyAggregate> {
        val result = linkedMapOf<LocalDate, DailyAggregate>()
        for (item in periodExpenses) {
            val date = item.date ?: continue
            val existing = result[date]
            result[date] = DailyAggregate(
                amountCents = (existing?.amountCents ?: 0L) + item.eligible.effectiveAmountCents,
                expenseCount = (existing?.expenseCount ?: 0) + 1
            )
        }
        return result
    }

    private fun resolveProjectedMonthlySpend(
        period: SpendingInsightsPeriod,
        totalSpentCents: Long,
        elapsedDays: Int,
        totalWindowDays: Int
    ): Long? {
        if (totalSpentCents <= 0L || elapsedDays <= 0) return null
        return when (period) {
            SpendingInsightsPeriod.ThisMonth -> BigDecimal.valueOf(totalSpentCents)
                .multiply(BigDecimal.valueOf(totalWindowDays.toLong()))
                .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_UP)
                .toLong()

            SpendingInsightsPeriod.LastMonth -> totalSpentCents

            SpendingInsightsPeriod.AllTime,
            is SpendingInsightsPeriod.CustomRange -> {
                if (elapsedDays >= MIN_DAYS_FOR_MONTHLY_PACE) {
                    BigDecimal.valueOf(totalSpentCents)
                        .multiply(STANDARD_MONTH_DAYS)
                        .divide(BigDecimal.valueOf(elapsedDays.toLong()), 0, RoundingMode.HALF_UP)
                        .toLong()
                } else {
                    null
                }
            }
        }
    }

    private fun buildDailySeries(
        groupedByDate: Map<LocalDate, DailyAggregate>,
        window: ResolvedWindow
    ): List<DailySpendingPoint> {
        if (groupedByDate.isEmpty()) return emptyList()

        val dates = resolveSeriesDates(groupedByDate, window)
        val maxDailyCents = groupedByDate.values.maxOfOrNull { it.amountCents } ?: 0L

        return dates.map { date ->
            val aggregate = groupedByDate[date]
            val amount = aggregate?.amountCents ?: 0L
            DailySpendingPoint(
                date = date,
                amountCents = amount,
                expenseCount = aggregate?.expenseCount ?: 0,
                heightRatio = breakdownCalculator.computeShareRatio(amount, maxDailyCents)
            )
        }
    }

    private fun resolveSeriesDates(
        groupedByDate: Map<LocalDate, DailyAggregate>,
        window: ResolvedWindow
    ): List<LocalDate> {
        val seriesStart = window.startDate
        val seriesEnd = window.seriesEndDate
        val isCompactWindow = seriesStart != null &&
            seriesEnd != null &&
            !seriesEnd.isBefore(seriesStart) &&
            ChronoUnit.DAYS.between(seriesStart, seriesEnd) < MAX_CONTIGUOUS_DAILY_BARS

        return if (isCompactWindow) {
            generateSequence(seriesStart) { current -> current.plusDays(1) }
                .takeWhile { current -> !current.isAfter(seriesEnd) }
                .toList()
        } else {
            groupedByDate.keys.sorted()
        }
    }

    private fun divideToLong(numerator: Long, denominator: Long): Long {
        if (numerator <= 0L || denominator <= 0L) return 0L
        return BigDecimal.valueOf(numerator)
            .divide(BigDecimal.valueOf(denominator), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private companion object {
        private val STANDARD_MONTH_DAYS = BigDecimal("30")
        private const val MIN_DAYS_FOR_MONTHLY_PACE = 7
        private const val MAX_CONTIGUOUS_DAILY_BARS = 31L
    }
}
