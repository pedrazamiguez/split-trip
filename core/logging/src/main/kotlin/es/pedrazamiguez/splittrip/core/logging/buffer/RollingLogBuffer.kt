package es.pedrazamiguez.splittrip.core.logging.buffer

import es.pedrazamiguez.splittrip.core.logging.sanitizer.sanitizePii

class RollingLogBuffer(
    private val maxCapacity: Int = DEFAULT_MAX_CAPACITY
) {
    private val lock = Any()
    private val buffer = ArrayDeque<String>()

    val size: Int
        get() = synchronized(lock) { buffer.size }

    fun append(logLine: String) {
        val sanitized = logLine.sanitizePii()
        synchronized(lock) {
            while (buffer.size >= maxCapacity) {
                buffer.removeFirst()
            }
            buffer.addLast(sanitized)
        }
    }

    fun getLogs(): List<String> = synchronized(lock) {
        buffer.toList()
    }

    fun dumpToString(): String = synchronized(lock) {
        buffer.joinToString("\n")
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
        }
    }

    companion object {
        const val DEFAULT_MAX_CAPACITY = 500
    }
}
