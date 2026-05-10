package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.isValidDecimalInput
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import java.math.BigDecimal

/**
 * Returns true when [amount] is either blank (user still typing) or a positive decimal number.
 * Non-numeric text, zero, and negative values are considered invalid.
 *
 * Extracted from [FormEventHandler] as a top-level function to keep the class within the
 * configured Detekt `TooManyFunctions` threshold while maintaining full testability.
 */
internal fun validateAmountInput(amount: String): Boolean {
    if (amount.isBlank()) return true
    if (!amount.isValidDecimalInput()) return false
    val parsed = CurrencyConverter.normalizeAmountString(amount.trim()).toBigDecimalOrNull()
        ?: return false
    return parsed > BigDecimal.ZERO
}
