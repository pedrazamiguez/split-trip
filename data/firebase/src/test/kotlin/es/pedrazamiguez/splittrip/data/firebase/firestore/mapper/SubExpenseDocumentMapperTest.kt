package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AddOnDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.SubExpenseDocument
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ExpenseDocumentMapper - SubExpense Mapping")
class SubExpenseDocumentMapperTest {

    private val testGroupDocRef: DocumentReference = mockk(relaxed = true)
    private val testDateTime = LocalDateTime.of(2026, 8, 20, 14, 0, 0)
    private val testFirebaseTimestamp = testDateTime.toTimestampUtc()!!

    private val sampleSubExpense = SubExpense(
        id = "sub-1",
        title = "Tranche 1",
        amountCents = 5000L,
        currency = "EUR",
        groupAmountCents = 5000L,
        exchangeRate = BigDecimal("1.000000"),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        paymentStatus = PaymentStatus.FINISHED,
        payerType = PayerType.USER,
        payerId = "user-1",
        dueDate = testDateTime,
        operationDate = testDateTime,
        notes = "First payment",
        addOns = listOf(
            AddOn(
                id = "addon-1",
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 250L,
                currency = "EUR",
                groupAmountCents = 250L
            )
        ),
        cashTranches = listOf(
            CashTranche(withdrawalId = "w-1", amountConsumed = 2000L)
        )
    )

    @Test
    fun `maps subExpenses from domain to document`() {
        val expense = Expense(
            id = "exp-1",
            subExpenses = listOf(sampleSubExpense)
        )

        val doc = expense.toDocument("exp-1", "grp-1", testGroupDocRef, "user-1")

        assertEquals(1, doc.subExpenses.size)
        val subDoc = doc.subExpenses[0]
        assertEquals("sub-1", subDoc.id)
        assertEquals("Tranche 1", subDoc.title)
        assertEquals(5000L, subDoc.amountCents)
        assertEquals("EUR", subDoc.currency)
        assertEquals("1.000000", subDoc.exchangeRate)
        assertEquals("CREDIT_CARD", subDoc.paymentMethod)
        assertEquals("FINISHED", subDoc.paymentStatus)
        assertEquals("USER", subDoc.payerType)
        assertEquals("user-1", subDoc.payerId)
        assertEquals(testFirebaseTimestamp, subDoc.dueDate)
        assertEquals(testFirebaseTimestamp, subDoc.operationDate)
        assertEquals("First payment", subDoc.notes)
        assertEquals(1, subDoc.addOns.size)
        assertEquals(1, subDoc.cashTranches.size)
    }

    @Test
    fun `maps subExpenses from document to domain`() {
        val subDoc = SubExpenseDocument(
            id = "sub-1",
            title = "Tranche 1",
            amountCents = 5000L,
            currency = "EUR",
            groupAmountCents = 5000L,
            exchangeRate = "1.000000",
            paymentMethod = "CREDIT_CARD",
            paymentStatus = "FINISHED",
            payerType = "USER",
            payerId = "user-1",
            dueDate = testFirebaseTimestamp,
            operationDate = testFirebaseTimestamp,
            notes = "First payment",
            addOns = listOf(
                AddOnDocument(
                    id = "addon-1",
                    type = "FEE",
                    mode = "ON_TOP",
                    valueType = "EXACT",
                    amountCents = 250L,
                    currency = "EUR",
                    groupAmountCents = 250L
                )
            ),
            cashTranches = listOf(
                mapOf("withdrawalId" to "w-1", "amountConsumed" to 2000L)
            )
        )

        val doc = ExpenseDocument(
            expenseId = "exp-1",
            subExpenses = listOf(subDoc)
        )

        val expense = doc.toDomain()

        assertEquals(1, expense.subExpenses.size)
        val sub = expense.subExpenses[0]
        assertEquals("sub-1", sub.id)
        assertEquals("Tranche 1", sub.title)
        assertEquals(5000L, sub.amountCents)
        assertEquals("EUR", sub.currency)
        assertEquals(0, BigDecimal("1.000000").compareTo(sub.exchangeRate))
        assertEquals(PaymentMethod.CREDIT_CARD, sub.paymentMethod)
        assertEquals(PaymentStatus.FINISHED, sub.paymentStatus)
        assertEquals(PayerType.USER, sub.payerType)
        assertEquals("user-1", sub.payerId)
        assertEquals(testDateTime, sub.dueDate)
        assertEquals(testDateTime, sub.operationDate)
        assertEquals("First payment", sub.notes)
        assertEquals(1, sub.addOns.size)
        assertEquals(1, sub.cashTranches.size)
    }
}
