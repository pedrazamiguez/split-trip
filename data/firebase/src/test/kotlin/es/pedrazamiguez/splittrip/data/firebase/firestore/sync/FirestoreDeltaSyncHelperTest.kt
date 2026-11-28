package es.pedrazamiguez.splittrip.data.firebase.firestore.sync

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SnapshotMetadata
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirestoreDeltaSyncHelperTest {

    private fun mockDoc(
        id: String,
        serverUpdatedAt: Timestamp?,
        isDeleted: Boolean? = null
    ): DocumentSnapshot {
        val doc = mockk<DocumentSnapshot>(relaxed = true)
        every { doc.id } returns id
        every { doc.getTimestamp(FirestoreSyncFields.SERVER_UPDATED_AT) } returns serverUpdatedAt
        every { doc.getBoolean(FirestoreSyncFields.IS_DELETED) } returns isDeleted
        return doc
    }

    private fun mockSnapshot(
        docs: List<DocumentSnapshot>,
        isFromCache: Boolean = false,
        hasPendingWrites: Boolean = false
    ): QuerySnapshot {
        val metadata = mockk<SnapshotMetadata> {
            every { isFromCache() } returns isFromCache
            every { hasPendingWrites() } returns hasPendingWrites
        }
        return mockk<QuerySnapshot> {
            every { documents } returns docs
            every { getMetadata() } returns metadata
        }
    }

    @Test
    fun `parseSnapshotToDeltaBatch separates active upserts and tombstone deletedIds`() {
        val activeDoc = mockDoc("doc-1", Timestamp(100L, 10), isDeleted = false)
        val deletedDoc = mockDoc("doc-2", Timestamp(200L, 20), isDeleted = true)
        val snapshot = mockSnapshot(listOf(activeDoc, deletedDoc))

        val batch = parseSnapshotToDeltaBatch(
            snapshot = snapshot,
            isFullQuery = false,
            mapDocument = { it.id }
        )

        assertEquals(listOf("doc-1"), batch.upserts)
        assertEquals(listOf("doc-2"), batch.deletedIds)
    }

    @Test
    fun `parseSnapshotToDeltaBatch computes maxServerUpdatedAt across both active and deleted docs`() {
        val olderActive = mockDoc("doc-1", Timestamp(100L, 500), isDeleted = false)
        val newerDeleted = mockDoc("doc-2", Timestamp(250L, 999), isDeleted = true)
        val snapshot = mockSnapshot(listOf(olderActive, newerDeleted))

        val batch = parseSnapshotToDeltaBatch(
            snapshot = snapshot,
            isFullQuery = false,
            mapDocument = { it.id }
        )

        assertEquals(SyncWatermark(250L, 999), batch.maxServerUpdatedAt)
    }

    @Test
    fun `parseSnapshotToDeltaBatch sets isServerConfirmed false when from cache`() {
        val snapshot = mockSnapshot(emptyList(), isFromCache = true, hasPendingWrites = false)

        val batch = parseSnapshotToDeltaBatch(snapshot, isFullQuery = false, mapDocument = { it.id })

        assertFalse(batch.isServerConfirmed)
    }

    @Test
    fun `parseSnapshotToDeltaBatch sets isServerConfirmed false when has pending writes`() {
        val snapshot = mockSnapshot(emptyList(), isFromCache = false, hasPendingWrites = true)

        val batch = parseSnapshotToDeltaBatch(snapshot, isFullQuery = false, mapDocument = { it.id })

        assertFalse(batch.isServerConfirmed)
    }

    @Test
    fun `parseSnapshotToDeltaBatch sets isServerConfirmed true when online and no pending writes`() {
        val snapshot = mockSnapshot(emptyList(), isFromCache = false, hasPendingWrites = false)

        val batch = parseSnapshotToDeltaBatch(snapshot, isFullQuery = false, mapDocument = { it.id })

        assertTrue(batch.isServerConfirmed)
    }

    @Test
    fun `parseSnapshotToDeltaBatch sets reachedPageLimit true when docs equal maxQueryLimit`() {
        val docs = (1..500).map { idx ->
            mockDoc("doc-$idx", Timestamp(100L + idx, 0), isDeleted = false)
        }
        val snapshot = mockSnapshot(docs)

        val batch = parseSnapshotToDeltaBatch(snapshot, isFullQuery = true, mapDocument = { it.id })

        assertTrue(batch.reachedPageLimit)
        assertFalse(batch.isFullSnapshot)
    }

    @Test
    fun `observeCollectionDeltaFlow with watermark applies whereGreaterThan orderBy and limit500`() = runTest {
        val collectionRef = mockk<CollectionReference>()
        val afterWhere = mockk<Query>()
        val afterOrder = mockk<Query>()
        val afterLimit = mockk<Query>()
        val listenerReg = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        val watermark = SyncWatermark(seconds = 500L, nanoseconds = 123)

        every {
            collectionRef.whereGreaterThan(FirestoreSyncFields.SERVER_UPDATED_AT, Timestamp(500L, 123))
        } returns afterWhere
        every {
            afterWhere.orderBy(FirestoreSyncFields.SERVER_UPDATED_AT, Query.Direction.ASCENDING)
        } returns afterOrder
        every { afterOrder.limit(500L) } returns afterLimit
        every {
            afterLimit.addSnapshotListener(MetadataChanges.INCLUDE, capture(listenerSlot))
        } answers {
            val doc = mockDoc("doc-1", Timestamp(600L, 0), isDeleted = false)
            listenerSlot.captured.onEvent(mockSnapshot(listOf(doc)), null)
            listenerReg
        }

        val batch = observeCollectionDeltaFlow(
            collectionRef = collectionRef,
            sinceWatermark = watermark,
            entityLabel = "expenses",
            mapDocument = { it.id }
        ).first()

        assertEquals(listOf("doc-1"), batch.upserts)
        verify(exactly = 1) { afterOrder.limit(500L) }
    }

    @Test
    fun `observeCollectionDeltaFlow without watermark applies limit500 without where filter`() = runTest {
        val collectionRef = mockk<CollectionReference>()
        val afterLimit = mockk<Query>()
        val listenerReg = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<EventListener<QuerySnapshot>>()

        every { collectionRef.limit(500L) } returns afterLimit
        every {
            afterLimit.addSnapshotListener(MetadataChanges.INCLUDE, capture(listenerSlot))
        } answers {
            val doc = mockDoc("doc-1", Timestamp(100L, 0), isDeleted = false)
            listenerSlot.captured.onEvent(mockSnapshot(listOf(doc)), null)
            listenerReg
        }

        val batch = observeCollectionDeltaFlow(
            collectionRef = collectionRef,
            sinceWatermark = null,
            entityLabel = "expenses",
            mapDocument = { it.id }
        ).first()

        assertTrue(batch.isFullSnapshot)
        verify(exactly = 0) { collectionRef.whereGreaterThan(any<String>(), any()) }
        verify(exactly = 1) { collectionRef.limit(500L) }
    }

    @Test
    fun `fetchCollectionFullSnapshot paginates across multiple pages using startAfter`() = runTest {
        val collectionRef = mockk<CollectionReference>()
        val orderedQuery = mockk<Query>()
        val page1Query = mockk<Query>()
        val page2StartAfterQuery = mockk<Query>()

        val page1Docs = (1..500).map { idx ->
            mockDoc("doc-$idx", Timestamp(idx.toLong(), 0), isDeleted = false)
        }
        val lastDocPage1 = page1Docs.last()
        val page2Docs = listOf(mockDoc("doc-501", Timestamp(501L, 0), isDeleted = true))

        every { collectionRef.orderBy(FieldPath.documentId()) } returns orderedQuery
        every { orderedQuery.limit(500L) } returns page1Query
        every { page1Query.get() } returns Tasks.forResult(mockSnapshot(page1Docs))
        every { page1Query.startAfter(lastDocPage1) } returns page2StartAfterQuery
        every { page2StartAfterQuery.get() } returns Tasks.forResult(mockSnapshot(page2Docs))

        val result = fetchCollectionFullSnapshot(
            collectionRef = collectionRef,
            mapDocument = { it.id }
        )

        assertEquals(500, result.upserts.size)
        assertEquals(listOf("doc-501"), result.deletedIds)
        assertEquals(SyncWatermark(501L, 0), result.maxServerUpdatedAt)
        assertTrue(result.isFullSnapshot)
        assertFalse(result.reachedPageLimit)
    }

    @Test
    fun `fetchCollectionDelta paginates using advancing serverUpdatedAt cursor`() = runTest {
        val collectionRef = mockk<CollectionReference>()
        val startWatermark = SyncWatermark(100L, 0)
        val cursor2Watermark = SyncWatermark(600L, 0)

        val page1Where = mockk<Query>()
        val page1Order = mockk<Query>()
        val page1Limit = mockk<Query>()
        val page1Docs = (1..500).map { idx ->
            mockDoc("d-$idx", Timestamp(100L + idx, 0), isDeleted = false)
        }

        val page2Where = mockk<Query>()
        val page2Order = mockk<Query>()
        val page2Limit = mockk<Query>()
        val page2Docs = listOf(mockDoc("d-501", Timestamp(650L, 0), isDeleted = true))

        every {
            collectionRef.whereGreaterThan(
                FirestoreSyncFields.SERVER_UPDATED_AT,
                startWatermark.toFirebaseTimestamp()
            )
        } returns page1Where
        every { page1Where.orderBy(FirestoreSyncFields.SERVER_UPDATED_AT, Query.Direction.ASCENDING) } returns
            page1Order
        every { page1Order.limit(500L) } returns page1Limit
        every { page1Limit.get() } returns Tasks.forResult(mockSnapshot(page1Docs))

        every {
            collectionRef.whereGreaterThan(
                FirestoreSyncFields.SERVER_UPDATED_AT,
                cursor2Watermark.toFirebaseTimestamp()
            )
        } returns page2Where
        every { page2Where.orderBy(FirestoreSyncFields.SERVER_UPDATED_AT, Query.Direction.ASCENDING) } returns
            page2Order
        every { page2Order.limit(500L) } returns page2Limit
        every { page2Limit.get() } returns Tasks.forResult(mockSnapshot(page2Docs))

        val result = fetchCollectionDelta(
            collectionRef = collectionRef,
            sinceWatermark = startWatermark,
            mapDocument = { it.id }
        )

        assertEquals(500, result.upserts.size)
        assertEquals(listOf("d-501"), result.deletedIds)
        assertEquals(SyncWatermark(650L, 0), result.maxServerUpdatedAt)
        assertFalse(result.isFullSnapshot)
        assertFalse(result.reachedPageLimit)
    }

    @Test
    fun `buildSoftDeleteTombstone includes groupId isDeleted serverTimestamps and 60DayExpireAt`() {
        val nowMillis = 1_700_000_000_000L
        val tombstone = FirestoreSyncFields.buildSoftDeleteTombstone(
            groupId = "group-1",
            deletedByUserId = "user-1",
            nowMillis = nowMillis
        )

        assertEquals("group-1", tombstone[FirestoreSyncFields.GROUP_ID])
        assertEquals(true, tombstone[FirestoreSyncFields.IS_DELETED])
        assertEquals(FieldValue.serverTimestamp(), tombstone[FirestoreSyncFields.DELETED_AT])
        assertEquals(FieldValue.serverTimestamp(), tombstone[FirestoreSyncFields.SERVER_UPDATED_AT])
        assertEquals("user-1", tombstone[FirestoreSyncFields.LAST_UPDATED_BY])

        val expireAt = tombstone[FirestoreSyncFields.EXPIRE_AT] as Timestamp
        val expectedMillis = nowMillis + TimeUnit.DAYS.toMillis(60L)
        assertEquals(expectedMillis / 1000L, expireAt.seconds)
    }
}
