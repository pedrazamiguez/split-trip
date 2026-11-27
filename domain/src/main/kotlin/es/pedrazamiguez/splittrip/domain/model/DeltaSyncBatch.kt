package es.pedrazamiguez.splittrip.domain.model

data class DeltaSyncBatch<T>(
    val upserts: List<T>,
    val deletedIds: List<String>,
    val maxServerUpdatedAt: SyncWatermark?,
    val isServerConfirmed: Boolean,
    val isFullSnapshot: Boolean,
    val reachedPageLimit: Boolean = false
)
