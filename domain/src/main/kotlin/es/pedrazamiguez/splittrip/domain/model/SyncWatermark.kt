package es.pedrazamiguez.splittrip.domain.model

data class SyncWatermark(
    val seconds: Long,
    val nanoseconds: Int
) : Comparable<SyncWatermark> {

    override fun compareTo(other: SyncWatermark): Int {
        val secondsComparison = seconds.compareTo(other.seconds)
        return if (secondsComparison != 0) {
            secondsComparison
        } else {
            nanoseconds.compareTo(other.nanoseconds)
        }
    }

    fun toEpochMillis(): Long = (seconds * MILLIS_PER_SECOND) + (nanoseconds / NANOS_PER_MILLI)

    companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val NANOS_PER_MILLI = 1_000_000
    }
}
