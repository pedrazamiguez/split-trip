package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.features.balance.presentation.model.EntityBreakdownMode
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.LocalDate

sealed interface SpendingInsightsUiEvent {
    data class SelectPeriodPreset(val preset: SpendingPeriodPreset) : SpendingInsightsUiEvent
    data class SelectCustomStartDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectCustomEndDate(val date: LocalDate) : SpendingInsightsUiEvent
    data class SelectDayBar(val barId: String) : SpendingInsightsUiEvent
    data class SelectEntityBreakdownMode(val mode: EntityBreakdownMode) : SpendingInsightsUiEvent
    data object NavigateBack : SpendingInsightsUiEvent
}
