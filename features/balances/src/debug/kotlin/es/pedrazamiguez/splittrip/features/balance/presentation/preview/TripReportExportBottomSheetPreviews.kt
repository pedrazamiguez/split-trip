package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewComplete
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemeWrapper
import es.pedrazamiguez.splittrip.core.designsystem.preview.PreviewThemes
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.presentation.component.export.TripReportExportBottomSheet
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import java.time.LocalDate

@PreviewComplete
@Composable
private fun TripReportExportBottomSheetDefaultPreview() {
    PreviewThemeWrapper {
        TripReportExportBottomSheet(
            sheetState = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = SpendingPeriodPreset.ALL_TIME,
                selectedFormat = TripReportFormat.PDF,
                isCsvLocked = false,
                includesItemisedDetails = true
            ),
            onSelectPreset = {},
            onSelectCustomStartDate = {},
            onSelectCustomEndDate = {},
            onSelectFormat = {},
            onConfirmExport = {},
            onSaveToDevice = {},
            onDismiss = {}
        )
    }
}

@PreviewThemes
@Composable
private fun TripReportExportBottomSheetFreeTierPreview() {
    PreviewThemeWrapper {
        TripReportExportBottomSheet(
            sheetState = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = SpendingPeriodPreset.ALL_TIME,
                selectedFormat = TripReportFormat.PDF,
                isCsvLocked = true,
                includesItemisedDetails = false,
                remainingFreeExports = 1,
                monthlyFreeExportLimit = 1
            ),
            onSelectPreset = {},
            onSelectCustomStartDate = {},
            onSelectCustomEndDate = {},
            onSelectFormat = {},
            onConfirmExport = {},
            onSaveToDevice = {},
            onDismiss = {}
        )
    }
}

@PreviewThemes
@Composable
private fun TripReportExportBottomSheetCustomCsvPreview() {
    PreviewThemeWrapper {
        TripReportExportBottomSheet(
            sheetState = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = SpendingPeriodPreset.CUSTOM,
                customStartDate = LocalDate.of(2026, 11, 1),
                customEndDate = LocalDate.of(2026, 11, 20),
                formattedCustomStartDate = "1 Nov",
                formattedCustomEndDate = "20 Nov",
                selectedFormat = TripReportFormat.CSV,
                isCsvLocked = false,
                includesItemisedDetails = true
            ),
            onSelectPreset = {},
            onSelectCustomStartDate = {},
            onSelectCustomEndDate = {},
            onSelectFormat = {},
            onConfirmExport = {},
            onSaveToDevice = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Free - Compact",
    widthDp = 360,
    heightDp = 640,
    fontScale = 1.3f,
    showBackground = true
)
@Composable
private fun TripReportExportBottomSheetFreeTierCompactPreview() {
    PreviewThemeWrapper {
        TripReportExportBottomSheet(
            sheetState = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = SpendingPeriodPreset.ALL_TIME,
                selectedFormat = TripReportFormat.PDF,
                isCsvLocked = true,
                includesItemisedDetails = false,
                remainingFreeExports = 1,
                monthlyFreeExportLimit = 1
            ),
            onSelectPreset = {},
            onSelectCustomStartDate = {},
            onSelectCustomEndDate = {},
            onSelectFormat = {},
            onConfirmExport = {},
            onSaveToDevice = {},
            onDismiss = {}
        )
    }
}

@Preview(
    name = "Pro Custom - Compact",
    widthDp = 360,
    heightDp = 640,
    showBackground = true
)
@Composable
private fun TripReportExportBottomSheetCustomProCompactPreview() {
    PreviewThemeWrapper {
        TripReportExportBottomSheet(
            sheetState = TripReportExportSheetState(
                isVisible = true,
                selectedPreset = SpendingPeriodPreset.CUSTOM,
                customStartDate = LocalDate.of(2026, 11, 1),
                customEndDate = LocalDate.of(2026, 11, 20),
                formattedCustomStartDate = "1 Nov",
                formattedCustomEndDate = "20 Nov",
                selectedFormat = TripReportFormat.CSV,
                isCsvLocked = false,
                includesItemisedDetails = true
            ),
            onSelectPreset = {},
            onSelectCustomStartDate = {},
            onSelectCustomEndDate = {},
            onSelectFormat = {},
            onConfirmExport = {},
            onSaveToDevice = {},
            onDismiss = {}
        )
    }
}
