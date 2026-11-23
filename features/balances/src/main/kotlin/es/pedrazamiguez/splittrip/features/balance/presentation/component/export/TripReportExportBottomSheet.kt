package es.pedrazamiguez.splittrip.features.balance.presentation.component.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Share
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTag
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportTagStyle
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.GradientButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.form.SecondaryButton
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.SegmentedPillSelector
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.HorizonModalBottomSheet
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.sheet.rememberHorizonBottomSheetState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SheetTitleText
import es.pedrazamiguez.splittrip.domain.model.report.TripReportFormat
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.component.insights.SpendingInsightsPeriodCard
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state.TripReportExportSheetState
import java.time.LocalDate
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
fun TripReportExportBottomSheet(
    sheetState: TripReportExportSheetState,
    onSelectPreset: (SpendingPeriodPreset) -> Unit,
    onSelectCustomStartDate: (LocalDate) -> Unit,
    onSelectCustomEndDate: (LocalDate) -> Unit,
    onSelectFormat: (TripReportFormat) -> Unit,
    onConfirmExport: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modalSheetState = rememberHorizonBottomSheetState()
    val scope = rememberCoroutineScope()
    val formats = remember { TripReportFormat.entries.toImmutableList() }
    val pdfLabel = stringResource(R.string.balances_export_format_pdf)
    val csvLabel = stringResource(resolveCsvFormatLabelRes(sheetState.isCsvLocked))
    val subtitleText = stringResource(resolveExportSubtitleRes(sheetState.includesItemisedDetails))

    HorizonModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalSheetState
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Default)
        ) {
            SheetTitleText(text = stringResource(R.string.balances_export_sheet_title))

            SecondaryBodyText(
                text = subtitleText,
                maxLines = Int.MAX_VALUE
            )

            resolveFreeQuotaDisplay(sheetState)?.let { quota ->
                PassportTag(
                    text = pluralStringResource(
                        R.plurals.balances_export_free_quota_badge,
                        quota.remaining,
                        quota.remaining,
                        quota.monthlyLimit
                    ),
                    style = resolveQuotaTagStyle(quota.remaining)
                )
            }

            LabelText(text = stringResource(R.string.balances_export_period_label))

            SpendingInsightsPeriodCard(
                selectedPreset = sheetState.selectedPreset,
                periodSummaryLabel = "",
                customStartDate = sheetState.customStartDate,
                customEndDate = sheetState.customEndDate,
                formattedCustomStartDate = sheetState.formattedCustomStartDate,
                formattedCustomEndDate = sheetState.formattedCustomEndDate,
                oldestExpenseDate = sheetState.oldestExpenseDate,
                newestExpenseDate = sheetState.newestExpenseDate,
                onSelectPreset = onSelectPreset,
                onSelectCustomStartDate = onSelectCustomStartDate,
                onSelectCustomEndDate = onSelectCustomEndDate
            )

            LabelText(text = stringResource(R.string.balances_export_format_label))

            SegmentedPillSelector(
                options = formats,
                selectedOption = sheetState.selectedFormat,
                onOptionSelected = onSelectFormat,
                optionLabel = { format -> resolveFormatOptionLabel(format, pdfLabel, csvLabel) },
                isOptionLocked = { format -> format == TripReportFormat.CSV && sheetState.isCsvLocked },
                enabled = !sheetState.isExporting
            )

            if (!sheetState.includesItemisedDetails) {
                SecondaryBodyText(
                    text = stringResource(R.string.balances_export_free_pro_hint),
                    maxLines = Int.MAX_VALUE
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))

            GradientButton(
                text = stringResource(R.string.balances_export_confirm_button),
                onClick = onConfirmExport,
                leadingIcon = TablerIcons.Outline.Share,
                isLoading = sheetState.isExporting,
                enabled = !sheetState.isExporting,
                modifier = Modifier.fillMaxWidth()
            )

            SecondaryButton(
                text = stringResource(R.string.balances_export_cancel_button),
                onClick = {
                    scope.launch { modalSheetState.hide() }.invokeOnCompletion { onDismiss() }
                },
                enabled = !sheetState.isExporting,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun resolveCsvFormatLabelRes(isCsvLocked: Boolean): Int =
    if (isCsvLocked) R.string.balances_export_format_csv_pro else R.string.balances_export_format_csv

private fun resolveExportSubtitleRes(includesItemisedDetails: Boolean): Int = if (includesItemisedDetails) {
    R.string.balances_export_sheet_subtitle
} else {
    R.string.balances_export_sheet_subtitle_free
}

private data class FreeExportQuotaDisplay(
    val remaining: Int,
    val monthlyLimit: Int
)

private fun resolveFreeQuotaDisplay(sheetState: TripReportExportSheetState): FreeExportQuotaDisplay? {
    val remaining = sheetState.remainingFreeExports ?: return null
    val monthlyLimit = sheetState.monthlyFreeExportLimit ?: return null
    return FreeExportQuotaDisplay(remaining = remaining, monthlyLimit = monthlyLimit)
}

private fun resolveQuotaTagStyle(remaining: Int): PassportTagStyle =
    if (remaining > 0) PassportTagStyle.INFO else PassportTagStyle.WARNING

private fun resolveFormatOptionLabel(
    format: TripReportFormat,
    pdfLabel: String,
    csvLabel: String
): String = when (format) {
    TripReportFormat.PDF -> pdfLabel
    TripReportFormat.CSV -> csvLabel
}
