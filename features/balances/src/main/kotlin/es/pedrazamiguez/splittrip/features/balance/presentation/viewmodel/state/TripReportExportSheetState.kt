package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state

import androidx.compose.runtime.Immutable
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.LocalDate

@Immutable
data class TripReportExportSheetState(
    val isVisible: Boolean = false,
    val selectedPreset: SpendingPeriodPreset = SpendingPeriodPreset.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val formattedCustomStartDate: String = "",
    val formattedCustomEndDate: String = "",
    val oldestExpenseDate: LocalDate? = null,
    val newestExpenseDate: LocalDate? = null,
    val selectedFormat: TripReportFormat = TripReportFormat.PDF,
    val isCsvLocked: Boolean = false,
    val includesItemisedDetails: Boolean = true,
    val remainingFreeExports: Int? = null,
    val monthlyFreeExportLimit: Int? = null,
    val isExporting: Boolean = false
)
