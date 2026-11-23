package es.pedrazamiguez.splittrip.domain.usecase.balance.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.usecase.balance.RecordReportExportUseCase
import java.time.YearMonth

class RecordReportExportUseCaseImpl(
    private val userPreferenceRepository: UserPreferenceRepository,
    private val yearMonthProvider: () -> YearMonth = { YearMonth.now() }
) : RecordReportExportUseCase {

    override suspend operator fun invoke() {
        userPreferenceRepository.incrementMonthlyReportExportCount(yearMonthProvider())
    }
}
