package es.pedrazamiguez.splittrip.domain.service

import java.math.BigDecimal

interface ExchangeRateCalculationService {
    companion object {
        const val DEFAULT_DECIMAL_PLACES = 2
    }
    fun calculateGroupAmount(
        sourceAmount: BigDecimal,
        rate: BigDecimal,
        targetDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): BigDecimal
    fun calculateImpliedRate(sourceAmount: BigDecimal, groupAmount: BigDecimal): BigDecimal
    fun calculateGroupAmountFromStrings(
        sourceAmountString: String,
        exchangeRateString: String,
        sourceDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
        targetDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): String
    fun calculateGroupAmountFromDisplayRate(
        sourceAmountString: String,
        displayRateString: String,
        sourceDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES,
        targetDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): String
    fun calculateImpliedDisplayRateFromStrings(
        sourceAmountString: String,
        groupAmountString: String,
        sourceDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): String
    fun displayRateToCalculationRate(displayRateString: String): BigDecimal
    fun calculateImpliedRateFromStrings(
        sourceAmountString: String,
        groupAmountString: String,
        sourceDecimalPlaces: Int = DEFAULT_DECIMAL_PLACES
    ): String
    fun calculateExchangeRate(amountWithdrawn: Long, deductedBaseAmount: Long): BigDecimal
    fun calculateBlendedRate(sourceAmountCents: Long, groupAmountCents: Long): BigDecimal
    fun calculateBlendedDisplayRate(sourceAmountCents: Long, groupAmountCents: Long): BigDecimal
    fun convertCentsToGroupCurrencyViaDisplayRate(
        amountCents: Long,
        displayRateString: String
    ): Long
}
