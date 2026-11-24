package es.pedrazamiguez.splittrip.update

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface AppUpdateUiState {
    data object Idle : AppUpdateUiState

    data class ForcedUpdateBlocked(
        val availableVersionCode: Long,
        val isRetrying: Boolean = false
    ) : AppUpdateUiState

    data class FlexibleUpdateDownloading(
        val bytesDownloaded: Long = 0L,
        val totalBytesToDownload: Long = 0L
    ) : AppUpdateUiState {
        val progressFraction: Float?
            get() = if (totalBytesToDownload > 0L) {
                val clampedBytes = bytesDownloaded.coerceIn(0L, totalBytesToDownload)
                BigDecimal.valueOf(clampedBytes)
                    .divide(
                        BigDecimal.valueOf(totalBytesToDownload),
                        PROGRESS_FRACTION_SCALE,
                        RoundingMode.HALF_UP
                    )
                    .toFloat()
                    .coerceIn(MIN_PROGRESS_FRACTION, MAX_PROGRESS_FRACTION)
            } else {
                null
            }

        val progressPercent: Int?
            get() = if (totalBytesToDownload > 0L) {
                val clampedBytes = bytesDownloaded.coerceIn(0L, totalBytesToDownload)
                BigDecimal.valueOf(clampedBytes)
                    .multiply(PERCENT_MULTIPLIER)
                    .divide(
                        BigDecimal.valueOf(totalBytesToDownload),
                        PERCENT_SCALE,
                        RoundingMode.HALF_UP
                    )
                    .toInt()
                    .coerceIn(MIN_PROGRESS_PERCENT, MAX_PROGRESS_PERCENT)
            } else {
                null
            }

        private companion object {
            const val PROGRESS_FRACTION_SCALE = 4
            const val PERCENT_SCALE = 0
            const val MIN_PROGRESS_FRACTION = 0f
            const val MAX_PROGRESS_FRACTION = 1f
            const val MIN_PROGRESS_PERCENT = 0
            const val MAX_PROGRESS_PERCENT = 100
            val PERCENT_MULTIPLIER: BigDecimal = BigDecimal.valueOf(100L)
        }
    }

    data object FlexibleUpdateReadyToInstall : AppUpdateUiState
}

sealed interface AppUpdateCoordinatorAction {
    data object OpenPlayStoreListing : AppUpdateCoordinatorAction
}
