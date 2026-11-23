package es.pedrazamiguez.splittrip.domain.usecase.balance

import es.pedrazamiguez.splittrip.domain.model.SpendingInsightsPeriod
import es.pedrazamiguez.splittrip.domain.model.report.GroupTripReport
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import java.time.LocalDate
import java.time.LocalDateTime

interface BuildGroupTripReportUseCase : UseCase {
    suspend operator fun invoke(
        groupId: String,
        period: SpendingInsightsPeriod = SpendingInsightsPeriod.AllTime,
        today: LocalDate = LocalDate.now(),
        generatedAt: LocalDateTime = LocalDateTime.now()
    ): GroupTripReport
}
