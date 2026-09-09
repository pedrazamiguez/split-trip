package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.extension.debouncedClickable
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.AppDatePickerDialog
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.model.DateRangePreset
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Suppress("LongMethod", "LongParameterList", "CognitiveComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateRangeFilterSection(
    criteria: ExpenseFilterCriteria,
    activePreset: DateRangePreset?,
    onPresetSelected: (DateRangePreset) -> Unit,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    oldestExpenseDate: LocalDate?,
    newestExpenseDate: LocalDate?,
    formattedStartDate: String,
    formattedEndDate: String,
    modifier: Modifier = Modifier
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_date_range))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            DateRangePreset.entries.forEach { preset ->
                PassportChip(
                    label = stringResource(preset.titleRes),
                    selected = activePreset == preset,
                    onClick = { onPresetSelected(preset) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StyledOutlinedTextField(
                    value = formattedStartDate,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.expenses_filter_date_from),
                    placeholder = stringResource(R.string.expenses_filter_date_start_placeholder),
                    trailingIcon = {
                        if (criteria.startDate != null) {
                            IconButton(
                                onClick = { onCriteriaChange(criteria.copy(startDate = null)) }
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Outline.X,
                                    contentDescription = stringResource(
                                        R.string.expenses_filter_date_clear_start_cd
                                    ),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Icon(
                                imageVector = TablerIcons.Outline.Calendar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = if (criteria.startDate != null) 48.dp else 0.dp)
                        .debouncedClickable { showStartDatePicker = true }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                StyledOutlinedTextField(
                    value = formattedEndDate,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.expenses_filter_date_to),
                    placeholder = stringResource(R.string.expenses_filter_date_end_placeholder),
                    trailingIcon = {
                        if (criteria.endDate != null) {
                            IconButton(
                                onClick = { onCriteriaChange(criteria.copy(endDate = null)) }
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Outline.X,
                                    contentDescription = stringResource(
                                        R.string.expenses_filter_date_clear_end_cd
                                    ),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Icon(
                                imageVector = TablerIcons.Outline.Calendar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(end = if (criteria.endDate != null) 48.dp else 0.dp)
                        .debouncedClickable { showEndDatePicker = true }
                )
            }
        }
    }

    if (showStartDatePicker) {
        val initialStartMillis = criteria.startDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
        AppDatePickerDialog(
            initialDateMillis = initialStartMillis,
            title = stringResource(R.string.expenses_filter_date_start_dialog_title),
            minDate = oldestExpenseDate,
            maxDate = criteria.endDate ?: newestExpenseDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { selectedMillis ->
                val selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                onCriteriaChange(criteria.copy(startDate = selectedDate))
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        val initialEndMillis = criteria.endDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
        AppDatePickerDialog(
            initialDateMillis = initialEndMillis,
            title = stringResource(R.string.expenses_filter_date_end_dialog_title),
            minDate = criteria.startDate ?: oldestExpenseDate,
            maxDate = newestExpenseDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { selectedMillis ->
                val selectedDate = Instant.ofEpochMilli(selectedMillis).atZone(ZoneOffset.UTC).toLocalDate()
                onCriteriaChange(criteria.copy(endDate = selectedDate))
                showEndDatePicker = false
            }
        )
    }
}
