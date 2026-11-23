package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface RecordReportExportUseCase : UseCase {
    suspend operator fun invoke()
}
