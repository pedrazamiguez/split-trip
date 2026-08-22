package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.subexpense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.layout.FlatCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import java.time.ZoneOffset
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun SubExpenseItemEditor(
    subExpense: SubExpenseUiModel,
    itemIndex: Int,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val locale = rememberLocale()
    var showDatePicker by remember { mutableStateOf(false) }

    FlatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
        ) {
            // Header: Title / Number and Remove Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.expense_sub_expense_number, itemIndex + 1),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = TablerIcons.Outline.Trash,
                        contentDescription = stringResource(R.string.expense_sub_expense_remove),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tranche Title field
            StyledOutlinedTextField(
                value = subExpense.title,
                onValueChange = { onEvent(AddExpenseUiEvent.SubExpenseTitleChanged(subExpense.id, it)) },
                label = stringResource(R.string.expense_field_title),
                placeholder = stringResource(R.string.expense_field_title_required),
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Amount field
            StyledOutlinedTextField(
                value = subExpense.amountInput,
                onValueChange = { onEvent(AddExpenseUiEvent.SubExpenseAmountChanged(subExpense.id, it)) },
                label = stringResource(R.string.expense_field_amount),
                suffix = { Text(subExpense.currency) },
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            // Payment Status Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                val isPaid = subExpense.paymentStatus == PaymentStatus.FINISHED
                PassportChip(
                    label = stringResource(PaymentStatus.FINISHED.toStringRes()),
                    selected = isPaid,
                    onClick = {
                        onEvent(
                            AddExpenseUiEvent.SubExpensePaymentStatusSelected(
                                subExpense.id,
                                PaymentStatus.FINISHED.name
                            )
                        )
                    }
                )
                val isScheduled = subExpense.paymentStatus == PaymentStatus.SCHEDULED
                PassportChip(
                    label = stringResource(PaymentStatus.SCHEDULED.toStringRes()),
                    selected = isScheduled,
                    onClick = {
                        onEvent(
                            AddExpenseUiEvent.SubExpensePaymentStatusSelected(
                                subExpense.id,
                                PaymentStatus.SCHEDULED.name
                            )
                        )
                    }
                )
            }

            // Due Date field (when SCHEDULED)
            AnimatedVisibility(visible = subExpense.paymentStatus == PaymentStatus.SCHEDULED) {
                val formattedDueDate = subExpense.dueDate?.toLocalDate()?.formatShortDate(locale) ?: ""
                StyledOutlinedTextField(
                    value = formattedDueDate,
                    onValueChange = {},
                    readOnly = true,
                    label = stringResource(R.string.add_expense_due_date_label),
                    trailingIcon = { Icon(TablerIcons.Outline.Calendar, null) },
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Payment Method Selection
            if (paymentMethods.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = subExpense.paymentMethod.name == method.id
                        PassportChip(
                            label = method.displayText,
                            selected = isSelected,
                            onClick = {
                                onEvent(
                                    AddExpenseUiEvent.SubExpensePaymentMethodSelected(
                                        subExpense.id,
                                        method.id
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Optional Notes
            StyledOutlinedTextField(
                value = subExpense.notes ?: "",
                onValueChange = { onEvent(AddExpenseUiEvent.SubExpenseNotesChanged(subExpense.id, it)) },
                label = stringResource(R.string.add_expense_notes_label),
                placeholder = stringResource(R.string.add_expense_notes_helper),
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = subExpense.dueDate?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onEvent(AddExpenseUiEvent.SubExpenseDueDateSelected(subExpense.id, it))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.add_expense_due_date_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.add_expense_due_date_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
