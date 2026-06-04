package es.pedrazamiguez.splittrip.features.expense.presentation.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import java.time.ZoneOffset

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
