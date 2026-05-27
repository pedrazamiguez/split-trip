package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AddOnDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CashWithdrawalDocumentMapperTest {

    private val testWithdrawalId = "withdrawal-123"
    private val testGroupId = "group-456"
    private val testUserId = "user-789"
    private val testActorId = "actor-111"
    private val testGroupDocRef: DocumentReference = mockk(relaxed = true)
    private val testTimestamp = LocalDateTime.of(2026, 1, 15, 12, 30, 0)
    private val testFirebaseTimestamp = testTimestamp.toTimestampUtc()!!

    private val fullWithdrawal = CashWithdrawal(
        id = testWithdrawalId,
        groupId = testGroupId,
        withdrawnBy = "user-1",
        createdBy = testActorId,
        withdrawalScope = PayerType.GROUP,
        subunitId = null,
        amountWithdrawn = 1000000L,
        remainingAmount = 750000L,
        currency = "THB",
        deductedBaseAmount = 27000L,
        exchangeRate = BigDecimal("37.037"),
        createdAt = testTimestamp,
        lastUpdatedAt = testTimestamp
    )

    @Nested
    inner class ToDocument {

        @Test
        fun `maps all core fields correctly`() {
            val document = fullWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(testWithdrawalId, document.withdrawalId)
            assertEquals(testGroupId, document.groupId)
            assertEquals(testGroupDocRef, document.groupRef)
            assertEquals("user-1", document.withdrawnBy)
            assertEquals("GROUP", document.withdrawalScope)
            assertNull(document.subunitId)
            assertEquals(1000000L, document.amountWithdrawn)
            assertEquals(750000L, document.remainingAmount)
            assertEquals("THB", document.currency)
            assertEquals(27000L, document.deductedBaseAmount)
            assertEquals("37.037", document.exchangeRate)
            assertEquals(testActorId, document.createdBy)
        }

        @Test
        fun `maps SUBUNIT scope with subunitId`() {
            val subunitWithdrawal = fullWithdrawal.copy(
                withdrawalScope = PayerType.SUBUNIT,
                subunitId = "subunit-123"
            )

            val document = subunitWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("SUBUNIT", document.withdrawalScope)
            assertEquals("subunit-123", document.subunitId)
        }

        @Test
        fun `maps USER scope with null subunitId`() {
            val personalWithdrawal = fullWithdrawal.copy(
                withdrawalScope = PayerType.USER,
                subunitId = null
            )

            val document = personalWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("USER", document.withdrawalScope)
            assertNull(document.subunitId)
        }

        @Test
        fun `maps createdAt and lastUpdatedAt when present`() {
            val document = fullWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertNotNull(document.createdAt)
            assertNotNull(document.lastUpdatedAt)
            assertEquals(testFirebaseTimestamp, document.createdAt)
            assertEquals(testFirebaseTimestamp, document.lastUpdatedAt)
        }

        @Test
        fun `falls back to LocalDateTime now when timestamps are null`() {
            val withdrawalNoTimestamps = fullWithdrawal.copy(
                createdAt = null,
                lastUpdatedAt = null
            )

            val document = withdrawalNoTimestamps.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            // The mapper uses LocalDateTime.now() as fallback, so timestamps must be non-null
            assertNotNull(document.createdAt)
            assertNotNull(document.lastUpdatedAt)
        }

        @Test
        fun `falls back to userId when withdrawnBy is blank`() {
            val withdrawalBlankUser = fullWithdrawal.copy(withdrawnBy = "")

            val document = withdrawalBlankUser.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(testUserId, document.withdrawnBy)
        }

        @Test
        fun `preserves withdrawnBy when not blank`() {
            val document = fullWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("user-1", document.withdrawnBy)
        }

        @Test
        fun `preserves createdBy from domain model when not blank`() {
            val document = fullWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(testActorId, document.createdBy)
        }

        @Test
        fun `falls back to userId param when domain createdBy is blank`() {
            val withdrawalBlankCreatedBy = fullWithdrawal.copy(createdBy = "")

            val document = withdrawalBlankCreatedBy.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(testUserId, document.createdBy)
        }

        @Test
        fun `maps exchangeRate BigDecimal to plain string`() {
            val withdrawalPreciseRate = fullWithdrawal.copy(
                exchangeRate = BigDecimal("0.00002700")
            )

            val document = withdrawalPreciseRate.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("0.00002700", document.exchangeRate)
        }

        @Test
        fun `maps title notes and receiptLocalUri when present`() {
            val withdrawalWithDetails = fullWithdrawal.copy(
                title = "Airport ATM",
                notes = "Bad rate but no other option",
                receiptLocalUri = "content://media/photo/123"
            )

            val document = withdrawalWithDetails.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("Airport ATM", document.title)
            assertEquals("Bad rate but no other option", document.notes)
            assertEquals("content://media/photo/123", document.receiptLocalUri)
        }

        @Test
        fun `maps null title notes and receiptLocalUri`() {
            val document = fullWithdrawal.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertNull(document.title)
            assertNull(document.notes)
            assertNull(document.receiptLocalUri)
        }
    }

    @Nested
    inner class ToDomain {

        private val fullDocument = CashWithdrawalDocument(
            withdrawalId = testWithdrawalId,
            groupId = testGroupId,
            withdrawnBy = "user-1",
            withdrawalScope = "GROUP",
            subunitId = null,
            amountWithdrawn = 1000000L,
            remainingAmount = 750000L,
            currency = "THB",
            deductedBaseAmount = 27000L,
            exchangeRate = "37.037",
            createdBy = testActorId,
            createdAt = testFirebaseTimestamp,
            lastUpdatedAt = testFirebaseTimestamp
        )

        @Test
        fun `maps all core fields correctly`() {
            val withdrawal = fullDocument.toDomain()

            assertEquals(testWithdrawalId, withdrawal.id)
            assertEquals(testGroupId, withdrawal.groupId)
            assertEquals("user-1", withdrawal.withdrawnBy)
            assertEquals(testActorId, withdrawal.createdBy)
            assertEquals(PayerType.GROUP, withdrawal.withdrawalScope)
            assertNull(withdrawal.subunitId)
            assertEquals(1000000L, withdrawal.amountWithdrawn)
            assertEquals(750000L, withdrawal.remainingAmount)
            assertEquals("THB", withdrawal.currency)
            assertEquals(27000L, withdrawal.deductedBaseAmount)
            assertEquals(0, BigDecimal("37.037").compareTo(withdrawal.exchangeRate))
        }

        @Test
        fun `maps SUBUNIT scope with subunitId`() {
            val subunitDocument = fullDocument.copy(
                withdrawalScope = "SUBUNIT",
                subunitId = "subunit-123"
            )

            val withdrawal = subunitDocument.toDomain()

            assertEquals(PayerType.SUBUNIT, withdrawal.withdrawalScope)
            assertEquals("subunit-123", withdrawal.subunitId)
        }

        @Test
        fun `maps USER scope with null subunitId`() {
            val personalDocument = fullDocument.copy(
                withdrawalScope = "USER",
                subunitId = null
            )

            val withdrawal = personalDocument.toDomain()

            assertEquals(PayerType.USER, withdrawal.withdrawalScope)
            assertNull(withdrawal.subunitId)
        }

        @Test
        fun `defaults to GROUP when withdrawalScope is unknown`() {
            val unknownScopeDocument = fullDocument.copy(
                withdrawalScope = "INVALID_VALUE"
            )

            val withdrawal = unknownScopeDocument.toDomain()

            assertEquals(PayerType.GROUP, withdrawal.withdrawalScope)
        }

        @Test
        fun `maps timestamps correctly`() {
            val withdrawal = fullDocument.toDomain()

            assertEquals(testTimestamp, withdrawal.createdAt)
            assertEquals(testTimestamp, withdrawal.lastUpdatedAt)
        }

        @Test
        fun `null timestamps map to null domain fields`() {
            val documentNullTimestamps = fullDocument.copy(
                createdAt = null,
                lastUpdatedAt = null
            )

            val withdrawal = documentNullTimestamps.toDomain()

            assertNull(withdrawal.createdAt)
            assertNull(withdrawal.lastUpdatedAt)
        }

        @Test
        fun `maps exchangeRate string to BigDecimal`() {
            val withdrawal = fullDocument.toDomain()

            assertEquals(0, BigDecimal("37.037").compareTo(withdrawal.exchangeRate))
        }

        @Test
        fun `maps title notes and receiptLocalUri when present in document`() {
            val documentWithDetails = fullDocument.copy(
                title = "Hotel exchange desk",
                notes = "Got a decent rate here",
                receiptLocalUri = "content://media/photo/456"
            )

            val withdrawal = documentWithDetails.toDomain()

            assertEquals("Hotel exchange desk", withdrawal.title)
            assertEquals("Got a decent rate here", withdrawal.notes)
            assertEquals("content://media/photo/456", withdrawal.receiptLocalUri)
        }

        @Test
        fun `null title notes and receiptLocalUri map to null domain fields`() {
            val withdrawal = fullDocument.toDomain()

            assertNull(withdrawal.title)
            assertNull(withdrawal.notes)
            assertNull(withdrawal.receiptLocalUri)
        }

        @Test
        fun `maps addOns from document to domain`() {
            val addOnDoc = AddOnDocument(
                id = "addon-1",
                type = "FEE",
                mode = "ON_TOP",
                valueType = "EXACT",
                amountCents = 250L,
                currency = "THB",
                exchangeRate = "37.037",
                groupAmountCents = 250L,
                paymentMethod = "CASH",
                description = "Bank fee"
            )

            val documentWithAddOns = fullDocument.copy(addOns = listOf(addOnDoc))

            val withdrawal = documentWithAddOns.toDomain()

            assertEquals(1, withdrawal.addOns.size)
            val addOn = withdrawal.addOns[0]
            assertEquals("addon-1", addOn.id)
            assertEquals(AddOnType.FEE, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
            assertEquals(AddOnValueType.EXACT, addOn.valueType)
            assertEquals(250L, addOn.amountCents)
            assertEquals("Bank fee", addOn.description)
        }

        @Test
        fun `defaults to FEE ON_TOP EXACT for unknown addOn enum strings`() {
            val addOnDoc = AddOnDocument(
                id = "addon-unknown",
                type = "UNKNOWN_TYPE",
                mode = "UNKNOWN_MODE",
                valueType = "UNKNOWN_VALUE",
                amountCents = 100L,
                currency = "EUR",
                exchangeRate = "1.0",
                groupAmountCents = 100L,
                paymentMethod = "OTHER"
            )

            val documentWithUnknownAddOn = fullDocument.copy(addOns = listOf(addOnDoc))

            val withdrawal = documentWithUnknownAddOn.toDomain()

            val addOn = withdrawal.addOns[0]
            assertEquals(AddOnType.FEE, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
            assertEquals(AddOnValueType.EXACT, addOn.valueType)
        }

        @Test
        fun `maps empty addOns list`() {
            val documentNoAddOns = fullDocument.copy(addOns = emptyList())

            val withdrawal = documentNoAddOns.toDomain()

            assertEquals(0, withdrawal.addOns.size)
        }
    }

    @Nested
    inner class ToDocumentWithAddOns {

        private val sampleAddOn = AddOn(
            id = "addon-1",
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 250L,
            currency = "THB",
            exchangeRate = BigDecimal("37.037"),
            groupAmountCents = 250L,
            paymentMethod = PaymentMethod.CASH,
            description = "Bank fee"
        )

        @Test
        fun `maps addOns from domain to document`() {
            val withdrawalWithAddOns = fullWithdrawal.copy(addOns = listOf(sampleAddOn))

            val document = withdrawalWithAddOns.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(1, document.addOns.size)
            val addOnDoc = document.addOns[0]
            assertEquals("addon-1", addOnDoc.id)
            assertEquals("FEE", addOnDoc.type)
            assertEquals("ON_TOP", addOnDoc.mode)
            assertEquals("EXACT", addOnDoc.valueType)
            assertEquals("Bank fee", addOnDoc.description)
        }

        @Test
        fun `serializes addOn exchangeRate as plain string`() {
            val withdrawalWithAddOns = fullWithdrawal.copy(addOns = listOf(sampleAddOn))

            val document = withdrawalWithAddOns.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals("37.037", document.addOns[0].exchangeRate)
        }

        @Test
        fun `maps empty addOns list`() {
            val withdrawalNoAddOns = fullWithdrawal.copy(addOns = emptyList())

            val document = withdrawalNoAddOns.toDocument(
                testWithdrawalId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(0, document.addOns.size)
        }
    }
}
