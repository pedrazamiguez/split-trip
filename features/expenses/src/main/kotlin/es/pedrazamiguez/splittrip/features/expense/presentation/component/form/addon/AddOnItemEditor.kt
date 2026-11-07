package es.pedrazamiguez.splittrip.features.expense.presentation.component.form.addon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.constant.UiConstants
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.CaretDownFilled
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencySelectorField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CaptionText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.LabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@Composable
fun AddOnItemEditor(
    addOn: AddOnUiModel,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    showCurrencySelector: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit,
    groupCurrency: CurrencyUiModel? = null,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        // Section 1: Extra type
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
            LabelText(stringResource(R.string.add_expense_add_on_type_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                AddOnType.entries.forEach { type ->
                    PassportChip(
                        label = stringResource(type.toStringRes()),
                        selected = addOn.type == type,
                        onClick = {
                            onEvent(AddExpenseUiEvent.AddOnTypeChanged(addOn.id, type))
                        }
                    )
                }
            }
        }

        // Section 2: Application Mode
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
            LabelText(stringResource(R.string.add_expense_add_on_mode_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                AddOnMode.entries.forEach { mode ->
                    PassportChip(
                        label = stringResource(mode.toStringRes()),
                        selected = addOn.mode == mode,
                        onClick = {
                            onEvent(AddExpenseUiEvent.AddOnModeChanged(addOn.id, mode))
                        }
                    )
                }
            }
            val modeSupportingText = when (addOn.mode) {
                AddOnMode.ON_TOP -> {
                    if (addOn.type == AddOnType.DISCOUNT) {
                        stringResource(R.string.add_expense_add_on_mode_on_top_discount_desc)
                    } else {
                        stringResource(R.string.add_expense_add_on_mode_on_top_desc)
                    }
                }
                AddOnMode.INCLUDED -> stringResource(R.string.add_expense_add_on_mode_included_desc)
            }
            CaptionText(
                text = modeSupportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 3: Calculation / Value Type
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)) {
            LabelText(stringResource(R.string.add_expense_add_on_value_type_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.ExtraSmall)
            ) {
                AddOnValueType.entries.forEach { valueType ->
                    PassportChip(
                        label = stringResource(valueType.toStringRes()),
                        selected = addOn.valueType == valueType,
                        onClick = {
                            onEvent(AddExpenseUiEvent.AddOnValueTypeChanged(addOn.id, valueType))
                        }
                    )
                }
            }
            val valueTypeSupportingText = when (addOn.valueType) {
                AddOnValueType.EXACT -> stringResource(R.string.add_expense_add_on_value_type_exact_desc)
                AddOnValueType.PERCENTAGE -> {
                    if (addOn.amountInput.isNotBlank() && addOn.formattedAmount.isNotBlank()) {
                        stringResource(
                            R.string.add_expense_add_on_value_type_percentage_resolved_desc,
                            "${addOn.amountInput}%",
                            addOn.formattedAmount
                        )
                    } else {
                        stringResource(R.string.add_expense_add_on_value_type_percentage_desc)
                    }
                }
            }
            CaptionText(
                text = valueTypeSupportingText,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Amount Input
        val amountSuffix = if (addOn.valueType == AddOnValueType.PERCENTAGE) "%" else null
        StyledOutlinedTextField(
            value = addOn.amountInput,
            onValueChange = { onEvent(AddExpenseUiEvent.AddOnAmountChanged(addOn.id, it)) },
            label = stringResource(R.string.add_expense_add_on_amount_hint),
            modifier = Modifier.fillMaxWidth(),
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Next,
            isError = !addOn.isAmountValid,
            suffix = amountSuffix?.let { { Text(it) } },
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus() }
            )
        )

        // Currency Selector
        AnimatedVisibility(
            visible = showCurrencySelector,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CurrencySelectorField(
                selectedCurrency = addOn.currency,
                availableCurrencies = availableCurrencies,
                onCurrencySelected = { code ->
                    onEvent(AddExpenseUiEvent.AddOnCurrencySelected(addOn.id, code))
                },
                label = stringResource(R.string.add_expense_currency_label)
            )
        }

        // Exchange Rate Section
        AnimatedVisibility(
            visible = addOn.showExchangeRateSection,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CurrencyConversionCard(
                state = CurrencyConversionCardState(
                    title = stringResource(R.string.add_expense_exchange_rate_title),
                    exchangeRateValue = addOn.displayExchangeRate,
                    exchangeRateLabel = addOn.exchangeRateLabel,
                    groupAmountValue = addOn.calculatedGroupAmount,
                    groupAmountLabel = addOn.groupAmountLabel,
                    isLoadingRate = addOn.isLoadingRate,
                    isExchangeRateLocked = addOn.isExchangeRateLocked,
                    exchangeRateLockedHint = addOn.exchangeRateLockedHint,
                    isInsufficientCash = addOn.isInsufficientCash,
                    isGroupAmountError = !CurrencyConverter.isPositiveAmount(addOn.calculatedGroupAmount),
                    isExchangeRateFieldError = !CurrencyConverter.isPositiveAmount(addOn.displayExchangeRate)
                ),
                groupAmountDecimalPlaces = groupCurrency?.decimalDigits ?: UiConstants.DEFAULT_MAX_DECIMAL_PLACES,
                groupAmountMinDecimalPlaces = groupCurrency?.decimalDigits ?: 0,
                onExchangeRateChanged = { rate ->
                    onEvent(AddExpenseUiEvent.AddOnExchangeRateChanged(addOn.id, rate))
                },
                onGroupAmountChanged = { amount ->
                    onEvent(AddExpenseUiEvent.AddOnGroupAmountChanged(addOn.id, amount))
                }
            )
        }

        // Payment Method Selector
        Box {
            var methodExpanded by remember { mutableStateOf(false) }
            StyledOutlinedTextField(
                value = addOn.paymentMethod?.displayText ?: "",
                onValueChange = {},
                readOnly = true,
                label = stringResource(R.string.add_expense_payment_method_title),
                trailingIcon = { Icon(TablerIcons.Filled.CaretDownFilled, null) },
                onClick = { methodExpanded = true },
                modifier = Modifier.fillMaxWidth()
            )

            DropdownMenu(
                expanded = methodExpanded,
                onDismissRequest = { methodExpanded = false }
            ) {
                paymentMethods.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(method.displayText) },
                        onClick = {
                            onEvent(AddExpenseUiEvent.AddOnPaymentMethodSelected(addOn.id, method.id))
                            methodExpanded = false
                        }
                    )
                }
            }
        }

        // Description
        StyledOutlinedTextField(
            value = addOn.description,
            onValueChange = { desc ->
                onEvent(AddExpenseUiEvent.AddOnDescriptionChanged(addOn.id, desc))
            },
            label = stringResource(R.string.add_expense_add_on_description_hint),
            modifier = Modifier.fillMaxWidth(),
            capitalization = KeyboardCapitalization.Sentences,
            singleLine = false,
            maxLines = 2,
            imeAction = ImeAction.Done,
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.ExtraSmall))
    }
}
