package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.ExpenseSubcategory
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SpendingInsightsSubcategoryAndAddOnTest {

    private val today = LocalDate.of(2026, 11, 20)
    private val service = SpendingInsightsServiceImpl(
        addOnCalculationService = AddOnCalculationServiceImpl(),
        remainderDistributionService = RemainderDistributionServiceImpl()
    )

    @Test
    fun `category breakdown includes sorted subcategories summing to 100 percent`() {
        val expenses = listOf(
            createExpense(
                id = "e1",
                groupAmount = 7000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.RESTAURANT
            ),
            createExpense(
                id = "e2",
                groupAmount = 3000L,
                category = ExpenseCategory.FOOD,
                subcategory = ExpenseSubcategory.CAFE_BREAKFAST
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

        assertEquals(1, result.categoryBreakdown.size)
        val food = result.categoryBreakdown.first()
        assertEquals(2, food.subcategories.size)
        assertEquals(ExpenseSubcategory.RESTAURANT, food.subcategories[0].subcategory)
        assertEquals(7000L, food.subcategories[0].totalAmountCents)
        assertEquals(BreakdownPercentage.Whole(70), food.subcategories[0].breakdownPercentage)
        assertEquals(ExpenseSubcategory.CAFE_BREAKFAST, food.subcategories[1].subcategory)
        assertEquals(3000L, food.subcategories[1].totalAmountCents)
        assertEquals(BreakdownPercentage.Whole(30), food.subcategories[1].breakdownPercentage)
    }

    @Test
    fun `addOnsSummary aggregates expense fees surcharges tips discounts and period withdrawal fees`() {
        val expenses = listOf(
            createExpense(
                id = "e1",
                groupAmount = 10000L,
                addOns = listOf(
                    AddOn(id = "a1", type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 500L),
                    AddOn(id = "a2", type = AddOnType.TIP, mode = AddOnMode.INCLUDED, groupAmountCents = 1500L),
                    AddOn(id = "a3", type = AddOnType.DISCOUNT, mode = AddOnMode.ON_TOP, amountCents = 300L)
                )
            )
        )
        val withdrawals = listOf(
            CashWithdrawal(
                id = "w1",
                groupId = "group-1",
                createdAt = today.atTime(10, 0),
                addOns = listOf(
                    AddOn(id = "wa1", type = AddOnType.FEE, mode = AddOnMode.ON_TOP, groupAmountCents = 400L)
                )
            ),
            CashWithdrawal(
                id = "w2",
                groupId = "group-1",
                createdAt = today.minusMonths(2).atTime(10, 0),
                addOns = listOf(
                    AddOn(id = "wa2", type = AddOnType.SURCHARGE, mode = AddOnMode.ON_TOP, groupAmountCents = 999L)
                )
            )
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
            today = today,
            withdrawals = withdrawals,
            isFiltered = false
        )

        assertEquals(2000L, result.addOnsSummary.expenseExtrasCents)
        assertEquals(400L, result.addOnsSummary.withdrawalFeesCents)
        assertEquals(2400L, result.addOnsSummary.totalExtrasCents)
        assertEquals(300L, result.addOnsSummary.totalDiscountsCents)
        assertEquals(2, result.addOnsSummary.typeBreakdown.size)
        assertEquals(AddOnType.TIP, result.addOnsSummary.typeBreakdown[0].type)
        assertEquals(1500L, result.addOnsSummary.typeBreakdown[0].totalAmountCents)
        assertEquals(AddOnType.FEE, result.addOnsSummary.typeBreakdown[1].type)
        assertEquals(900L, result.addOnsSummary.typeBreakdown[1].totalAmountCents)
        assertFalse(result.isFiltered)
    }

    private fun createExpense(
        id: String,
        groupAmount: Long,
        category: ExpenseCategory = ExpenseCategory.FOOD,
        subcategory: ExpenseSubcategory = ExpenseSubcategory.UNSPECIFIED,
        addOns: List<AddOn> = emptyList()
    ): Expense = Expense(
        id = id,
        groupId = "group-1",
        sourceAmount = groupAmount,
        groupAmount = groupAmount,
        category = category,
        subcategory = subcategory,
        paymentStatus = PaymentStatus.FINISHED,
        operationDate = today.atTime(12, 0),
        addOns = addOns,
        splits = listOf(ExpenseSplit(userId = "u1", amountCents = groupAmount))
    )
}
