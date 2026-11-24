package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupMemberDocument
import es.pedrazamiguez.splittrip.domain.enums.GroupContext
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FirestoreGroupDataSourceImplTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var authenticationService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor
    private lateinit var dataSource: FirestoreGroupDataSourceImpl

    private val testUserId = "1vjerwDcOqdPUzWSR39tFgZIUvx1"
    private val testGroupId = "92b6ac9e-f7d4-4a07-a237-7816e01bdfbf"

    @BeforeEach
    fun setUp() {
        firestore = mockk(relaxed = true)
        authenticationService = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true) {
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        }

        every { authenticationService.requireUserId() } returns testUserId

        dataSource = FirestoreGroupDataSourceImpl(
            firestore = firestore,
            authenticationService = authenticationService,
            performanceMonitor = performanceMonitor
        )
    }

    // region Helper methods

    private fun mockGroupDocumentRef(groupId: String): DocumentReference {
        val groupsCollection = mockk<CollectionReference>(relaxed = true)
        val groupDocRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection(GroupDocument.COLLECTION_PATH) } returns groupsCollection
        every { groupsCollection.document(groupId) } returns groupDocRef
        every { groupDocRef.id } returns groupId

        return groupDocRef
    }

    private fun mockMemberCollectionRef(groupId: String, userId: String): DocumentReference {
        val membersCollection = mockk<CollectionReference>(relaxed = true)
        val memberDocRef = mockk<DocumentReference>(relaxed = true)

        every {
            firestore.collection(GroupMemberDocument.collectionPath(groupId))
        } returns membersCollection
        every { membersCollection.document(userId) } returns memberDocRef

        return memberDocRef
    }

    private fun mockBatch(): WriteBatch {
        val batch = mockk<WriteBatch>(relaxed = true)
        every { firestore.batch() } returns batch
        every { batch.set(any(), any()) } returns batch
        every { batch.update(any(), any<Map<String, Any>>()) } returns batch
        every { batch.delete(any()) } returns batch
        every { batch.commit() } returns Tasks.forResult(null)
        return batch
    }

    private fun mockDocumentSnapshot(exists: Boolean, groupDocument: GroupDocument? = null): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>(relaxed = true)
        every { snapshot.exists() } returns exists
        if (groupDocument != null) {
            every { snapshot.toObject(GroupDocument::class.java) } returns groupDocument
        } else {
            every { snapshot.toObject(GroupDocument::class.java) } returns null
        }
        return snapshot
    }

    // endregion

    @Nested
    inner class CreateGroup {

        @Test
        fun `creates group with creator added to memberIds when not already present`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Trip to Paris",
                description = "Summer trip",
                currency = "EUR",
                members = listOf("user-other-1", "user-other-2"),
                createdAt = LocalDateTime.of(2025, 6, 15, 10, 0),
                lastUpdatedAt = LocalDateTime.of(2025, 6, 15, 10, 0)
            )

            val groupDocRef = mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val groupDocSlot = slot<GroupDocument>()
            every { batch.set(groupDocRef, capture(groupDocSlot)) } returns batch

            // When
            val result = dataSource.createGroup(group)

            // Then
            assertEquals(testGroupId, result)

            val capturedDoc = groupDocSlot.captured
            assertTrue(testUserId in capturedDoc.memberIds)
            assertTrue("user-other-1" in capturedDoc.memberIds)
            assertTrue("user-other-2" in capturedDoc.memberIds)
            assertEquals(3, capturedDoc.memberIds.size)
        }

        @Test
        fun `creates group without duplicating creator when already in members`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Trip to Berlin",
                description = "Weekend trip",
                currency = "EUR",
                members = listOf(testUserId, "user-other-1"),
                createdAt = LocalDateTime.of(2025, 7, 1, 9, 0),
                lastUpdatedAt = LocalDateTime.of(2025, 7, 1, 9, 0)
            )

            val groupDocRef = mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val groupDocSlot = slot<GroupDocument>()
            every { batch.set(groupDocRef, capture(groupDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedDoc = groupDocSlot.captured
            val creatorOccurrences = capturedDoc.memberIds.count { it == testUserId }
            assertEquals(1, creatorOccurrences, "Creator should appear exactly once")
            assertEquals(2, capturedDoc.memberIds.size)
        }

        @Test
        fun `creates group with only creator when members list is empty`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Solo Trip",
                description = "Just me",
                currency = "USD",
                members = emptyList(),
                createdAt = LocalDateTime.of(2025, 8, 10, 14, 0),
                lastUpdatedAt = LocalDateTime.of(2025, 8, 10, 14, 0)
            )

            val groupDocRef = mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val groupDocSlot = slot<GroupDocument>()
            every { batch.set(groupDocRef, capture(groupDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedDoc = groupDocSlot.captured
            assertEquals(listOf(testUserId), capturedDoc.memberIds)
        }

        @Test
        fun `creates admin member document in subcollection`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Admin Test",
                members = emptyList()
            )

            mockGroupDocumentRef(testGroupId)
            val memberDocRef = mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val memberDocSlot = slot<GroupMemberDocument>()
            every { batch.set(memberDocRef, capture(memberDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedMemberDoc = memberDocSlot.captured
            assertEquals(testUserId, capturedMemberDoc.userId)
            assertEquals(testUserId, capturedMemberDoc.memberId)
            assertEquals("ADMIN", capturedMemberDoc.role)
        }

        @Test
        fun `sets addedBy to creator for admin member document`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "AddedBy Admin Test",
                members = emptyList()
            )

            mockGroupDocumentRef(testGroupId)
            val memberDocRef = mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val memberDocSlot = slot<GroupMemberDocument>()
            every { batch.set(memberDocRef, capture(memberDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedMemberDoc = memberDocSlot.captured
            assertEquals(testUserId, capturedMemberDoc.addedBy)
        }

        @Test
        fun `sets addedBy to creator for regular member documents`() = runTest {
            // Given
            val otherMemberId = "user-other-1"
            val group = Group(
                id = testGroupId,
                name = "AddedBy Regular Test",
                members = listOf(otherMemberId)
            )

            val groupDocRef = mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val otherMemberDocRef = mockMemberCollectionRef(testGroupId, otherMemberId)
            val batch = mockBatch()

            val memberDocSlot = slot<GroupMemberDocument>()
            every { batch.set(otherMemberDocRef, capture(memberDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedMemberDoc = memberDocSlot.captured
            assertEquals(otherMemberId, capturedMemberDoc.userId)
            assertEquals(
                testUserId,
                capturedMemberDoc.addedBy,
                "addedBy should be the creator, not the member themselves"
            )
        }

        @Test
        fun `returns the group id`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Return ID Test",
                members = emptyList()
            )

            mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            mockBatch()

            // When
            val result = dataSource.createGroup(group)

            // Then
            assertEquals(testGroupId, result)
        }

        @Test
        fun `maps group fields correctly to document`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Field Mapping Test",
                description = "Testing all fields",
                currency = "GBP",
                extraCurrencies = listOf("USD", "EUR"),
                members = listOf(testUserId),
                createdAt = LocalDateTime.of(2025, 3, 20, 8, 30),
                lastUpdatedAt = LocalDateTime.of(2025, 3, 20, 9, 45)
            )

            val groupDocRef = mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            val groupDocSlot = slot<GroupDocument>()
            every { batch.set(groupDocRef, capture(groupDocSlot)) } returns batch

            // When
            dataSource.createGroup(group)

            // Then
            val capturedDoc = groupDocSlot.captured
            assertEquals(testGroupId, capturedDoc.groupId)
            assertEquals("Field Mapping Test", capturedDoc.name)
            assertEquals("Testing all fields", capturedDoc.description)
            assertEquals("GBP", capturedDoc.currency)
            assertEquals(listOf("USD", "EUR"), capturedDoc.extraCurrencies)
            assertEquals(testUserId, capturedDoc.createdBy)
        }

        @Test
        fun `commits the batch with group and member documents`() = runTest {
            // Given
            val group = Group(
                id = testGroupId,
                name = "Batch Commit Test",
                members = emptyList()
            )

            mockGroupDocumentRef(testGroupId)
            mockMemberCollectionRef(testGroupId, testUserId)
            val batch = mockBatch()

            // When
            dataSource.createGroup(group)

            // Then
            verify(exactly = 2) { batch.set(any(), any()) }
            verify(exactly = 1) { batch.commit() }
        }
    }

    @Nested
    inner class GetGroupById {

        @Test
        fun `returns group from cache when available`() = runTest {
            // Given
            val groupDocument = GroupDocument(
                groupId = testGroupId,
                name = "Cached Group",
                description = "From cache",
                currency = "EUR",
                memberIds = listOf("user-1", "user-2")
            )

            val snapshot = mockDocumentSnapshot(exists = true, groupDocument = groupDocument)
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every { groupDocRef.get(com.google.firebase.firestore.Source.CACHE) } returns Tasks.forResult(
                snapshot
            )

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertEquals(testGroupId, result?.id)
            assertEquals("Cached Group", result?.name)
            assertEquals(listOf("user-1", "user-2"), result?.members)
        }

        @Test
        fun `falls back to server when cache misses`() = runTest {
            // Given - Cache throws exception
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every {
                groupDocRef.get(com.google.firebase.firestore.Source.CACHE)
            } returns Tasks.forException(Exception("Cache miss"))

            // Given - Server returns valid document
            val groupDocument = GroupDocument(
                groupId = testGroupId,
                name = "Server Group",
                description = "From server",
                currency = "USD",
                memberIds = listOf("user-a", "user-b", "user-c")
            )
            val serverSnapshot = mockDocumentSnapshot(exists = true, groupDocument = groupDocument)
            every { groupDocRef.get() } returns Tasks.forResult(serverSnapshot)

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertEquals(testGroupId, result?.id)
            assertEquals("Server Group", result?.name)
            assertEquals(listOf("user-a", "user-b", "user-c"), result?.members)
        }

        @Test
        fun `returns null when group does not exist on server`() = runTest {
            // Given - Cache miss
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every {
                groupDocRef.get(com.google.firebase.firestore.Source.CACHE)
            } returns Tasks.forException(Exception("Cache miss"))

            // Given - Server returns non-existent doc
            val snapshot = mockDocumentSnapshot(exists = false)
            every { groupDocRef.get() } returns Tasks.forResult(snapshot)

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `returns null when server fetch fails`() = runTest {
            // Given - Cache miss
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every {
                groupDocRef.get(com.google.firebase.firestore.Source.CACHE)
            } returns Tasks.forException(Exception("Cache miss"))

            // Given - Server also fails
            every { groupDocRef.get() } returns Tasks.forException(Exception("Network error"))

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertNull(result)
        }

        @Test
        fun `maps empty memberIds for backward compatibility`() = runTest {
            // Given - A group document with no memberIds (legacy data)
            val groupDocument = GroupDocument(
                groupId = testGroupId,
                name = "Legacy Group",
                description = "No members field",
                currency = "EUR",
                memberIds = emptyList()
            )

            val snapshot = mockDocumentSnapshot(exists = true, groupDocument = groupDocument)
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every { groupDocRef.get(com.google.firebase.firestore.Source.CACHE) } returns Tasks.forResult(
                snapshot
            )

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertEquals(testGroupId, result?.id)
            assertEquals(emptyList<String>(), result?.members)
        }

        @Test
        fun `maps memberIds correctly to domain members`() = runTest {
            // Given
            val expectedMembers = listOf("user-alpha", "user-beta", "user-gamma", "user-delta")
            val groupDocument = GroupDocument(
                groupId = testGroupId,
                name = "Full Members Group",
                memberIds = expectedMembers
            )

            val snapshot = mockDocumentSnapshot(exists = true, groupDocument = groupDocument)
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every { groupDocRef.get(com.google.firebase.firestore.Source.CACHE) } returns Tasks.forResult(
                snapshot
            )

            // When
            val result = dataSource.getGroupById(testGroupId)

            // Then
            assertEquals(expectedMembers.sorted(), result?.members)
        }
    }

    @Nested
    inner class DeleteGroup {

        private fun mockDeleteGroupSetup(
            memberDocIds: List<String> = emptyList()
        ): Pair<DocumentReference, List<DocumentReference>> {
            // Mock group document
            val groupsCollection = mockk<CollectionReference>(relaxed = true)
            val groupDocRef = mockk<DocumentReference>(relaxed = true)
            every { firestore.collection(GroupDocument.COLLECTION_PATH) } returns groupsCollection
            every { groupsCollection.document(testGroupId) } returns groupDocRef
            every { groupDocRef.delete() } returns Tasks.forResult(null)

            // Mock members subcollection
            val membersCollection = mockk<CollectionReference>(relaxed = true)
            every {
                firestore.collection(GroupMemberDocument.collectionPath(testGroupId))
            } returns membersCollection

            val memberDocRefs = memberDocIds.map { _ ->
                val docRef = mockk<DocumentReference>(relaxed = true)
                every { docRef.delete() } returns Tasks.forResult(null)
                docRef
            }

            val memberSnapshots = memberDocRefs.map { docRef ->
                val snapshot = mockk<DocumentSnapshot>(relaxed = true)
                every { snapshot.reference } returns docRef
                snapshot
            }

            val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
            every { querySnapshot.documents } returns memberSnapshots
            every { membersCollection.get() } returns Tasks.forResult(querySnapshot)

            return groupDocRef to memberDocRefs
        }

        @Test
        fun `deletes member documents before group document`() = runTest {
            // Given
            val (groupDocRef, memberDocRefs) = mockDeleteGroupSetup(
                memberDocIds = listOf("user-1", "user-2")
            )

            // When
            dataSource.deleteGroup(testGroupId)

            // Then - All member docs should be deleted
            memberDocRefs.forEach { memberDocRef ->
                verify(exactly = 1) { memberDocRef.delete() }
            }
            // And the group doc should be deleted
            verify(exactly = 1) { groupDocRef.delete() }
        }

        @Test
        fun `deletes group document from Firestore`() = runTest {
            // Given
            val (groupDocRef, _) = mockDeleteGroupSetup()

            // When
            dataSource.deleteGroup(testGroupId)

            // Then
            verify(exactly = 1) { groupDocRef.delete() }
        }

        @Test
        fun `handles group with no members`() = runTest {
            // Given
            val (groupDocRef, _) = mockDeleteGroupSetup(memberDocIds = emptyList())

            // When
            dataSource.deleteGroup(testGroupId)

            // Then - Group doc should still be deleted
            verify(exactly = 1) { groupDocRef.delete() }
        }

        @Test
        fun `deletes all member documents for multi-member group`() = runTest {
            // Given
            val (_, memberDocRefs) = mockDeleteGroupSetup(
                memberDocIds = listOf("user-a", "user-b", "user-c", "user-d")
            )

            // When
            dataSource.deleteGroup(testGroupId)

            // Then
            assertEquals(4, memberDocRefs.size)
            memberDocRefs.forEach { memberDocRef ->
                verify(exactly = 1) { memberDocRef.delete() }
            }
        }
    }

    @Nested
    inner class RequestGroupDeletion {

        private fun mockRequestGroupDeletionSetup(
            memberDocIds: List<String> = emptyList()
        ): Pair<DocumentReference, List<DocumentReference>> {
            val groupDocRef = mockGroupDocumentRef(testGroupId)

            // Mock members subcollection
            val membersCollection = mockk<CollectionReference>(relaxed = true)
            every {
                firestore.collection(GroupMemberDocument.collectionPath(testGroupId))
            } returns membersCollection

            val memberDocRefs = memberDocIds.map { id ->
                val docRef = mockk<DocumentReference>(relaxed = true)
                every { docRef.id } returns id
                docRef
            }

            val memberSnapshots = memberDocRefs.map { docRef ->
                val snapshot = mockk<DocumentSnapshot>(relaxed = true)
                every { snapshot.reference } returns docRef
                snapshot
            }

            val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
            every { querySnapshot.documents } returns memberSnapshots
            every { membersCollection.get() } returns Tasks.forResult(querySnapshot)

            return groupDocRef to memberDocRefs
        }

        @Test
        fun `uses WriteBatch to atomically update group and delete member doc`() = runTest {
            // Given
            val (groupDocRef, memberDocRefs) = mockRequestGroupDeletionSetup(listOf(testUserId))
            val batch = mockBatch()

            // When
            dataSource.requestGroupDeletion(testGroupId)

            // Then — single batch commit with both operations
            verify(exactly = 1) { batch.update(groupDocRef, any<Map<String, Any>>()) }
            verify(exactly = 1) { batch.delete(memberDocRefs[0]) }
            verify(exactly = 1) { batch.commit() }
        }

        @Test
        fun `includes correct deletion fields in batch update`() = runTest {
            // Given
            mockRequestGroupDeletionSetup(listOf(testUserId))
            val batch = mockBatch()

            // When
            dataSource.requestGroupDeletion(testGroupId)

            // Then — verify the update map contains the correct fields
            val mapSlot = slot<Map<String, Any>>()
            verify { batch.update(any(), capture(mapSlot)) }
            val capturedMap = mapSlot.captured
            assertEquals(true, capturedMap["deletionRequested"])
            assertEquals(testUserId, capturedMap["deletedBy"])
            assertTrue(capturedMap.containsKey("deletedAt"))
        }

        @Test
        fun `deletes all user member docs for the group`() = runTest {
            // Given
            val (_, memberDocRefs) = mockRequestGroupDeletionSetup(listOf(testUserId, "other-user-1", "other-user-2"))
            val batch = mockBatch()

            // When
            dataSource.requestGroupDeletion(testGroupId)

            // Then — all member docs are deleted
            assertEquals(3, memberDocRefs.size)
            memberDocRefs.forEach { memberDocRef ->
                verify(exactly = 1) { batch.delete(memberDocRef) }
            }
        }
    }

    @Nested
    inner class UpdateGroup {

        @Test
        fun `updateGroup passes context in Firestore update map`() = runTest {
            val groupDocRef = mockGroupDocumentRef(testGroupId)
            every { groupDocRef.update(any<Map<String, Any?>>()) } returns Tasks.forResult(null)
            val group = Group(
                id = testGroupId,
                name = "Shared Flat",
                description = "Monthly bills",
                currency = "EUR",
                lastUpdatedAt = LocalDateTime.of(2026, 6, 1, 12, 0),
                context = GroupContext.EVERYDAY
            )

            dataSource.updateGroup(group)

            val mapSlot = slot<Map<String, Any?>>()
            verify(exactly = 1) { groupDocRef.update(capture(mapSlot)) }
            assertEquals("EVERYDAY", mapSlot.captured["context"])
        }
    }
}
