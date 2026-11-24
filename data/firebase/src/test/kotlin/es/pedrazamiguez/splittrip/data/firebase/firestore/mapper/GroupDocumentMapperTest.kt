package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.Group
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GroupDocumentMapperTest {

    private val testGroupId = "group-123"
    private val testUserId = "user-456"
    private val testTimestamp = LocalDateTime.of(2026, 1, 15, 12, 30, 0)
    private val testFirebaseTimestamp = testTimestamp.toTimestampUtc()!!

    private val fullGroup = Group(
        id = testGroupId,
        name = "Trip to Japan",
        description = "Travel expenses",
        currency = "EUR",
        extraCurrencies = listOf("JPY", "THB"),
        members = listOf("user-1", "user-2", "user-3"),
        mainImagePath = "/images/japan.jpg",
        createdAt = testTimestamp,
        lastUpdatedAt = testTimestamp,
        lastArchiveEventId = "archive-evt-123"
    )

    @Nested
    inner class ToDocument {

        @Test
        fun `maps all core fields correctly`() {
            val document = fullGroup.toDocument(testGroupId, testUserId)

            assertEquals(testGroupId, document.groupId)
            assertEquals("Trip to Japan", document.name)
            assertEquals("Travel expenses", document.description)
            assertEquals("EUR", document.currency)
            assertEquals(testUserId, document.createdBy)
            assertEquals("archive-evt-123", document.lastArchiveEventId)
        }

        @Test
        fun `maps mainImagePath correctly`() {
            val document = fullGroup.toDocument(testGroupId, testUserId)

            assertEquals("/images/japan.jpg", document.mainImagePath)
        }

        @Test
        fun `maps extraCurrencies list`() {
            val document = fullGroup.toDocument(testGroupId, testUserId)

            assertEquals(listOf("JPY", "THB"), document.extraCurrencies)
        }

        @Test
        fun `maps memberIds list`() {
            val document = fullGroup.toDocument(testGroupId, testUserId)

            assertEquals(listOf("user-1", "user-2", "user-3"), document.memberIds)
        }

        @Test
        fun `maps createdAt and lastUpdatedAt when present`() {
            val document = fullGroup.toDocument(testGroupId, testUserId)

            assertNotNull(document.createdAt)
            assertNotNull(document.lastUpdatedAt)
            assertEquals(testFirebaseTimestamp, document.createdAt)
            assertEquals(testFirebaseTimestamp, document.lastUpdatedAt)
        }

        @Test
        fun `createdAt and lastUpdatedAt are null when domain timestamps are null`() {
            val groupWithoutTimestamps = fullGroup.copy(createdAt = null, lastUpdatedAt = null)

            val document = groupWithoutTimestamps.toDocument(testGroupId, testUserId)

            assertNull(document.createdAt)
            assertNull(document.lastUpdatedAt)
        }

        @Test
        fun `maps empty lists correctly`() {
            val groupEmptyLists = fullGroup.copy(
                extraCurrencies = emptyList(),
                members = emptyList()
            )

            val document = groupEmptyLists.toDocument(testGroupId, testUserId)

            assertTrue(document.extraCurrencies.isEmpty())
            assertTrue(document.memberIds.isEmpty())
        }

        @Test
        fun `null lastArchiveEventId maps to null in document`() {
            val groupWithoutArchiveEventId = fullGroup.copy(lastArchiveEventId = null)
            val document = groupWithoutArchiveEventId.toDocument(testGroupId, testUserId)
            assertNull(document.lastArchiveEventId)
        }

        @Test
        fun `maps Group domain context EVERYDAY to GroupDocument context EVERYDAY`() {
            val everydayGroup = fullGroup.copy(context = GroupContext.EVERYDAY)
            val document = everydayGroup.toDocument(testGroupId, testUserId)
            assertEquals("EVERYDAY", document.context)
        }
    }

    @Nested
    inner class ToDomain {

        private val fullDocument = GroupDocument(
            groupId = testGroupId,
            name = "Trip to Japan",
            description = "Travel expenses",
            currency = "EUR",
            extraCurrencies = listOf("JPY", "THB"),
            memberIds = listOf("user-1", "user-2", "user-3"),
            mainImagePath = "/images/japan.jpg",
            createdBy = testUserId,
            createdAt = testFirebaseTimestamp,
            lastUpdatedAt = testFirebaseTimestamp,
            lastArchiveEventId = "archive-evt-123"
        )

        @Test
        fun `maps all core fields correctly`() {
            val group = fullDocument.toDomain()!!

            assertEquals(testGroupId, group.id)
            assertEquals("Trip to Japan", group.name)
            assertEquals("Travel expenses", group.description)
            assertEquals("EUR", group.currency)
            assertEquals("/images/japan.jpg", group.mainImagePath)
            assertEquals("archive-evt-123", group.lastArchiveEventId)
        }

        @Test
        fun `maps lists correctly`() {
            val group = fullDocument.toDomain()!!

            assertEquals(listOf("JPY", "THB"), group.extraCurrencies)
            assertEquals(listOf("user-1", "user-2", "user-3"), group.members)
        }

        @Test
        fun `maps timestamps correctly`() {
            val group = fullDocument.toDomain()!!

            assertEquals(testTimestamp, group.createdAt)
            assertEquals(testTimestamp, group.lastUpdatedAt)
        }

        @Test
        fun `null timestamps map to null domain fields`() {
            val documentNullTimestamps = fullDocument.copy(
                createdAt = null,
                lastUpdatedAt = null
            )

            val group = documentNullTimestamps.toDomain()!!

            assertNull(group.createdAt)
            assertNull(group.lastUpdatedAt)
        }

        @Test
        fun `maps empty lists correctly`() {
            val documentEmptyLists = fullDocument.copy(
                extraCurrencies = emptyList(),
                memberIds = emptyList()
            )

            val group = documentEmptyLists.toDomain()!!

            assertTrue(group.extraCurrencies.isEmpty())
            assertTrue(group.members.isEmpty())
        }

        @Test
        fun `null lastArchiveEventId maps to null in domain`() {
            val documentWithoutArchiveEventId = fullDocument.copy(lastArchiveEventId = null)
            val group = documentWithoutArchiveEventId.toDomain()!!
            assertNull(group.lastArchiveEventId)
        }

        @Test
        fun `returns null when deletionRequested is true`() {
            val deletedDocument = fullDocument.copy(deletionRequested = true)

            val group = deletedDocument.toDomain()

            assertNull(group)
        }

        @Test
        fun `returns group when deletionRequested is false`() {
            val activeDocument = fullDocument.copy(deletionRequested = false)

            val group = activeDocument.toDomain()

            assertNotNull(group)
            assertEquals(testGroupId, group!!.id)
        }

        @Test
        fun `maps GroupDocument context EVERYDAY to Group domain context EVERYDAY`() {
            val everydayDoc = fullDocument.copy(context = "EVERYDAY")
            val group = everydayDoc.toDomain()!!
            assertEquals(GroupContext.EVERYDAY, group.context)
        }

        @Test
        fun `maps missing or unknown GroupDocument context to TRAVEL default`() {
            val unknownDoc = fullDocument.copy(context = "UNKNOWN_CONTEXT")
            val emptyDoc = fullDocument.copy(context = "")

            assertEquals(GroupContext.TRAVEL, unknownDoc.toDomain()!!.context)
            assertEquals(GroupContext.TRAVEL, emptyDoc.toDomain()!!.context)
        }
    }

    @Nested
    inner class ToAdminMemberDocumentTest {

        @Test
        fun `sets userId and memberId to provided userId`() {
            val doc = toAdminMemberDocument(testGroupId, testUserId)

            assertEquals(testUserId, doc.userId)
            assertEquals(testUserId, doc.memberId)
        }

        @Test
        fun `sets role to ADMIN`() {
            val doc = toAdminMemberDocument(testGroupId, testUserId)

            assertEquals("ADMIN", doc.role)
        }

        @Test
        fun `sets groupId correctly`() {
            val doc = toAdminMemberDocument(testGroupId, testUserId)

            assertEquals(testGroupId, doc.groupId)
        }

        @Test
        fun `defaults addedBy to userId when not specified`() {
            val doc = toAdminMemberDocument(testGroupId, testUserId)

            assertEquals(testUserId, doc.addedBy)
        }

        @Test
        fun `uses explicit addedBy when provided`() {
            val adminUserId = "admin-789"
            val doc = toAdminMemberDocument(testGroupId, testUserId, addedBy = adminUserId)

            assertEquals(adminUserId, doc.addedBy)
            assertEquals(testUserId, doc.userId)
        }

        @Test
        fun `sets joinedAt to a non-null local timestamp`() {
            val doc = toAdminMemberDocument(testGroupId, testUserId)

            assertNotNull(doc.joinedAt)
        }
    }

    @Nested
    inner class ToRegularMemberDocumentTest {

        private val memberId = "member-789"
        private val addedByUserId = "admin-456"

        @Test
        fun `sets userId and memberId to provided memberId`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertEquals(memberId, doc.userId)
            assertEquals(memberId, doc.memberId)
        }

        @Test
        fun `sets role to MEMBER`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertEquals("MEMBER", doc.role)
        }

        @Test
        fun `sets groupId correctly`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertEquals(testGroupId, doc.groupId)
        }

        @Test
        fun `sets addedBy to the user who added the member`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertEquals(addedByUserId, doc.addedBy)
        }

        @Test
        fun `addedBy differs from memberId when admin adds another user`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertEquals(addedByUserId, doc.addedBy)
            assertEquals(memberId, doc.userId)
            assertTrue(doc.addedBy != doc.userId)
        }

        @Test
        fun `sets joinedAt to a non-null local timestamp`() {
            val doc = toRegularMemberDocument(testGroupId, memberId, addedBy = addedByUserId)

            assertNotNull(doc.joinedAt)
        }
    }
}
