package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.LocalDate

sealed interface SpendingInsightsUiEvent {
    data class SelectPeriodPreset(val preset: SpendingPeriodPreset) : SpendingInsightsUiEvent
    data class SelectCustomStartDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectCustomEndDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectDayBar(val barId: String) : SpendingInsightsUiEvent
    data class SelectEntityBreakdownMode(val mode: EntityBreakdownMode) : SpendingInsightsUiEvent
    data object ClearExpenseFilters : SpendingInsightsUiEvent
    data object NavigateBack : SpendingInsightsUiEvent
    data object RequestExportReport : SpendingInsightsUiEvent
    data object DismissExportReportSheet : SpendingInsightsUiEvent
    data class SelectExportPeriodPreset(val preset: SpendingPeriodPreset) : SpendingInsightsUiEvent
    data class SelectExportCustomStartDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectExportCustomEndDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectExportFormat(val format: TripReportFormat) : SpendingInsightsUiEvent
    data object ConfirmExportReport : SpendingInsightsUiEvent
    data object SaveExportReport : SpendingInsightsUiEvent
    data class CompleteSaveExportedReport(val destinationUri: String) : SpendingInsightsUiEvent
    data object CancelSaveExportedReport : SpendingInsightsUiEvent
}
