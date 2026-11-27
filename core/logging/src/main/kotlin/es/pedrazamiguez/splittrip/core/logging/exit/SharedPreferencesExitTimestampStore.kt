package es.pedrazamiguez.splittrip.core.logging.exit

import android.content.Context

class SharedPreferencesExitTimestampStore(
    private val context: Context
) : LastReportedExitTimestampStore {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun getLastReportedTimestampMillis(): Long =
        prefs.getLong(KEY_LAST_REPORTED_TIMESTAMP_MS, 0L)

    override fun setLastReportedTimestampMillis(timestampMillis: Long) {
        prefs.edit().putLong(KEY_LAST_REPORTED_TIMESTAMP_MS, timestampMillis).apply()
    }

    private companion object {
        private const val PREFS_NAME = "splittrip_process_exit_diagnostics"
        private const val KEY_LAST_REPORTED_TIMESTAMP_MS = "last_reported_exit_timestamp_ms"
    }
}
