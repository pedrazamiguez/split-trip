package es.pedrazamiguez.splittrip.domain.model

data class ModelPackConfig(
    val packId: String,
    val displayName: String = "",
    val tier: ModelPackTier = ModelPackTier.BALANCED,
    val priority: Int = 0,
    val version: String,
    val downloadUrl: String,
    val sha256Checksum: String,
    val sizeBytes: Long,
    val minRamMb: Int,
    val minFreeStorageBytes: Long,
    val isEnabled: Boolean
)
