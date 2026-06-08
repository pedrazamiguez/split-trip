package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import es.pedrazamiguez.splittrip.features.expense.R
import java.util.Calendar as JavaCalendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpenseTimePickerDialog(
    initialTimeMillis: Long?,
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    val calendar = if (initialTimeMillis != null) {
        JavaCalendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = initialTimeMillis
        }
    } else {
        JavaCalendar.getInstance()
    }
    val initialHour = calendar.get(JavaCalendar.HOUR_OF_DAY)
    val initialMinute = calendar.get(JavaCalendar.MINUTE)
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                }
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
