package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.event

import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.LocalDate

sealed interface BalancesUiEvent {

    // Balance animation
    data object BalanceAnimationComplete : BalancesUiEvent

    // Contribution actions flow
    data class ContributionActionsRequested(val contribution: ContributionUiModel) : BalancesUiEvent
    data object ContributionActionsDismissed : BalancesUiEvent
    data class DeleteContributionConfirmed(val contributionId: String) : BalancesUiEvent

    // Cash withdrawal delete flow
    data class DeleteWithdrawalRequested(val withdrawal: CashWithdrawalUiModel) : BalancesUiEvent
    data object DeleteWithdrawalDismissed : BalancesUiEvent
    data class DeleteWithdrawalConfirmed(val withdrawalId: String) : BalancesUiEvent

    // Statement export flow
    data object RequestExportReport : BalancesUiEvent
    data object DismissExportReportSheet : BalancesUiEvent
    data class SelectExportPeriodPreset(val preset: SpendingPeriodPreset) : BalancesUiEvent
    data class SelectExportCustomStartDate(val date: LocalDate) : BalancesUiEvent
    data class SelectExportCustomEndDate(val date: LocalDate) : BalancesUiEvent
    data class SelectExportFormat(val format: TripReportFormat) : BalancesUiEvent
    data object ConfirmExportReport : BalancesUiEvent
}
