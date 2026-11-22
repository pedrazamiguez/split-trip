package es.pedrazamiguez.splittrip.domain.service.insights

import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.AddOnTypeSpendingInsight
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.CategorySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.SpendingAddOnsSummary
import es.pedrazamiguez.splittrip.domain.model.SubcategorySpendingInsight
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import java.math.BigDecimal
import java.math.RoundingMode

internal class SpendingInsightsCategoryCalculator(
    private val breakdownCalculator: SpendingInsightsEntityBreakdownCalculator,
    private val remainderDistributionService: RemainderDistributionService
) {

    private data class CategoryAggregate(
        val category: ExpenseCategory,
        val totalAmountCents: Long,
        val expenseCount: Int,
        val items: List<EligibleInsightExpense>
    )

    private data class SubcategoryAggregate(
        val subcategory: ExpenseSubcategory,
        val totalAmountCents: Long,
        val expenseCount: Int
    )

    private data class AddOnTypeAggregate(
        var totalAmountCents: Long = 0L,
        var itemCount: Int = 0
    )

    private data class ExpenseAddOnTotals(
        val extrasCents: Long,
        val discountsCents: Long
    )

    fun buildCategoryBreakdown(
        periodExpenses: List<EligibleInsightExpense>,
        totalSpentCents: Long
    ): List<CategorySpendingInsight> {
        if (periodExpenses.isEmpty() || totalSpentCents <= 0L) return emptyList()

        val sorted = periodExpenses
            .groupBy { it.expense.category }
            .map { (category, items) ->
                CategoryAggregate(
                    category = category,
                    totalAmountCents = items.sumOf { it.effectiveAmountCents },
                    expenseCount = items.size,
                    items = items
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
                breakdownPercentage = metrics.breakdownPercentages[index],
                subcategories = buildSubcategoryBreakdown(
                    categoryItems = item.items,
                    categoryTotalCents = item.totalAmountCents
                )
            )
        }
    }

    private fun buildSubcategoryBreakdown(
        categoryItems: List<EligibleInsightExpense>,
        categoryTotalCents: Long
    ): List<SubcategorySpendingInsight> {
        if (categoryItems.isEmpty() || categoryTotalCents <= 0L) return emptyList()

        val sorted = categoryItems
            .groupBy { it.expense.subcategory }
            .map { (subcategory, items) ->
                SubcategoryAggregate(
                    subcategory = subcategory,
                    totalAmountCents = items.sumOf { it.effectiveAmountCents },
                    expenseCount = items.size
                )
            }
            .sortedWith(
                compareByDescending<SubcategoryAggregate> { it.totalAmountCents }
                    .thenBy { it.subcategory.ordinal }
            )

        val metrics = breakdownCalculator.computeBreakdownMetrics(
            amounts = sorted.map { it.totalAmountCents },
            totalCents = categoryTotalCents
        )
        return sorted.mapIndexed { index, item ->
            SubcategorySpendingInsight(
                subcategory = item.subcategory,
                totalAmountCents = item.totalAmountCents,
                expenseCount = item.expenseCount,
                percentage = metrics.percentages[index],
                shareRatio = breakdownCalculator.computeShareRatio(item.totalAmountCents, metrics.totalCents),
                breakdownPercentage = metrics.breakdownPercentages[index]
            )
        }
    }

    fun buildAddOnsSummary(
        periodExpenses: List<EligibleInsightExpense>,
        periodWithdrawals: List<CashWithdrawal>,
        totalSpentCents: Long
    ): SpendingAddOnsSummary {
        val typeAggregates = mutableMapOf<AddOnType, AddOnTypeAggregate>()
        val expenseTotals = accumulateExpenseAddOns(periodExpenses, typeAggregates)
        val withdrawalFeesCents = accumulateWithdrawalAddOns(periodWithdrawals, typeAggregates)
        val totalExtrasCents = expenseTotals.extrasCents + withdrawalFeesCents
        val typeBreakdown = buildSortedTypeBreakdown(typeAggregates)
        val cappedExpenseExtras = expenseTotals.extrasCents.coerceAtMost(totalSpentCents)

        return SpendingAddOnsSummary(
            totalExtrasCents = totalExtrasCents,
            expenseExtrasCents = expenseTotals.extrasCents,
            withdrawalFeesCents = withdrawalFeesCents,
            totalDiscountsCents = expenseTotals.discountsCents,
            extrasShareRatio = breakdownCalculator.computeShareRatio(cappedExpenseExtras, totalSpentCents),
            extrasBreakdownPercentage = resolveExtrasPercentage(cappedExpenseExtras, totalSpentCents),
            typeBreakdown = typeBreakdown
        )
    }

    private fun accumulateExpenseAddOns(
        periodExpenses: List<EligibleInsightExpense>,
        typeAggregates: MutableMap<AddOnType, AddOnTypeAggregate>
    ): ExpenseAddOnTotals {
        var extrasCents = 0L
        var discountsCents = 0L
        for (item in periodExpenses) {
            for (addOn in item.expense.addOns) {
                when {
                    addOn.type == AddOnType.DISCOUNT -> discountsCents += resolveDiscountCents(addOn)
                    addOn.groupAmountCents > 0L -> {
                        extrasCents += addOn.groupAmountCents
                        recordAddOnType(typeAggregates, addOn.type, addOn.groupAmountCents)
                    }
                }
            }
        }
        return ExpenseAddOnTotals(extrasCents = extrasCents, discountsCents = discountsCents)
    }

    private fun accumulateWithdrawalAddOns(
        periodWithdrawals: List<CashWithdrawal>,
        typeAggregates: MutableMap<AddOnType, AddOnTypeAggregate>
    ): Long {
        var withdrawalFeesCents = 0L
        periodWithdrawals
            .asSequence()
            .flatMap { it.addOns.asSequence() }
            .filter { it.type != AddOnType.DISCOUNT && it.groupAmountCents > 0L }
            .forEach { addOn ->
                withdrawalFeesCents += addOn.groupAmountCents
                recordAddOnType(typeAggregates, addOn.type, addOn.groupAmountCents)
            }
        return withdrawalFeesCents
    }

    private fun buildSortedTypeBreakdown(
        typeAggregates: Map<AddOnType, AddOnTypeAggregate>
    ): List<AddOnTypeSpendingInsight> = typeAggregates.entries
        .map { (type, aggregate) ->
            AddOnTypeSpendingInsight(
                type = type,
                totalAmountCents = aggregate.totalAmountCents,
                itemCount = aggregate.itemCount
            )
        }
        .sortedWith(
            compareByDescending<AddOnTypeSpendingInsight> { it.totalAmountCents }
                .thenBy { it.type.ordinal }
        )

    private fun resolveExtrasPercentage(
        cappedExpenseExtras: Long,
        totalSpentCents: Long
    ): BreakdownPercentage {
        if (cappedExpenseExtras <= 0L || totalSpentCents <= 0L) return BreakdownPercentage.Zero
        val remainingCents = (totalSpentCents - cappedExpenseExtras).coerceAtLeast(0L)
        return remainderDistributionService
            .distributeBreakdownPercentages(listOf(cappedExpenseExtras, remainingCents), totalSpentCents)
            .firstOrNull()
            ?: BreakdownPercentage.Zero
    }

    private fun recordAddOnType(
        aggregates: MutableMap<AddOnType, AddOnTypeAggregate>,
        type: AddOnType,
        amountCents: Long
    ) {
        val aggregate = aggregates.getOrPut(type) { AddOnTypeAggregate() }
        aggregate.totalAmountCents += amountCents
        aggregate.itemCount += 1
    }

    private fun resolveDiscountCents(addOn: AddOn): Long {
        if (addOn.groupAmountCents > 0L) return addOn.groupAmountCents
        if (addOn.amountCents > 0L) {
            return BigDecimal(addOn.amountCents)
                .multiply(addOn.exchangeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()
        }
        return 0L
    }
}
