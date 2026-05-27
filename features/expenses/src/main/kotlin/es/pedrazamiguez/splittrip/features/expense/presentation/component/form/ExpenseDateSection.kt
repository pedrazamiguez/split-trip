package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.features.expense.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.Calendar as JavaCalendar

/**
 * Expense date and time picker section.
 * Shows a read-only text field that opens a Material 3 DatePickerDialog on tap,
 * followed by a TimePicker in an AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseDateSection(
    formattedExpenseDate: String,
    isExpenseDateValid: Boolean,
    expenseDateMillis: Long?,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var tempSelectedDateMillis by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(
            text = stringResource(R.string.add_expense_date_time_title)
        )
        StyledOutlinedTextField(
            value = formattedExpenseDate,
            onValueChange = {},
            readOnly = true,
            label = stringResource(R.string.add_expense_date_time_label),
            trailingIcon = { Icon(TablerIcons.Outline.Calendar, null) },
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            isError = !isExpenseDateValid
        )
    }

    if (showDatePicker) {
        ExpenseDatePickerDialog(
            initialDateMillis = expenseDateMillis,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                tempSelectedDateMillis = selectedDate
                showDatePicker = false
                showTimePicker = true
            }
        )
    }

    if (showTimePicker) {
        ExpenseTimePickerDialog(
            selectedDateMillis = tempSelectedDateMillis ?: System.currentTimeMillis(),
            initialTimeMillis = expenseDateMillis,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { hour, minute ->
                val dateMillis = tempSelectedDateMillis ?: System.currentTimeMillis()
                val localDate = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val localDateTime = localDate.atTime(hour, minute)
                val combinedMillis = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli()
                onDateSelected(combinedMillis)
                showTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseDatePickerDialog(
    initialDateMillis: Long?,
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis ?: System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= System.currentTimeMillis()
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= LocalDate.now(ZoneOffset.UTC).year
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it)
                    }
                }
            ) {
                Text(stringResource(R.string.expense_date_time_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.expense_date_time_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTimePickerDialog(
    selectedDateMillis: Long,
    initialTimeMillis: Long?,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val calendar = JavaCalendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        initialTimeMillis?.let { timeInMillis = it }
    }
    val initialHour = calendar.get(JavaCalendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(JavaCalendar.MINUTE)
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    // Validate selectable times for today to prevent future times
    val selectedLocalDate = Instant.ofEpochMilli(selectedDateMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val nowUtc = LocalDateTime.now(ZoneOffset.UTC)
    val isToday = selectedLocalDate == nowUtc.toLocalDate()

    val isValidTime = if (isToday) {
        val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
        val nowTime = nowUtc.toLocalTime()
        !selectedTime.isAfter(nowTime)
    } else {
        true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                },
                enabled = isValidTime
            ) {
                Text(stringResource(R.string.expense_date_time_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.expense_date_time_cancel))
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
