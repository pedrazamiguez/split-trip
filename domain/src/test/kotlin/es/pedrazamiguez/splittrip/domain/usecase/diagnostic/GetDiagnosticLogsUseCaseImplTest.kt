package es.pedrazamiguez.splittrip.domain.usecase.diagnostic

import es.pedrazamiguez.splittrip.domain.repository.DiagnosticLogRepository
import es.pedrazamiguez.splittrip.domain.usecase.diagnostic.impl.GetDiagnosticLogsUseCaseImpl
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetDiagnosticLogsUseCaseImplTest {

    private val repository: DiagnosticLogRepository = mockk()
    private lateinit var useCase: GetDiagnosticLogsUseCaseImpl

    @BeforeEach
    fun setUp() {
        useCase = GetDiagnosticLogsUseCaseImpl(repository)
    }

    @Test
    fun `invoke delegates to repository getDiagnosticLogs and returns string`() {
        every { repository.getDiagnosticLogs() } returns "Diagnostic log output"

        val result = useCase()

        assertEquals("Diagnostic log output", result)
        verify { repository.getDiagnosticLogs() }
    }
}
