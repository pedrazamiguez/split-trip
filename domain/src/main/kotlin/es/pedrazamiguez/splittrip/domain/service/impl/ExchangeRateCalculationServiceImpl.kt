package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import java.math.BigDecimal
import java.math.RoundingMode

class ExchangeRateCalculationServiceImpl : ExchangeRateCalculationService {

    private companion object {
        const val RATE_PRECISION = 6
        const val DEFAULT_DECIMAL_PLACES = 2
    }

    // ── Core BigDecimal-based Calculations ───────────────────────────────

    /**
     * Calculates the group amount from source amount and exchange rate.
     *
     * @param sourceAmount The amount in source currency
     * @param rate The exchange rate (source to target)
     * @param targetDecimalPlaces Number of decimal places for the target currency (default 2)
     * @return The calculated amount in group currency
     */
    override fun calculateGroupAmount(
        sourceAmount: BigDecimal,
        rate: BigDecimal,
        targetDecimalPlaces: Int
    ): BigDecimal {
        if (rate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        // Source * Rate = Target (e.g. 1000 THB * 0.027 = 27 EUR)
        return sourceAmount.multiply(rate).setScale(targetDecimalPlaces, RoundingMode.HALF_UP)
    }

    /**
     * Calculates the implied exchange rate from source and target amounts.
     *
     * @param sourceAmount The amount in source currency
     * @param groupAmount The amount in group currency
     * @return The implied exchange rate
     */
    override fun calculateImpliedRate(sourceAmount: BigDecimal, groupAmount: BigDecimal): BigDecimal {
        if (sourceAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        // Target / Source = Rate (e.g. 27.35 EUR / 1000 THB = 0.02735)
        return groupAmount.divide(sourceAmount, RATE_PRECISION, RoundingMode.HALF_UP)
    }

    // ── String-based Convenience Methods ─────────────────────────────────

    /**
     * Calculates the group amount from string inputs (UI layer convenience method).
     * Handles parsing and formatting, returning a formatted string result.
     *
     * @param sourceAmountString The source amount as entered by user
     * @param exchangeRateString The exchange rate as entered by user (source to group format)
     * @param sourceDecimalPlaces Number of decimal places for the source currency (default 2)
     * @param targetDecimalPlaces Number of decimal places for the target currency (default 2)
     * @return Formatted string representation of the calculated group amount
     */
    override fun calculateGroupAmountFromStrings(
        sourceAmountString: String,
        exchangeRateString: String,
        sourceDecimalPlaces: Int,
        targetDecimalPlaces: Int
    ): String {
        val sourceAmount = parseAmount(sourceAmountString, sourceDecimalPlaces)
        val rate = parseRate(exchangeRateString)

        val result = calculateGroupAmount(sourceAmount, rate, targetDecimalPlaces)
        return result.toPlainString()
    }

    /**
     * Calculates the group amount from source amount using a user-friendly display rate.
     *
     * The display rate is in "group to source" format (e.g., "1 EUR = 37 THB"),
     * which is the inverse of the internal calculation rate.
     *
     * @param sourceAmountString The source amount as entered by user
     * @param displayRateString The display exchange rate (1 GroupCurrency = X SourceCurrency)
     * @param sourceDecimalPlaces Number of decimal places for the source currency (default 2)
     * @param targetDecimalPlaces Number of decimal places for the target currency (default 2)
     * @return Formatted string representation of the calculated group amount
     */
    override fun calculateGroupAmountFromDisplayRate(
        sourceAmountString: String,
        displayRateString: String,
        sourceDecimalPlaces: Int,
        targetDecimalPlaces: Int
    ): String {
        val sourceAmount = parseAmount(sourceAmountString, sourceDecimalPlaces)
        val displayRate = parseRate(displayRateString)

        if (displayRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO.toPlainString()

        // Convert display rate (group to source) to calculation rate (source to group)
        // If 1 EUR = 37 THB, then 1 THB = 1/37 EUR
        val calculationRate = BigDecimal.ONE.divide(displayRate, RATE_PRECISION, RoundingMode.HALF_UP)

        val result = calculateGroupAmount(sourceAmount, calculationRate, targetDecimalPlaces)
        return result.toPlainString()
    }

    /**
     * Calculates the implied display exchange rate from source and group amounts.
     *
     * Returns the rate in user-friendly "group to source" format (e.g., "1 EUR = 37 THB"),
     * which is the inverse of the internal calculation rate.
     *
     * @param sourceAmountString The source amount as entered by user
     * @param groupAmountString The target group amount as entered by user
     * @param sourceDecimalPlaces Number of decimal places for the source currency (default 2)
     * @return Formatted string representation of the implied display exchange rate
     */
    override fun calculateImpliedDisplayRateFromStrings(
        sourceAmountString: String,
        groupAmountString: String,
        sourceDecimalPlaces: Int
    ): String {
        val sourceAmount = parseAmount(sourceAmountString, sourceDecimalPlaces)
        val targetAmount = parseAmountOrZero(groupAmountString)

        if (targetAmount.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO.toPlainString()

        // Display rate = source / target (e.g., 1000 THB / 27 EUR = 37 THB per EUR)
        val displayRate = sourceAmount.divide(targetAmount, RATE_PRECISION, RoundingMode.HALF_UP)
        return displayRate.stripTrailingZeros().toPlainString()
    }

    /**
     * Converts a display exchange rate to the internal calculation rate.
     *
     * @param displayRateString The display rate in "group to source" format
     * @return The internal rate in "source to group" format (1/displayRate)
     */
    override fun displayRateToCalculationRate(displayRateString: String): BigDecimal {
        val displayRate = parseRate(displayRateString)
        if (displayRate.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
        return BigDecimal.ONE.divide(displayRate, RATE_PRECISION, RoundingMode.HALF_UP)
    }

    /**
     * Calculates the implied exchange rate from string inputs (UI layer convenience method).
     * Handles parsing and formatting, returning a formatted string result.
     *
     * @param sourceAmountString The source amount as entered by user
     * @param groupAmountString The target group amount as entered by user
     * @param sourceDecimalPlaces Number of decimal places for the source currency (default 2)
     * @return Formatted string representation of the implied exchange rate (source to group format)
     */
    override fun calculateImpliedRateFromStrings(
        sourceAmountString: String,
        groupAmountString: String,
        sourceDecimalPlaces: Int
    ): String {
        val sourceAmount = parseAmount(sourceAmountString, sourceDecimalPlaces)
        val targetAmount = parseAmountOrZero(groupAmountString)

        val result = calculateImpliedRate(sourceAmount, targetAmount)
        return result.stripTrailingZeros().toPlainString()
    }

    // ── Exchange Rate Calculations ───────────────────────────────────────

    /**
     * Calculates the exchange rate between two amounts in their smallest currency units.
     *
     * Used to derive the rate from a cash withdrawal where the user withdrew a foreign
     * currency amount and the equivalent was deducted from the group pocket.
     *
     * @param amountWithdrawn The amount in the withdrawn currency (smallest units, e.g., cents).
     * @param deductedBaseAmount The equivalent amount deducted in the group's base currency (smallest units).
     * @return The exchange rate as BigDecimal (amountWithdrawn / deductedBaseAmount),
     *         or [BigDecimal.ONE] if deductedBaseAmount is zero or negative.
     */
    override fun calculateExchangeRate(amountWithdrawn: Long, deductedBaseAmount: Long): BigDecimal {
        if (deductedBaseAmount <= 0) return BigDecimal.ONE
        return BigDecimal(amountWithdrawn)
            .divide(BigDecimal(deductedBaseAmount), RATE_PRECISION, RoundingMode.HALF_UP)
    }

    /**
     * Computes the blended internal exchange rate from a FIFO cash expense result.
     *
     * Internal rate = groupAmountCents / sourceAmountCents
     * (i.e., "1 source unit = X group units").
     *
     * This is used to set a correct `exchangeRate` on cash expenses, replacing the
     * incorrect Open Exchange Rates API rate that the UI may have initially shown.
     *
     * @param sourceAmountCents The expense amount in the source (cash) currency, in cents.
     * @param groupAmountCents The blended cost in the group's base currency, in cents (from FIFO).
     * @return The blended internal rate, or [BigDecimal.ONE] if either input is non-positive.
     */
    override fun calculateBlendedRate(sourceAmountCents: Long, groupAmountCents: Long): BigDecimal {
        if (sourceAmountCents <= 0 || groupAmountCents <= 0) return BigDecimal.ONE
        return BigDecimal(groupAmountCents)
            .divide(BigDecimal(sourceAmountCents), RATE_PRECISION, RoundingMode.HALF_UP)
    }

    /**
     * Computes the blended display exchange rate from a FIFO cash expense result.
     *
     * Display rate = sourceAmountCents / groupAmountCents
     * (i.e., "1 group unit = X source units", e.g., "1 EUR = 37.22 THB").
     *
     * This is the user-facing rate shown in the exchange rate section when the
     * payment method is CASH.
     *
     * @param sourceAmountCents The expense amount in the source (cash) currency, in cents.
     * @param groupAmountCents The blended cost in the group's base currency, in cents (from FIFO).
     * @return The blended display rate, or [BigDecimal.ONE] if either input is non-positive.
     */
    override fun calculateBlendedDisplayRate(sourceAmountCents: Long, groupAmountCents: Long): BigDecimal {
        if (sourceAmountCents <= 0 || groupAmountCents <= 0) return BigDecimal.ONE
        return BigDecimal(sourceAmountCents)
            .divide(BigDecimal(groupAmountCents), RATE_PRECISION, RoundingMode.HALF_UP)
    }

    /**
     * Converts an amount in cents from one currency to group currency cents
     * using the user-facing display exchange rate.
     *
     * The display rate is in "group to source" format (e.g., "1 EUR = 37 THB").
     * Internally inverts it to a calculation rate and applies it to [amountCents].
     *
     * @param amountCents      The amount in the source currency's smallest unit.
     * @param displayRateString The display exchange rate string (may use locale separators).
     * @return The equivalent amount in the group currency's smallest unit, or 0 if the rate
     *         is unparseable or zero.
     */
    override fun convertCentsToGroupCurrencyViaDisplayRate(
        amountCents: Long,
        displayRateString: String
    ): Long {
        // For this path we need invalid/blank input to yield 0, not 1 as in parseRate()
        val normalized = CurrencyConverter.normalizeAmountString(displayRateString.trim())
        val displayRate = normalized.toBigDecimalOrNull() ?: return 0L
        if (displayRate.compareTo(BigDecimal.ZERO) == 0) return 0L

        val calculationRate = BigDecimal.ONE.divide(displayRate, RATE_PRECISION, RoundingMode.HALF_UP)

        return BigDecimal(amountCents)
            .multiply(calculationRate)
            .setScale(0, RoundingMode.HALF_UP)
            .toLong()
    }

    // ── Private Parsing Helpers ──────────────────────────────────────────

    /**
     * Parses an amount string to BigDecimal with proper scale for the currency.
     */
    private fun parseAmount(amountString: String, decimalPlaces: Int): BigDecimal {
        val cleanString = amountString.trim()
        if (cleanString.isBlank()) return BigDecimal.ZERO

        val normalizedString = CurrencyConverter.normalizeAmountString(cleanString)

        return normalizedString.toBigDecimalOrNull()
            ?.setScale(decimalPlaces, RoundingMode.HALF_UP)
            ?: BigDecimal.ZERO
    }

    /**
     * Parses a rate string to BigDecimal.
     */
    private fun parseRate(rateString: String): BigDecimal {
        val cleanString = rateString.trim()
        if (cleanString.isBlank()) return BigDecimal.ONE

        val normalizedString = CurrencyConverter.normalizeAmountString(cleanString)

        return normalizedString.toBigDecimalOrNull() ?: BigDecimal.ONE
    }

    /**
     * Parses an amount string to BigDecimal without scale adjustment.
     */
    private fun parseAmountOrZero(amountString: String): BigDecimal {
        val cleanString = amountString.trim()
        if (cleanString.isBlank()) return BigDecimal.ZERO

        val normalizedString = CurrencyConverter.normalizeAmountString(cleanString)

        return normalizedString.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
}
