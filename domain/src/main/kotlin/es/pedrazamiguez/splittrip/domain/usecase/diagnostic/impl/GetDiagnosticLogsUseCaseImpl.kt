package es.pedrazamiguez.splittrip.domain.usecase.diagnostic.impl

import es.pedrazamiguez.splittrip.domain.repository.DiagnosticLogRepository
import es.pedrazamiguez.splittrip.domain.usecase.diagnostic.GetDiagnosticLogsUseCase

class GetDiagnosticLogsUseCaseImpl(
    private val diagnosticLogRepository: DiagnosticLogRepository
) : GetDiagnosticLogsUseCase {
    override fun invoke(): String = diagnosticLogRepository.getDiagnosticLogs()
}
