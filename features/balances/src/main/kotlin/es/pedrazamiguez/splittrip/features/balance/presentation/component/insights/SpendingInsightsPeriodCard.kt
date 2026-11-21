package es.pedrazamiguez.splittrip.features.balance.presentation.component.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.extension.debounced
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.AppDatePickerDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.SecondaryBodyText
import es.pedrazamiguez.splittrip.features.balance.R
import es.pedrazamiguez.splittrip.features.balance.presentation.model.SpendingPeriodPreset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun SpendingInsightsPeriodCard(
    selectedPreset: SpendingPeriodPreset,
    periodSummaryLabel: String,
    customStartDate: LocalDate?,
    customEndDate: LocalDate?,
    formattedCustomStartDate: String,
    formattedCustomEndDate: String,
    oldestExpenseDate: LocalDate?,
    newestExpenseDate: LocalDate?,
    onSelectPreset: (SpendingPeriodPreset) -> Unit,
    onSelectCustomStartDate: (LocalDate) -> Unit,
    onSelectCustomEndDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val presets = listOf(
        SpendingPeriodPreset.ALL_TIME to R.string.balances_insights_period_all_time,
        SpendingPeriodPreset.THIS_MONTH to R.string.balances_insights_period_this_month,
        SpendingPeriodPreset.LAST_MONTH to R.string.balances_insights_period_last_month,
        SpendingPeriodPreset.CUSTOM to R.string.balances_insights_period_custom
    )

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.Default),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                presets.forEach { (preset, labelRes) ->
                    val isSelected = selectedPreset == preset
                    val onChipClick = debounced { onSelectPreset(preset) }
                    PassportChip(
                        label = stringResource(labelRes),
                        selected = isSelected,
                        onClick = onChipClick,
                        leadingIcon = if (isSelected) {
                            { Icon(imageVector = TablerIcons.Outline.Check, contentDescription = null) }
                        } else {
                            null
                        }
                    )
                }
            }

            if (periodSummaryLabel.isNotBlank()) {
                SecondaryBodyText(text = periodSummaryLabel)
            }

            if (selectedPreset == SpendingPeriodPreset.CUSTOM) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
                ) {
                    StyledOutlinedTextField(
                        value = formattedCustomStartDate,
                        onValueChange = {},
                        label = stringResource(R.string.balances_insights_date_from),
                        readOnly = true,
                        focusable = false,
                        trailingIcon = {
                            Icon(imageVector = TablerIcons.Outline.Calendar, contentDescription = null)
                        },
                        onClick = { showStartPicker = true },
                        modifier = Modifier.weight(1f)
                    )

                    StyledOutlinedTextField(
                        value = formattedCustomEndDate,
                        onValueChange = {},
                        label = stringResource(R.string.balances_insights_date_to),
                        readOnly = true,
                        focusable = false,
                        trailingIcon = {
                            Icon(imageVector = TablerIcons.Outline.Calendar, contentDescription = null)
                        },
                        onClick = { showEndPicker = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        AppDatePickerDialog(
            initialDateMillis = customStartDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            onDismiss = { showStartPicker = false },
            onDateSelected = { millis ->
                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onSelectCustomStartDate(date)
                showStartPicker = false
            },
            minDate = oldestExpenseDate,
            maxDate = newestExpenseDate,
            title = stringResource(R.string.balances_insights_date_start_dialog_title)
        )
    }

    if (showEndPicker) {
        AppDatePickerDialog(
            initialDateMillis = customEndDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            onDismiss = { showEndPicker = false },
            onDateSelected = { millis ->
                val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onSelectCustomEndDate(date)
                showEndPicker = false
            },
            minDate = oldestExpenseDate,
            maxDate = newestExpenseDate,
            title = stringResource(R.string.balances_insights_date_end_dialog_title)
        )
    }
}
