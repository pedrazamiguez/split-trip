package es.pedrazamiguez.splittrip.core.logging.exit

interface LastReportedExitTimestampStore {
    fun getLastReportedTimestampMillis(): Long
    fun setLastReportedTimestampMillis(timestampMillis: Long)
}
