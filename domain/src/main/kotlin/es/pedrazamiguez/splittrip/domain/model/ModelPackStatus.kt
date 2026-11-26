package es.pedrazamiguez.splittrip.domain.model

sealed interface ModelPackStatus {
    data class NotInstalled(
        val availableVersion: String,
        val downloadSizeBytes: Long
    ) : ModelPackStatus

    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : ModelPackStatus

    data class Verifying(
        val version: String
    ) : ModelPackStatus

    data class Installed(
        val version: String,
        val sizeBytes: Long,
        val isUpdateAvailable: Boolean = false
    ) : ModelPackStatus

    data class Incompatible(
        val reason: ModelPackIncompatibilityReason,
        val requiredRamMb: Int,
        val availableRamMb: Long,
        val requiredStorageBytes: Long,
        val availableStorageBytes: Long
    ) : ModelPackStatus

    data class Error(
        val reason: ModelPackErrorReason,
        val canRetry: Boolean = true
    ) : ModelPackStatus
}
