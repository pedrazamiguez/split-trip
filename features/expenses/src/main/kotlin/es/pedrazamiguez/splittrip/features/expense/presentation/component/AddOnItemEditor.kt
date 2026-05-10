package es.pedrazamiguez.splittrip.features.expense.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.icon.TablerIcons
import es.pedrazamiguez.splittrip.core.designsystem.icon.filled.CaretDownFilled
import es.pedrazamiguez.splittrip.core.designsystem.icon.outline.X
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCard
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyConversionCardState
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.currency.CurrencyDropdown
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.model.CurrencyUiModel
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.features.expense.R
import es.pedrazamiguez.splittrip.features.expense.presentation.extensions.toStringRes
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.model.PaymentMethodUiModel
import es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.event.AddExpenseUiEvent
import kotlinx.collections.immutable.ImmutableList

/**
 * Editor for a single add-on within the expense form.
 *
 * Displays type/mode/value-type chip selectors, an amount input field,
 * optional currency and payment method dropdowns, and a description field.
 *
 * Stateless: takes pure data and emits [AddExpenseUiEvent]s via [onEvent].
 */
@Suppress("LongMethod") // Compose UI builder DSL
@Composable
fun AddOnItemEditor(
    addOn: AddOnUiModel,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    showCurrencySelector: Boolean,
    onEvent: (AddExpenseUiEvent) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
    ) {
        AddOnEditorHeader(
            addOn = addOn,
            onRemove = onRemove
        )

        AddOnChipSelectors(
            addOn = addOn,
            onEvent = onEvent
        )

        AddOnAmountInput(
            addOn = addOn,
            onAmountChanged = { amount ->
                onEvent(AddExpenseUiEvent.AddOnAmountChanged(addOn.id, amount))
            }
        )

        AddOnCurrencySelector(
            addOn = addOn,
            availableCurrencies = availableCurrencies,
            showCurrencySelector = showCurrencySelector,
            onCurrencySelected = { code ->
                onEvent(AddExpenseUiEvent.AddOnCurrencySelected(addOn.id, code))
            }
        )

        AddOnExchangeRateSection(
            addOn = addOn,
            onRateChanged = { rate ->
                onEvent(AddExpenseUiEvent.AddOnExchangeRateChanged(addOn.id, rate))
            },
            onGroupAmountChanged = { amount ->
                onEvent(AddExpenseUiEvent.AddOnGroupAmountChanged(addOn.id, amount))
            }
        )

        AddOnPaymentMethodSelector(
            addOn = addOn,
            paymentMethods = paymentMethods,
            onPaymentMethodSelected = { methodId ->
                onEvent(
                    AddExpenseUiEvent.AddOnPaymentMethodSelected(addOn.id, methodId)
                )
            }
        )

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

@Composable
private fun AddOnEditorHeader(
    addOn: AddOnUiModel,
    onRemove: () -> Unit
) {
    // ── Header: Type label + Remove button ──────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(addOn.type.toStringRes()),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = TablerIcons.Outline.X,
                contentDescription = stringResource(
                    R.string.add_expense_add_on_remove
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AddOnChipSelectors(
    addOn: AddOnUiModel,
    onEvent: (AddExpenseUiEvent) -> Unit
) {
    // ── Type Chips ──────────────────────────────────────────
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
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

    // ── Mode Chips (On top / Included) ──────────────────────
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
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

    // ── Value Type Chips (Amount / Percentage) ──────────────
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)) {
        AddOnValueType.entries.forEach { valueType ->
            PassportChip(
                label = stringResource(valueType.toStringRes()),
                selected = addOn.valueType == valueType,
                onClick = {
                    onEvent(
                        AddExpenseUiEvent.AddOnValueTypeChanged(addOn.id, valueType)
                    )
                }
            )
        }
    }
}

@Composable
private fun AddOnAmountInput(
    addOn: AddOnUiModel,
    onAmountChanged: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    // ── Amount Input ────────────────────────────────────────
    val amountSuffix = if (addOn.valueType == AddOnValueType.PERCENTAGE) "%" else null

    StyledOutlinedTextField(
        value = addOn.amountInput,
        onValueChange = onAmountChanged,
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
}

@Composable
private fun AddOnCurrencySelector(
    addOn: AddOnUiModel,
    availableCurrencies: ImmutableList<CurrencyUiModel>,
    showCurrencySelector: Boolean,
    onCurrencySelected: (String) -> Unit
) {
    // ── Currency Selector (only when multi-currency) ────────
    AnimatedVisibility(
        visible = showCurrencySelector,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        CurrencyDropdown(
            selectedCurrency = addOn.currency,
            availableCurrencies = availableCurrencies,
            onCurrencySelected = onCurrencySelected,
            label = stringResource(R.string.add_expense_currency_label)
        )
    }
}

@Composable
private fun AddOnPaymentMethodSelector(
    addOn: AddOnUiModel,
    paymentMethods: ImmutableList<PaymentMethodUiModel>,
    onPaymentMethodSelected: (String) -> Unit
) {
    // ── Payment Method Selector ─────────────────────────────
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
                        onPaymentMethodSelected(method.id)
                        methodExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AddOnExchangeRateSection(
    addOn: AddOnUiModel,
    onRateChanged: (String) -> Unit,
    onGroupAmountChanged: (String) -> Unit
) {
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
                isGroupAmountError = false
            ),
            onExchangeRateChanged = onRateChanged,
            onGroupAmountChanged = onGroupAmountChanged
        )
    }
}
