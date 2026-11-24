package es.pedrazamiguez.splittrip.update

data class PlayUpdateSnapshot(
    val availability: PlayUpdateAvailability,
    val availableVersionCode: Long,
    val isImmediateAllowed: Boolean,
    val isFlexibleAllowed: Boolean,
    val installStatus: PlayInstallStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytesToDownload: Long = 0L
)

data class PlayInstallState(
    val status: PlayInstallStatus,
    val bytesDownloaded: Long = 0L,
    val totalBytesToDownload: Long = 0L
)

enum class PlayUpdateAvailability {
    UNKNOWN,
    UPDATE_NOT_AVAILABLE,
    UPDATE_AVAILABLE,
    DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
}

enum class PlayInstallStatus {
    UNKNOWN,
    PENDING,
    DOWNLOADING,
    DOWNLOADED,
    INSTALLING,
    INSTALLED,
    FAILED,
    CANCELED
}

enum class PlayUpdateFlowType {
    IMMEDIATE,
    FLEXIBLE
}
