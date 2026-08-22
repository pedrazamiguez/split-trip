package es.pedrazamiguez.splittrip.data.local.converter

import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.SubExpense
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SubExpenseListConverter")
class SubExpenseListConverterTest {

    private val converter = SubExpenseListConverter()

    private val sampleSubExpense1 = SubExpense(
        id = "sub-1",
        title = "Deposit reservation",
        amountCents = 5000L,
        currency = "EUR",
        groupAmountCents = 5000L,
        exchangeRate = BigDecimal("1.000000"),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        paymentStatus = PaymentStatus.FINISHED,
        payerType = PayerType.USER,
        payerId = "user-1",
        dueDate = LocalDateTime.of(2026, 8, 20, 10, 0),
        operationDate = LocalDateTime.of(2026, 8, 20, 10, 0),
        notes = "Wire transfer fee included",
        addOns = listOf(
            AddOn(
                id = "addon-1",
                type = AddOnType.FEE,
                mode = AddOnMode.ON_TOP,
                valueType = AddOnValueType.EXACT,
                amountCents = 250,
                currency = "EUR",
                groupAmountCents = 250
            )
        ),
        cashTranches = emptyList()
    )

    private val sampleSubExpense2 = SubExpense(
        id = "sub-2",
        title = "Final payment",
        amountCents = 8000L,
        currency = "USD",
        groupAmountCents = 7360L,
        exchangeRate = BigDecimal("0.920000"),
        paymentMethod = PaymentMethod.CASH,
        paymentStatus = PaymentStatus.SCHEDULED,
        payerType = PayerType.GROUP,
        payerId = null,
        dueDate = LocalDateTime.of(2026, 9, 1, 12, 0),
        operationDate = null,
        notes = null,
        addOns = emptyList(),
        cashTranches = listOf(
            CashTranche(withdrawalId = "w-1", amountConsumed = 8000L)
        )
    )

    @Nested
    @DisplayName("Serialize")
    inner class Serialize {

        @Test
        fun `returns null for null or empty input`() {
            assertNull(converter.fromSubExpenseList(null))
            assertNull(converter.fromSubExpenseList(emptyList()))
        }

        @Test
        fun `serializes list to valid JSON`() {
            val json = converter.fromSubExpenseList(listOf(sampleSubExpense1, sampleSubExpense2))
            assertNotNull(json)
            assertTrue(json!!.contains("sub-1"))
            assertTrue(json.contains("sub-2"))
            assertTrue(json.contains("Deposit reservation"))
            assertTrue(json.contains("w-1"))
        }
    }

    @Nested
    @DisplayName("Deserialize & RoundTrip")
    inner class DeserializeAndRoundTrip {

        @Test
        fun `returns null for null or blank input`() {
            assertNull(converter.toSubExpenseList(null))
            assertNull(converter.toSubExpenseList(""))
            assertNull(converter.toSubExpenseList("   "))
        }

        @Test
        fun `round-trip preserves all fields`() {
            val original = listOf(sampleSubExpense1, sampleSubExpense2)
            val json = converter.fromSubExpenseList(original)
            val restored = converter.toSubExpenseList(json)

            assertNotNull(restored)
            assertEquals(2, restored!!.size)

            original.zip(restored).forEach { (orig, rest) ->
                assertEquals(orig.id, rest.id)
                assertEquals(orig.title, rest.title)
                assertEquals(orig.amountCents, rest.amountCents)
                assertEquals(orig.currency, rest.currency)
                assertEquals(orig.groupAmountCents, rest.groupAmountCents)
                assertEquals(0, orig.exchangeRate.compareTo(rest.exchangeRate))
                assertEquals(orig.paymentMethod, rest.paymentMethod)
                assertEquals(orig.paymentStatus, rest.paymentStatus)
                assertEquals(orig.payerType, rest.payerType)
                assertEquals(orig.payerId, rest.payerId)
                assertEquals(orig.dueDate, rest.dueDate)
                assertEquals(orig.operationDate, rest.operationDate)
                assertEquals(orig.notes, rest.notes)
                assertEquals(orig.addOns.size, rest.addOns.size)
                assertEquals(orig.cashTranches.size, rest.cashTranches.size)
            }
        }
    }
}
