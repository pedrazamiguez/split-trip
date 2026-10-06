package es.pedrazamiguez.splittrip.data.repository.impl

import es.pedrazamiguez.splittrip.core.logging.buffer.RollingLogBuffer
import es.pedrazamiguez.splittrip.domain.repository.DiagnosticLogRepository

class DiagnosticLogRepositoryImpl(
    private val rollingLogBuffer: RollingLogBuffer
) : DiagnosticLogRepository {
    override fun getDiagnosticLogs(): String = rollingLogBuffer.dumpToString()
}
