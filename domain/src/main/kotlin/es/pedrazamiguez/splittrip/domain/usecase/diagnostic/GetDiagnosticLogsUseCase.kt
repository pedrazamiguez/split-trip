package es.pedrazamiguez.splittrip.domain.usecase.diagnostic

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetDiagnosticLogsUseCase : UseCase {
    operator fun invoke(): String
}
