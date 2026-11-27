package es.pedrazamiguez.splittrip.data.firebase.firestore.sync

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber

internal fun <T> parseSnapshotToDeltaBatch(
    snapshot: QuerySnapshot,
    isFullQuery: Boolean,
    mapDocument: (DocumentSnapshot) -> T?,
    sort: (List<T>) -> List<T> = { it }
): DeltaSyncBatch<T> {
    val upserts = mutableListOf<T>()
    val deletedIds = mutableListOf<String>()
    var maxWatermark: SyncWatermark? = null

    for (doc in snapshot.documents) {
        val docWatermark = doc.getTimestamp(FirestoreSyncFields.SERVER_UPDATED_AT)?.toSyncWatermark()
        if (docWatermark != null && (maxWatermark == null || docWatermark > maxWatermark)) {
            maxWatermark = docWatermark
        }
        if (doc.getBoolean(FirestoreSyncFields.IS_DELETED) == true) {
            deletedIds.add(doc.id)
        } else {
            mapDocument(doc)?.let(upserts::add)
        }
    }

    val isServerConfirmed = !snapshot.metadata.isFromCache && !snapshot.metadata.hasPendingWrites()
    val reachedPageLimit = snapshot.documents.size >= FirestoreSyncFields.MAX_QUERY_LIMIT
    val isFullSnapshot = isFullQuery && !reachedPageLimit

    return DeltaSyncBatch(
        upserts = sort(upserts),
        deletedIds = deletedIds,
        maxServerUpdatedAt = maxWatermark,
        isServerConfirmed = isServerConfirmed,
        isFullSnapshot = isFullSnapshot,
        reachedPageLimit = reachedPageLimit
    )
}

internal suspend fun <T> fetchCollectionFullSnapshot(
    collectionRef: CollectionReference,
    mapDocument: (DocumentSnapshot) -> T?,
    sort: (List<T>) -> List<T> = { it }
): DeltaSyncBatch<T> {
    val allUpserts = mutableListOf<T>()
    val allDeletedIds = mutableListOf<String>()
    var maxWatermark: SyncWatermark? = null
    var anyPageFromCache = false
    var anyPageHasPendingWrites = false
    var lastDoc: DocumentSnapshot? = null

    do {
        var query = collectionRef
            .orderBy(FieldPath.documentId())
            .limit(FirestoreSyncFields.MAX_QUERY_LIMIT)
        if (lastDoc != null) {
            query = query.startAfter(lastDoc)
        }
        val page = query.get().await()
        if (page.metadata.isFromCache) anyPageFromCache = true
        if (page.metadata.hasPendingWrites()) anyPageHasPendingWrites = true

        val pageBatch = parseSnapshotToDeltaBatch(
            snapshot = page,
            isFullQuery = true,
            mapDocument = mapDocument
        )
        allUpserts.addAll(pageBatch.upserts)
        allDeletedIds.addAll(pageBatch.deletedIds)
        maxWatermark = maxOfWatermark(maxWatermark, pageBatch.maxServerUpdatedAt)

        lastDoc = page.documents.lastOrNull()
    } while (page.documents.size >= FirestoreSyncFields.MAX_QUERY_LIMIT && lastDoc != null)

    return DeltaSyncBatch(
        upserts = sort(allUpserts),
        deletedIds = allDeletedIds,
        maxServerUpdatedAt = maxWatermark,
        isServerConfirmed = !anyPageFromCache && !anyPageHasPendingWrites,
        isFullSnapshot = true,
        reachedPageLimit = false
    )
}

internal suspend fun <T> fetchCollectionDelta(
    collectionRef: CollectionReference,
    sinceWatermark: SyncWatermark,
    mapDocument: (DocumentSnapshot) -> T?,
    sort: (List<T>) -> List<T> = { it }
): DeltaSyncBatch<T> {
    val allUpserts = mutableListOf<T>()
    val allDeletedIds = mutableListOf<String>()
    var maxWatermark: SyncWatermark? = null
    var anyPageFromCache = false
    var anyPageHasPendingWrites = false
    var cursor = sinceWatermark

    while (true) {
        val page = collectionRef
            .whereGreaterThan(FirestoreSyncFields.SERVER_UPDATED_AT, cursor.toFirebaseTimestamp())
            .orderBy(FirestoreSyncFields.SERVER_UPDATED_AT, Query.Direction.ASCENDING)
            .limit(FirestoreSyncFields.MAX_QUERY_LIMIT)
            .get()
            .await()

        if (page.metadata.isFromCache) anyPageFromCache = true
        if (page.metadata.hasPendingWrites()) anyPageHasPendingWrites = true

        val pageBatch = parseSnapshotToDeltaBatch(
            snapshot = page,
            isFullQuery = false,
            mapDocument = mapDocument
        )
        allUpserts.addAll(pageBatch.upserts)
        allDeletedIds.addAll(pageBatch.deletedIds)
        maxWatermark = maxOfWatermark(maxWatermark, pageBatch.maxServerUpdatedAt)

        val nextCursor = pageBatch.maxServerUpdatedAt
        val shouldContinue = page.documents.size >= FirestoreSyncFields.MAX_QUERY_LIMIT &&
            nextCursor != null &&
            nextCursor > cursor
        if (!shouldContinue) break
        cursor = nextCursor!!
    }

    return DeltaSyncBatch(
        upserts = sort(allUpserts),
        deletedIds = allDeletedIds,
        maxServerUpdatedAt = maxWatermark,
        isServerConfirmed = !anyPageFromCache && !anyPageHasPendingWrites,
        isFullSnapshot = false,
        reachedPageLimit = false
    )
}

internal fun <T> observeCollectionDeltaFlow(
    collectionRef: CollectionReference,
    sinceWatermark: SyncWatermark?,
    entityLabel: String,
    mapDocument: (DocumentSnapshot) -> T?,
    sort: (List<T>) -> List<T> = { it }
): Flow<DeltaSyncBatch<T>> = callbackFlow {
    val query = if (sinceWatermark != null) {
        collectionRef
            .whereGreaterThan(FirestoreSyncFields.SERVER_UPDATED_AT, sinceWatermark.toFirebaseTimestamp())
            .orderBy(FirestoreSyncFields.SERVER_UPDATED_AT, Query.Direction.ASCENDING)
            .limit(FirestoreSyncFields.MAX_QUERY_LIMIT)
    } else {
        collectionRef.limit(FirestoreSyncFields.MAX_QUERY_LIMIT)
    }

    val listener = query.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
            Timber.e(error, "Error listening to $entityLabel")
            return@addSnapshotListener
        }
        if (snapshot != null) {
            trySend(
                parseSnapshotToDeltaBatch(
                    snapshot = snapshot,
                    isFullQuery = sinceWatermark == null,
                    mapDocument = mapDocument,
                    sort = sort
                )
            )
        }
    }
    awaitClose { listener.remove() }
}

private fun maxOfWatermark(current: SyncWatermark?, candidate: SyncWatermark?): SyncWatermark? =
    when {
        candidate == null -> current
        current == null || candidate > current -> candidate
        else -> current
    }
