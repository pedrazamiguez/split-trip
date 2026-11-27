package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SettlementStatus
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SettlementRecordDocumentMapperTest {

    private val testId = "settlement-1"
    private val testGroupId = "group-123"
    private val testFromUserId = "user-1"
    private val testToUserId = "user-2"
    private val testAmountCents = 1000L
    private val testCurrency = "EUR"
    private val testSourcePocket = SettlementPocketType.CASH
    private val testStatus = SettlementStatus.SUGGESTED
    private val testTimestamp = LocalDateTime.of(2026, 7, 2, 12, 0, 0)
    private val testFirebaseTimestamp = Timestamp(
        testTimestamp.toInstant(ZoneOffset.UTC).epochSecond,
        testTimestamp.toInstant(ZoneOffset.UTC).nano
    )

    private val fullDomain = SettlementRecord(
        id = testId,
        groupId = testGroupId,
        settlement = Settlement(
            fromUserId = testFromUserId,
            toUserId = testToUserId,
            amount = testAmountCents,
            currency = testCurrency,
            sourcePocket = testSourcePocket
        ),
        status = testStatus,
        createdAt = testTimestamp,
        confirmedByPayerAt = testTimestamp,
        confirmedByPayeeAt = testTimestamp,
        resolvedAt = testTimestamp,
        disputedBy = "user-3",
        disputeReason = "Wrong amount"
    )

    @Nested
    inner class ToDocument {

        @Test
        fun `maps all fields correctly`() {
            val doc = fullDomain.toDocument()

            assertEquals(testId, doc["id"])
            assertEquals(testGroupId, doc["groupId"])
            assertEquals(testFromUserId, doc["fromUserId"])
            assertEquals(testToUserId, doc["toUserId"])
            assertEquals("1000", doc["amountCents"])
            assertEquals(testCurrency, doc["currency"])
            assertEquals(testSourcePocket.name, doc["sourcePocket"])
            assertEquals(SettlementStatus.SUGGESTED.name, doc["status"])
            assertNotNull(doc["createdAt"])
            assertNotNull(doc["confirmedByPayerAt"])
            assertNotNull(doc["confirmedByPayeeAt"])
            assertNotNull(doc["resolvedAt"])
            assertEquals("user-3", doc["disputedBy"])
            assertEquals("Wrong amount", doc["disputeReason"])
            assertEquals(FieldValue.serverTimestamp(), doc["serverUpdatedAt"])
            assertEquals(false, doc["isDeleted"])
            assertTrue(doc.containsKey("deletedAt"))
            assertNull(doc["deletedAt"])
            assertTrue(doc.containsKey("expireAt"))
            assertNull(doc["expireAt"])
        }

        @Test
        fun `nullable timestamps map to null`() {
            val domain = fullDomain.copy(
                confirmedByPayerAt = null,
                confirmedByPayeeAt = null,
                resolvedAt = null
            )
            val doc = domain.toDocument()

            assertNull(doc["confirmedByPayerAt"])
            assertNull(doc["confirmedByPayeeAt"])
            assertNull(doc["resolvedAt"])
        }
    }

    @Nested
    inner class ToDomain {

        @Test
        fun `maps all fields from DocumentSnapshot`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns testGroupId
            every { snapshot.getString("fromUserId") } returns testFromUserId
            every { snapshot.getString("toUserId") } returns testToUserId
            every { snapshot.getString("amountCents") } returns testAmountCents.toString()
            every { snapshot.getString("currency") } returns testCurrency
            every { snapshot.getString("sourcePocket") } returns testSourcePocket.name
            every { snapshot.getString("status") } returns testStatus.name
            every { snapshot.getTimestamp("createdAt") } returns testFirebaseTimestamp
            every { snapshot.getTimestamp("confirmedByPayerAt") } returns testFirebaseTimestamp
            every { snapshot.getTimestamp("confirmedByPayeeAt") } returns testFirebaseTimestamp
            every { snapshot.getTimestamp("resolvedAt") } returns testFirebaseTimestamp
            every { snapshot.getString("disputedBy") } returns "user-3"
            every { snapshot.getString("disputeReason") } returns "Wrong amount"

            val result = snapshot.toSettlementRecord()

            assertNotNull(result)
            assertEquals(testId, result!!.id)
            assertEquals(testGroupId, result.groupId)
            assertEquals(testFromUserId, result.settlement.fromUserId)
            assertEquals(testToUserId, result.settlement.toUserId)
            assertEquals(testAmountCents, result.settlement.amount)
            assertEquals(testCurrency, result.settlement.currency)
            assertEquals(testSourcePocket, result.settlement.sourcePocket)
            assertEquals(testStatus, result.status)
            assertEquals(testTimestamp, result.createdAt)
            assertEquals(testTimestamp, result.confirmedByPayerAt)
            assertEquals(testTimestamp, result.confirmedByPayeeAt)
            assertEquals(testTimestamp, result.resolvedAt)
            assertEquals("user-3", result.disputedBy)
            assertEquals("Wrong amount", result.disputeReason)
        }

        @Test
        fun `returns null when id is null`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns null
            every { snapshot.getString("groupId") } returns testGroupId

            assertNull(snapshot.toSettlementRecord())
        }

        @Test
        fun `returns null when groupId is null`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns null

            assertNull(snapshot.toSettlementRecord())
        }

        @Test
        fun `returns null when createdAt timestamp is null`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns testGroupId
            every { snapshot.getString("fromUserId") } returns testFromUserId
            every { snapshot.getString("toUserId") } returns testToUserId
            every { snapshot.getString("amountCents") } returns testAmountCents.toString()
            every { snapshot.getString("currency") } returns testCurrency
            every { snapshot.getString("sourcePocket") } returns testSourcePocket.name
            every { snapshot.getString("status") } returns testStatus.name
            every { snapshot.getTimestamp("createdAt") } returns null

            assertNull(snapshot.toSettlementRecord())
        }

        @Test
        fun `returns null when amountCents is not a valid long`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns testGroupId
            every { snapshot.getString("fromUserId") } returns testFromUserId
            every { snapshot.getString("toUserId") } returns testToUserId
            every { snapshot.getString("amountCents") } returns "not-a-number"
            every { snapshot.getString("currency") } returns testCurrency
            every { snapshot.getTimestamp("createdAt") } returns testFirebaseTimestamp

            assertNull(snapshot.toSettlementRecord())
        }

        @Test
        fun `nullable optional timestamps map to null`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns testGroupId
            every { snapshot.getString("fromUserId") } returns testFromUserId
            every { snapshot.getString("toUserId") } returns testToUserId
            every { snapshot.getString("amountCents") } returns testAmountCents.toString()
            every { snapshot.getString("currency") } returns testCurrency
            every { snapshot.getString("sourcePocket") } returns testSourcePocket.name
            every { snapshot.getString("status") } returns testStatus.name
            every { snapshot.getTimestamp("createdAt") } returns testFirebaseTimestamp
            every { snapshot.getTimestamp("confirmedByPayerAt") } returns null
            every { snapshot.getTimestamp("confirmedByPayeeAt") } returns null
            every { snapshot.getTimestamp("resolvedAt") } returns null
            every { snapshot.getString("disputedBy") } returns null
            every { snapshot.getString("disputeReason") } returns null

            val result = snapshot.toSettlementRecord()

            assertNotNull(result)
            assertNull(result!!.confirmedByPayerAt)
            assertNull(result.confirmedByPayeeAt)
            assertNull(result.resolvedAt)
            assertNull(result.disputedBy)
            assertNull(result.disputeReason)
        }

        @Test
        fun `returns null when document is soft-deleted tombstone`() {
            val snapshot = mockk<DocumentSnapshot>(relaxed = true)
            every { snapshot.getBoolean("isDeleted") } returns true
            every { snapshot.getString("id") } returns testId
            every { snapshot.getString("groupId") } returns testGroupId
            every { snapshot.getString("fromUserId") } returns testFromUserId
            every { snapshot.getString("toUserId") } returns testToUserId
            every { snapshot.getString("amountCents") } returns testAmountCents.toString()
            every { snapshot.getTimestamp("createdAt") } returns testFirebaseTimestamp

            assertNull(snapshot.toSettlementRecord())
            assertFalse(false)
        }
    }
}
