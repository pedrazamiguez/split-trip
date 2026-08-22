package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.subexpense

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Calendar
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Check
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Clock
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.Trash
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyDropdown
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.ArithmeticTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.rememberLocale
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatShortDate
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.service.calculator.ExpressionCalculatorService
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SubExpenseUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import java.time.ZoneOffset
import kotlinx.collections.immutable.ImmutableList
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod", "LongParameterList")
@Composable
fun SubExpenseItemEditor(
    subExpense: SubExpenseUiModel,
    itemIndex: Int,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onRemove: () -> Unit,
    groupCurrency: CurrencyUiModel? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val locale = rememberLocale()
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedCurrencyModel = subExpense.currency
        ?: availableCurrencies.find { it.code == subExpense.resolvedCurrencyCode }
        ?: groupCurrency

    Column(
        modifier = modifier.fillMaxWidth(),
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

        // Amount & Currency Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            val evaluator = koinInject<ExpressionCalculatorService>()

            ArithmeticTextField(
                value = subExpense.amountInput,
                onValueChange = { onEvent(AddExpenseUiEvent.SubExpenseAmountChanged(subExpense.id, it)) },
                evaluator = evaluator,
                maxDecimalPlaces = selectedCurrencyModel?.decimalDigits ?: UiConstants.DEFAULT_MAX_DECIMAL_PLACES,
                minDecimalPlaces = selectedCurrencyModel?.decimalDigits ?: 0,
                label = stringResource(R.string.expense_field_amount),
                modifier = Modifier.weight(AMOUNT_WEIGHT),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )

            CurrencyDropdown(
                selectedCurrency = selectedCurrencyModel,
                availableCurrencies = availableCurrencies,
                onCurrencySelected = { code ->
                    onEvent(AddExpenseUiEvent.SubExpenseCurrencySelected(subExpense.id, code))
                },
                label = stringResource(R.string.expense_sub_expense_currency_label),
                modifier = Modifier.weight(CURRENCY_WEIGHT)
            )
        }

        // Exchange Rate Section (when tranche currency is foreign)
        AnimatedVisibility(
            visible = subExpense.showExchangeRateSection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val groupDisplayText = groupCurrency?.displayText ?: "EUR (€)"
            val trancheDisplayText = selectedCurrencyModel?.displayText ?: subExpense.currencyCode
            CurrencyConversionCard(
                state = CurrencyConversionCardState(
                    title = stringResource(R.string.expense_sub_expense_exchange_rate_title),
                    exchangeRateValue = subExpense.displayExchangeRate,
                    exchangeRateLabel = stringResource(
                        R.string.add_expense_rate_label_format,
                        groupDisplayText,
                        trancheDisplayText
                    ),
                    groupAmountValue = subExpense.calculatedGroupAmount,
                    groupAmountLabel = stringResource(
                        R.string.add_expense_amount_in,
                        groupDisplayText
                    ),
                    isLoadingRate = false,
                    isExchangeRateLocked = false,
                    isInsufficientCash = false,
                    isGroupAmountError = false
                ),
                groupAmountDecimalPlaces = groupCurrency?.decimalDigits ?: UiConstants.DEFAULT_MAX_DECIMAL_PLACES,
                groupAmountMinDecimalPlaces = groupCurrency?.decimalDigits ?: 0,
                onExchangeRateChanged = { rate ->
                    onEvent(AddExpenseUiEvent.SubExpenseExchangeRateChanged(subExpense.id, rate))
                },
                onGroupAmountChanged = { amount ->
                    onEvent(AddExpenseUiEvent.SubExpenseGroupAmountChanged(subExpense.id, amount))
                }
            )
        }

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
                leadingIcon = { Icon(TablerIcons.Outline.Check, contentDescription = null) },
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
                leadingIcon = { Icon(TablerIcons.Outline.Clock, contentDescription = null) },
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

        // Payment Method Selection (with icons)
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
                        leadingIcon = { Icon(method.icon, contentDescription = null) },
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

        HorizontalDivider(
            modifier = Modifier.padding(top = MaterialTheme.spacing.Small),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
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

private const val AMOUNT_WEIGHT = 0.55f
private const val CURRENCY_WEIGHT = 0.45f
