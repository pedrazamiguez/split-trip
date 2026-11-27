package es.pedrazamiguez.splittrip.core.logging.exit

import es.pedrazamiguez.splittrip.core.logging.exception.AbnormalProcessExitException
import es.pedrazamiguez.splittrip.core.logging.recordDiagnosticException
import es.pedrazamiguez.splittrip.core.logging.sanitizer.sanitizePii
import timber.log.Timber

class ProcessExitDiagnosticsReporter(
    private val exitInfoProvider: ProcessExitInfoProvider,
    private val timestampStore: LastReportedExitTimestampStore,
    private val diagnosticRecorder: (Throwable, Map<String, String>) -> Unit = ::recordDiagnosticException
) {

    fun reportRecentAbnormalExits(): Int {
        val lastReportedTimestampMs = timestampStore.getLastReportedTimestampMillis()
        val newExits = exitInfoProvider.getRecentAbnormalExits(MAX_HISTORICAL_EXITS)
            .filter { it.timestampMillis > lastReportedTimestampMs }
            .sortedBy { it.timestampMillis }

        if (newExits.isEmpty()) {
            return 0
        }

        newExits.forEach { exit ->
            val sanitizedDescription = exit.description
                ?.takeIf { it.isNotBlank() }
                ?.sanitizePii()
                ?.take(MAX_DESCRIPTION_CHARS)
                ?: DESCRIPTION_FALLBACK

            val message =
                "Previous process terminated abnormally: " +
                    "reason=${exit.reason.label} " +
                    "status=${exit.status} " +
                    "importance=${exit.importance} " +
                    "pssKb=${exit.pssKb} " +
                    "rssKb=${exit.rssKb} " +
                    "desc=$sanitizedDescription"

            Timber.w(message)

            val customKeys = mapOf(
                KEY_REASON to exit.reason.label,
                KEY_STATUS to exit.status.toString(),
                KEY_IMPORTANCE to exit.importance.toString(),
                KEY_PSS_KB to exit.pssKb.toString(),
                KEY_RSS_KB to exit.rssKb.toString(),
                KEY_TIMESTAMP_MS to exit.timestampMillis.toString()
            )
            diagnosticRecorder(AbnormalProcessExitException(message), customKeys)
        }

        timestampStore.setLastReportedTimestampMillis(newExits.maxOf { it.timestampMillis })
        return newExits.size
    }

    private companion object {
        private const val MAX_HISTORICAL_EXITS = 10
        private const val MAX_DESCRIPTION_CHARS = 200
        private const val DESCRIPTION_FALLBACK = "none"
        private const val KEY_REASON = "last_abnormal_exit_reason"
        private const val KEY_STATUS = "last_abnormal_exit_status"
        private const val KEY_IMPORTANCE = "last_abnormal_exit_importance"
        private const val KEY_PSS_KB = "last_abnormal_exit_pss_kb"
        private const val KEY_RSS_KB = "last_abnormal_exit_rss_kb"
        private const val KEY_TIMESTAMP_MS = "last_abnormal_exit_timestamp_ms"
    }
}
