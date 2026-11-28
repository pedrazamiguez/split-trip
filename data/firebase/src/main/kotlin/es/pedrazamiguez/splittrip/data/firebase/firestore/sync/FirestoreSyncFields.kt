package es.pedrazamiguez.splittrip.data.firebase.firestore.sync

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import java.util.Date
import java.util.concurrent.TimeUnit

internal object FirestoreSyncFields {
    const val SERVER_UPDATED_AT = "serverUpdatedAt"
    const val IS_DELETED = "isDeleted"
    const val DELETED_AT = "deletedAt"
    const val EXPIRE_AT = "expireAt"
    const val LAST_UPDATED_BY = "lastUpdatedBy"
    const val GROUP_ID = "groupId"
    const val MAX_QUERY_LIMIT = 500L
    const val TOMBSTONE_TTL_DAYS = 60L

    fun buildSoftDeleteTombstone(
        groupId: String,
        deletedByUserId: String? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Map<String, Any> = buildMap {
        put(GROUP_ID, groupId)
        put(IS_DELETED, true)
        put(DELETED_AT, FieldValue.serverTimestamp())
        put(SERVER_UPDATED_AT, FieldValue.serverTimestamp())
        put(EXPIRE_AT, Timestamp(Date(nowMillis + TimeUnit.DAYS.toMillis(TOMBSTONE_TTL_DAYS))))
        if (!deletedByUserId.isNullOrBlank()) {
            put(LAST_UPDATED_BY, deletedByUserId)
        }
    }
}

internal fun Timestamp.toSyncWatermark(): SyncWatermark = SyncWatermark(
    seconds = seconds,
    nanoseconds = nanoseconds
)

internal fun SyncWatermark.toFirebaseTimestamp(): Timestamp = Timestamp(seconds, nanoseconds)
