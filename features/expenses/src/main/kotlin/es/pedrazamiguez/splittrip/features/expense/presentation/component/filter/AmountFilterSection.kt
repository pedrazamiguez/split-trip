package es.pedrazamiguez.splittrip.features.expense.presentation.component.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import es.pedrazamiguez.splittrip.core.designsystem.foundation.spacing
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.chip.PassportChip
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.input.StyledOutlinedTextField
import es.pedrazamiguez.splittrip.core.designsystem.presentation.component.text.CardSectionLabelText
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.parseAmountToSmallestUnit
import es.pedrazamiguez.splittrip.domain.model.ExpenseFilterCriteria
import es.pedrazamiguez.splittrip.features.expense.R
import java.math.BigDecimal
import java.util.Currency
import kotlinx.collections.immutable.ImmutableList

@Suppress("LongMethod", "CyclomaticComplexMethod", "CognitiveComplexMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmountFilterSection(
    criteria: ExpenseFilterCriteria,
    groupCurrencies: ImmutableList<String>,
    defaultCurrency: String,
    onCriteriaChange: (ExpenseFilterCriteria) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeCurrency = criteria.filterCurrency ?: defaultCurrency
    var minText by rememberSaveable { mutableStateOf("") }
    var maxText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(criteria.minAmount, criteria.maxAmount, activeCurrency) {
        minText = syncAmountText(minText, criteria.minAmount, activeCurrency)
        maxText = syncAmountText(maxText, criteria.maxAmount, activeCurrency)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
    ) {
        CardSectionLabelText(text = stringResource(R.string.expenses_filter_section_amount))

        if (groupCurrencies.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Small)
            ) {
                groupCurrencies.forEach { currency ->
                    val isSelected = criteria.filterCurrency.equals(currency, ignoreCase = true)
                    PassportChip(
                        label = currency,
                        selected = isSelected,
                        onClick = {
                            val updated = updateCriteriaForToggledCurrency(
                                criteria = criteria,
                                toggledCurrency = currency,
                                minText = minText,
                                maxText = maxText,
                                defaultCurrency = defaultCurrency
                            )
                            onCriteriaChange(updated)
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.Medium)
        ) {
            StyledOutlinedTextField(
                value = minText,
                onValueChange = { input ->
                    if (isValidAmountInput(input)) {
                        minText = input
                        onCriteriaChange(criteria.copy(minAmount = parseInputToCents(input, activeCurrency)))
                    }
                },
                label = stringResource(R.string.expenses_filter_amount_min),
                placeholder = stringResource(R.string.expenses_filter_amount_min),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next,
                suffix = {
                    Text(
                        text = activeCurrency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            )

            StyledOutlinedTextField(
                value = maxText,
                onValueChange = { input ->
                    if (isValidAmountInput(input)) {
                        maxText = input
                        onCriteriaChange(criteria.copy(maxAmount = parseInputToCents(input, activeCurrency)))
                    }
                },
                label = stringResource(R.string.expenses_filter_amount_max),
                placeholder = stringResource(R.string.expenses_filter_amount_max),
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
                suffix = {
                    Text(
                        text = activeCurrency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun syncAmountText(currentText: String, amount: Long?, currency: String): String {
    val parsed = parseInputToCents(currentText, currency)
    return if (amount != parsed) {
        formatSmallestUnitToInputString(amount, currency)
    } else {
        currentText
    }
}

private fun parseInputToCents(text: String, currency: String): Long? =
    if (text.isNotBlank() && text.any { it.isDigit() }) {
        parseAmountToSmallestUnit(text, currency)
    } else {
        null
    }

private fun updateCriteriaForToggledCurrency(
    criteria: ExpenseFilterCriteria,
    toggledCurrency: String,
    minText: String,
    maxText: String,
    defaultCurrency: String
): ExpenseFilterCriteria {
    val isSelected = criteria.filterCurrency.equals(toggledCurrency, ignoreCase = true)
    val newCurrency = if (isSelected) null else toggledCurrency
    val targetCurrency = newCurrency ?: defaultCurrency
    val newMin = parseInputToCents(minText, targetCurrency)
    val newMax = parseInputToCents(maxText, targetCurrency)
    return criteria.copy(
        filterCurrency = newCurrency,
        minAmount = newMin,
        maxAmount = newMax
    )
}

private fun isValidAmountInput(input: String): Boolean {
    if (input.isEmpty()) return true
    return input.matches(Regex("^[0-9]*[.,]?[0-9]*$"))
}

private fun formatSmallestUnitToInputString(amount: Long?, currencyCode: String): String {
    if (amount == null) return ""
    val fractionDigits = runCatching {
        Currency.getInstance(currencyCode).defaultFractionDigits
    }.getOrElse { 2 }
    return BigDecimal(amount)
        .movePointLeft(fractionDigits)
        .stripTrailingZeros()
        .toPlainString()
}
