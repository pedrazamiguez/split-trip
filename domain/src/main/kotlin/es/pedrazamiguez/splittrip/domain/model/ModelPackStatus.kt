package es.pedrazamiguez.splittrip.domain.model

sealed interface ModelPackStatus {
    data class NotInstalled(
        val availableVersion: String,
        val downloadSizeBytes: Long,
        val packId: String = ""
    ) : ModelPackStatus

    data class InstallingRuntime(
        val progressPercent: Int? = null,
        val packId: String = ""
    ) : ModelPackStatus

    data class Downloading(
        val progressPercent: Int,
        val downloadedBytes: Long,
        val totalBytes: Long,
        val packId: String = ""
    ) : ModelPackStatus

    data class Verifying(
        val version: String,
        val packId: String = ""
    ) : ModelPackStatus

    data class Installed(
        val version: String,
        val sizeBytes: Long,
        val isUpdateAvailable: Boolean = false,
        val packId: String = ""
    ) : ModelPackStatus

    data class Incompatible(
        val reason: ModelPackIncompatibilityReason,
        val requiredRamMb: Int,
        val availableRamMb: Long,
        val requiredStorageBytes: Long,
        val availableStorageBytes: Long,
        val packId: String = ""
    ) : ModelPackStatus

    data class Error(
        val reason: ModelPackErrorReason,
        val canRetry: Boolean = true,
        val packId: String = ""
    ) : ModelPackStatus
}
