package es.pedrazamiguez.splittrip.core.logging.tree

import android.util.Log
import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RollingLogTreeTest {

    private lateinit var buffer: RollingLogBuffer
    private lateinit var tree: RollingLogTree

    @BeforeEach
    fun setUp() {
        buffer = RollingLogBuffer(maxCapacity = 10)
        tree = RollingLogTree(buffer = buffer, minPriority = Log.DEBUG)
    }

    @Test
    fun `log with priority at or above minPriority appends formatted entry to buffer`() {
        tree.log(Log.DEBUG, "CustomTag", "Debug message", null)

        val logs = buffer.getLogs()
        assertEquals(1, logs.size)
        assertTrue(logs.first().contains("[D/CustomTag] Debug message"))
    }

    @Test
    fun `log with priority below minPriority is ignored`() {
        tree.log(Log.VERBOSE, "CustomTag", "Verbose message", null)

        assertTrue(buffer.getLogs().isEmpty())
    }

    @Test
    fun `log with null tag defaults to SplitTrip`() {
        tree.log(priority = Log.INFO, tag = null, message = "Info message", t = null)

        val logs = buffer.getLogs()
        assertEquals(1, logs.size)
        assertTrue(logs.first().contains("[I/SplitTrip] Info message"))
    }

    @Test
    fun `log with Throwable appends stack trace to entry`() {
        val exception = IllegalArgumentException("Invalid state")
        tree.log(Log.ERROR, "ErrorTag", "Failure happened", exception)

        val logs = buffer.getLogs()
        assertEquals(1, logs.size)
        val entry = logs.first()
        assertTrue(entry.contains("[E/ErrorTag] Failure happened"))
        assertTrue(entry.contains("IllegalArgumentException: Invalid state"))
    }
}
