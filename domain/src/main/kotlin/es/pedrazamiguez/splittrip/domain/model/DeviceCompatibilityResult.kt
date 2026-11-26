package es.pedrazamiguez.splittrip.domain.model

enum class ModelPackIncompatibilityReason {
    UNSUPPORTED_ABI,
    INSUFFICIENT_RAM,
    INSUFFICIENT_STORAGE,
    PACK_DISABLED
}

data class DeviceCompatibilityResult(
    val isCompatible: Boolean,
    val reason: ModelPackIncompatibilityReason? = null,
    val availableRamMb: Long,
    val requiredRamMb: Int,
    val availableStorageBytes: Long,
    val requiredStorageBytes: Long,
    val supportedAbis: List<String>
)
