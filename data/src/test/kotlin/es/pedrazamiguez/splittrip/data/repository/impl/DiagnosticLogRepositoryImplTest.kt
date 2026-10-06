package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiagnosticLogRepositoryImplTest {

    private val rollingLogBuffer: RollingLogBuffer = mockk()
    private lateinit var repository: DiagnosticLogRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = DiagnosticLogRepositoryImpl(rollingLogBuffer)
    }

    @Test
    fun `getDiagnosticLogs delegates to rollingLogBuffer dumpToString`() {
        every { rollingLogBuffer.dumpToString() } returns "dumped logs"

        val result = repository.getDiagnosticLogs()

        assertEquals("dumped logs", result)
        verify { rollingLogBuffer.dumpToString() }
    }
}
