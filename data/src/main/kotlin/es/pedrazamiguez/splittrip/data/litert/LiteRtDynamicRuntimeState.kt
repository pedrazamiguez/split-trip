package es.pedrazamiguez.splittrip.data.litert

import es.pedrazamiguez.splittrip.domain.model.ModelPackErrorReason

internal sealed interface LiteRtDynamicRuntimeState {
    data object NotInstalled : LiteRtDynamicRuntimeState
    data object Pending : LiteRtDynamicRuntimeState
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long
    ) : LiteRtDynamicRuntimeState
    data object Installing : LiteRtDynamicRuntimeState
    data object Installed : LiteRtDynamicRuntimeState
    data class Failed(
        val reason: ModelPackErrorReason,
        val errorCode: Int = 0
    ) : LiteRtDynamicRuntimeState
}

internal fun LiteRtDynamicRuntimeState.toProgressPercent(): Int? = when (this) {
    is LiteRtDynamicRuntimeState.Downloading -> if (totalBytesToDownload > 0L) {
        ((bytesDownloaded.coerceAtLeast(0L) * PERCENT_SCALE) / totalBytesToDownload)
            .toInt()
            .coerceIn(0, PERCENT_SCALE)
    } else {
        null
    }
    LiteRtDynamicRuntimeState.Pending,
    LiteRtDynamicRuntimeState.Installing,
    LiteRtDynamicRuntimeState.NotInstalled,
    LiteRtDynamicRuntimeState.Installed,
    is LiteRtDynamicRuntimeState.Failed -> null
}

internal fun LiteRtDynamicRuntimeState.isActiveInstall(): Boolean =
    this is LiteRtDynamicRuntimeState.Pending ||
        this is LiteRtDynamicRuntimeState.Downloading ||
        this is LiteRtDynamicRuntimeState.Installing

private const val PERCENT_SCALE = 100
