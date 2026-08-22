package es.pedrazamiguez.splittrip.domain.model

import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import java.math.BigDecimal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SubExpenseTest {

    @Test
    fun `single expense isComposite is false`() {
        val expense = Expense(
            id = "exp-1",
            groupAmount = 10000L,
            paymentStatus = PaymentStatus.FINISHED
        )
        assertFalse(expense.isComposite)
        assertEquals(10000L, expense.paidGroupAmountCents)
        assertEquals(0, BigDecimal("100.0000").compareTo(expense.paidPercentage))
    }

    @Test
    fun `composite expense computes isComposite and paidGroupAmountCents correctly`() {
        val expense = Expense(
            id = "exp-1",
            groupAmount = 10000L,
            paymentStatus = PaymentStatus.PARTIAL,
            subExpenses = listOf(
                SubExpense(
                    id = "sub-1",
                    groupAmountCents = 3000L,
                    paymentStatus = PaymentStatus.FINISHED
                ),
                SubExpense(
                    id = "sub-2",
                    groupAmountCents = 2000L,
                    paymentStatus = PaymentStatus.FINISHED
                ),
                SubExpense(
                    id = "sub-3",
                    groupAmountCents = 5000L,
                    paymentStatus = PaymentStatus.SCHEDULED
                )
            )
        )
        assertTrue(expense.isComposite)
        assertEquals(5000L, expense.paidGroupAmountCents)
        assertEquals(0, BigDecimal("50.0000").compareTo(expense.paidPercentage))
    }

    @Test
    fun `cancelled sub-expenses are excluded from paidGroupAmountCents`() {
        val expense = Expense(
            id = "exp-1",
            groupAmount = 10000L,
            paymentStatus = PaymentStatus.PARTIAL,
            subExpenses = listOf(
                SubExpense(
                    id = "sub-1",
                    groupAmountCents = 3000L,
                    paymentStatus = PaymentStatus.FINISHED
                ),
                SubExpense(
                    id = "sub-2",
                    groupAmountCents = 2000L,
                    paymentStatus = PaymentStatus.CANCELLED
                ),
                SubExpense(
                    id = "sub-3",
                    groupAmountCents = 5000L,
                    paymentStatus = PaymentStatus.SCHEDULED
                )
            )
        )
        assertEquals(3000L, expense.paidGroupAmountCents)
        assertEquals(0, BigDecimal("30.0000").compareTo(expense.paidPercentage))
    }

    @Test
    fun `sub-expense default values`() {
        val sub = SubExpense(id = "sub-1")
        assertEquals("sub-1", sub.id)
        assertEquals("", sub.title)
        assertEquals(0L, sub.amountCents)
        assertEquals("EUR", sub.currency)
        assertEquals(0L, sub.groupAmountCents)
        assertEquals(BigDecimal.ONE, sub.exchangeRate)
        assertEquals(PaymentMethod.OTHER, sub.paymentMethod)
        assertEquals(PaymentStatus.FINISHED, sub.paymentStatus)
        assertTrue(sub.addOns.isEmpty())
        assertTrue(sub.cashTranches.isEmpty())
    }
}
