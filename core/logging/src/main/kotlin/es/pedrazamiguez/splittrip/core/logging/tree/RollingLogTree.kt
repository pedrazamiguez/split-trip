package es.pedrazamiguez.splittrip.core.logging.tree

import android.util.Log
import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import timber.log.Timber

class RollingLogTree(
    private val buffer: RollingLogBuffer,
    private val minPriority: Int = Log.DEBUG
) : Timber.Tree() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    public override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < minPriority) return

        val timestamp = synchronized(dateFormat) {
            dateFormat.format(Date())
        }
        val level = priorityToString(priority)
        val formattedTag = tag ?: "SplitTrip"
        val fullMessage = if (t != null) {
            val stackTrace = t.stackTraceToString().trimEnd()
            if (stackTrace.isNotEmpty()) {
                "$message\n$stackTrace"
            } else {
                message
            }
        } else {
            message
        }

        buffer.append("[$timestamp] [$level/$formattedTag] $fullMessage")
    }

    private fun priorityToString(priority: Int): String = when (priority) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
    }
}
