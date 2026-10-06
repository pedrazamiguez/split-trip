package es.pedrazamiguez.splittrip.core.logging.buffer

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RollingLogBufferTest {

    @Test
    fun `append stores sanitized log line in buffer`() {
        val buffer = RollingLogBuffer(maxCapacity = 10)
        buffer.append("System initialized")

        assertEquals(1, buffer.size)
        assertEquals(listOf("System initialized"), buffer.getLogs())
    }

    @Test
    fun `append sanitizes email PII in log lines`() {
        val buffer = RollingLogBuffer(maxCapacity = 10)
        buffer.append("User test@example.com logged in")

        assertEquals(listOf("User t***t@e***e.com logged in"), buffer.getLogs())
    }

    @Test
    fun `buffer maintains FIFO order and evicts oldest line when exceeding capacity`() {
        val buffer = RollingLogBuffer(maxCapacity = 3)
        buffer.append("line 1")
        buffer.append("line 2")
        buffer.append("line 3")
        buffer.append("line 4")

        assertEquals(3, buffer.size)
        assertEquals(listOf("line 2", "line 3", "line 4"), buffer.getLogs())
    }

    @Test
    fun `dumpToString joins buffered lines with newline`() {
        val buffer = RollingLogBuffer(maxCapacity = 5)
        buffer.append("first")
        buffer.append("second")

        assertEquals("first\nsecond", buffer.dumpToString())
    }

    @Test
    fun `clear removes all buffered lines`() {
        val buffer = RollingLogBuffer(maxCapacity = 5)
        buffer.append("first")
        buffer.append("second")
        buffer.clear()

        assertEquals(0, buffer.size)
        assertTrue(buffer.getLogs().isEmpty())
        assertEquals("", buffer.dumpToString())
    }

    @Test
    fun `concurrent appends from multiple threads do not drop entries or throw ConcurrentModificationException`() {
        val buffer = RollingLogBuffer(maxCapacity = 1000)
        val threadCount = 10
        val entriesPerThread = 50
        val executor = Executors.newFixedThreadPool(threadCount)

        for (i in 0 until threadCount) {
            executor.submit {
                for (j in 0 until entriesPerThread) {
                    buffer.append("Thread $i log $j")
                }
            }
        }

        executor.shutdown()
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS))
        assertEquals(500, buffer.size)
    }
}
