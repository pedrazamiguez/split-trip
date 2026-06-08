package es.pedrazamiguez.splittrip.features.expense.presentation.mapper

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.features.expense.presentation.model.AddOnUiModel
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Maps add-on UI models to domain [AddOn] objects.
 *
 * Extracted from [AddExpenseUiMapper] so each mapper stays within detekt's
 * function-count threshold.
 */
class AddExpenseAddOnUiMapper {

    /**
     * Maps add-on UI models to domain [AddOn] objects.
     * Only includes add-ons with a valid resolved amount.
     */
    fun mapAddOnsToDomain(
        addOns: List<AddOnUiModel>,
        fallbackCurrencyCode: String
    ): List<AddOn> = addOns
        .filter { it.resolvedAmountCents > 0 }
        .map { uiModel ->
            val exchangeRate = resolveAddOnExchangeRate(uiModel.displayExchangeRate)
            AddOn(
                id = uiModel.id,
                type = uiModel.type,
                mode = uiModel.mode,
                valueType = uiModel.valueType,
                amountCents = uiModel.resolvedAmountCents,
                currency = uiModel.currency?.code ?: fallbackCurrencyCode,
                exchangeRate = exchangeRate,
                groupAmountCents = uiModel.groupAmountCents,
                paymentMethod = uiModel.paymentMethod?.let {
                    runCatching {
                        PaymentMethod.fromString(it.id)
                    }.getOrDefault(PaymentMethod.OTHER)
                } ?: PaymentMethod.OTHER,
                description = uiModel.description.ifBlank { null }
            )
        }

    private fun resolveAddOnExchangeRate(displayExchangeRate: String): BigDecimal {
        val normalizedRate = CurrencyConverter.normalizeAmountString(displayExchangeRate.trim())
        val displayRate = normalizedRate.toBigDecimalOrNull() ?: BigDecimal.ONE
        return if (displayRate.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal.ONE.divide(displayRate, DomainConstants.RATE_PRECISION, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ONE
        }
    }
}
