package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.service.addon.AddOnResolverFactory
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Service for add-on-specific calculations: resolution, totalling, effective
 * amounts (with add-ons), and included-base-cost decomposition.
 *
 * Extracted from [ExpenseCalculatorService] to adhere to the single-responsibility
 * principle and keep each service below the configured function-count threshold.
 */
class AddOnCalculationService(
    private val addOnResolverFactory: AddOnResolverFactory = AddOnResolverFactory()
) {

    private companion object {
        const val RATE_PRECISION = 6
    }

    // ── Add-On Amount Resolution ────────────────────────────────────────

    /**
     * Resolves an add-on's user input into the absolute amount in the add-on's
     * own currency (minor units / cents).
     *
     * For [AddOnValueType.EXACT], converts the normalized input to cents directly
     * (`input × 10^decimalDigits`).
     *
     * For [AddOnValueType.PERCENTAGE], computes the percentage of the source amount:
     * `sourceAmountCents × input / 100`.
     *
     * @param normalizedInput  The user input already normalized to a parseable [BigDecimal]
     *                         (e.g., via [CurrencyConverter.normalizeAmountString]).
     * @param valueType        Whether the user entered an exact amount or a percentage.
     * @param decimalDigits    Number of decimal places for the add-on's currency.
     * @param sourceAmountCents The expense's source amount in cents — used only for
     *                          [AddOnValueType.PERCENTAGE].
     * @return The resolved amount in minor units, or 0 if [sourceAmountCents] is non-positive
     *         when value type is PERCENTAGE.
     */
    fun resolveAddOnAmountCents(
        normalizedInput: BigDecimal,
        valueType: AddOnValueType,
        decimalDigits: Int,
        sourceAmountCents: Long
    ): Long = addOnResolverFactory.create(valueType).resolve(
        normalizedInput = normalizedInput,
        decimalDigits = decimalDigits,
        sourceAmountCents = sourceAmountCents
    )

    /**
     * Resolves the absolute discount amount in minor units for an INCLUDED DISCOUNT
     * add-on whose user input is a PERCENTAGE.
     *
     * The generic [resolveAddOnAmountCents] formula (`source × pct / 100`) is wrong
     * for this combination because the source amount the user entered is the
     * **post-discount** price. The pre-discount base cost is
     * `source / (1 − pct/100)`, so the embedded discount equals
     * `source / (1 − pct/100) − source`, which simplifies to
     * `source × pct / (100 − pct)`.
     *
     * @param sourceAmountCents   Paid (post-discount) amount in the add-on's currency minor units.
     * @param discountPercentage  The discount percentage (e.g., `10` for 10 %).
     * @return The embedded discount in minor units; `0` when [sourceAmountCents] is non-positive
     *         or when [discountPercentage] is ≥ 100 (degenerate case — no positive base cost exists).
     */
    fun calculateIncludedDiscountPercentageCents(
        sourceAmountCents: Long,
        discountPercentage: BigDecimal
    ): Long {
        if (sourceAmountCents <= 0L) return 0L
        if (discountPercentage.compareTo(BigDecimal.ZERO) <= 0) return 0L
        val divisor = BigDecimal("100").subtract(discountPercentage)
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) return 0L
        return BigDecimal(sourceAmountCents)
            .multiply(discountPercentage)
            .divide(divisor, 0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(0L)
    }

    // ── Add-On Totals ───────────────────────────────────────────────────

    /**
     * Sums the [AddOn.groupAmountCents] of all ON_TOP add-ons that are NOT discounts.
     *
     * Returns only ON_TOP extras (fees, tips, surcharges added on top of the base).
     * INCLUDED add-ons are excluded because they are part of the original total
     * (extracted from it), not additional costs.
     * Discounts are handled separately in [calculateEffectiveGroupAmount].
     *
     * @param addOns The list of add-ons attached to an expense.
     * @return The total group-currency amount of on-top, non-discount add-ons.
     */
    fun calculateTotalOnTopAddOns(addOns: List<AddOn>): Long =
        addOns.filter { it.mode == AddOnMode.ON_TOP && it.type != AddOnType.DISCOUNT }
            .sumOf { it.groupAmountCents }

    /**
     * Sums the [AddOn.groupAmountCents] of ALL non-discount add-ons, regardless of mode.
     *
     * This is the value shown as "Extras" in the group balance screen — it covers both
     * ON_TOP add-ons (e.g., ATM fee, bank fee) and INCLUDED add-ons (e.g., tip already
     * embedded in the total). Discounts are excluded because they reduce the price
     * paid but are not extra costs to surface in the summary.
     *
     * Contrast with [calculateTotalOnTopAddOns] which only counts ON_TOP mode.
     *
     * @param addOns The list of add-ons attached to an expense.
     * @return The total group-currency amount of all non-discount add-ons.
     */
    fun calculateTotalAddOnExtras(addOns: List<AddOn>): Long =
        addOns.filter { it.type != AddOnType.DISCOUNT }
            .sumOf { it.groupAmountCents }

    // ── Effective Amount Calculations ────────────────────────────────────

    /**
     * Computes the effective group debt for an expense, accounting for add-ons.
     *
     * Formula: `baseGroupAmount + ON_TOP (non-discount) + INCLUDED (non-discount) − ON_TOP DISCOUNT`
     *
     * - **ON_TOP** add-ons (fees, tips, surcharges) increase the total.
     * - **INCLUDED** add-ons reconstruct the original user-entered total from the
     *   decomposed base cost stored in [baseGroupAmount].
     * - **ON_TOP DISCOUNT** add-ons reduce the total.
     * - **INCLUDED DISCOUNT** add-ons are **informational only** — the user already
     *   entered the discounted price, so they do not affect the effective amount.
     *
     * ON_TOP discount [AddOn.groupAmountCents] are zeroed during submission after
     * being baked into [baseGroupAmount], preventing double-subtraction in post-save
     * callers (balance use cases). Before save (display preview), the non-zero value
     * is subtracted to show the correct effective total.
     *
     * When [addOns] is empty the result equals [baseGroupAmount] — no behavioral
     * change for expenses without add-ons.
     *
     * @param baseGroupAmount The expense's `groupAmount` (base cost, in minor units).
     * @param addOns The structured add-ons list.
     * @return The effective group amount in minor units.
     */
    fun calculateEffectiveGroupAmount(baseGroupAmount: Long, addOns: List<AddOn>): Long {
        if (addOns.isEmpty()) return baseGroupAmount

        val onTop = addOns
            .filter { it.mode == AddOnMode.ON_TOP && it.type != AddOnType.DISCOUNT }
            .sumOf { it.groupAmountCents }

        val included = addOns
            .filter { it.mode == AddOnMode.INCLUDED && it.type != AddOnType.DISCOUNT }
            .sumOf { it.groupAmountCents }

        val onTopDiscounts = addOns
            .filter { it.type == AddOnType.DISCOUNT && it.mode == AddOnMode.ON_TOP }
            .sumOf { it.groupAmountCents }

        return (baseGroupAmount + onTop + included - onTopDiscounts).coerceAtLeast(0L)
    }

    /**
     * Computes the base cost of an expense after extracting INCLUDED add-ons.
     *
     * INCLUDED add-ons are portions already contained within the total. The base cost
     * is the remaining amount once those portions are removed:
     *
     * 1. **EXACT** INCLUDED add-ons: non-discount amounts are subtracted, discount
     *    amounts are added back (reversing the discount that reduced the original price):
     *    `afterExact = total − nonDiscountExactCents + discountExactCents`
     *
     * 2. **PERCENTAGE** INCLUDED add-ons are extracted via division. Discount percentages
     *    reduce the divisor (they were subtracted from the original), non-discount
     *    percentages increase it (they were added to the base):
     *    `baseCost = afterExact / (1 + nonDiscountPct/100 − discountPct/100)`
     *
     * @param totalAmountCents       The total expense amount in minor units (group currency).
     * @param includedExactCents     Sum of group-currency cents for EXACT INCLUDED
     *                               non-discount add-ons.
     * @param totalIncludedPercentage Combined percentage of PERCENTAGE INCLUDED
     *                                non-discount add-ons (e.g., 20 for 20 %).
     * @param includedExactDiscountCents Sum of group-currency cents for EXACT INCLUDED
     *                                   discount add-ons. Defaults to 0.
     * @param totalIncludedDiscountPercentage Combined percentage of PERCENTAGE INCLUDED
     *                                        discount add-ons (e.g., 10 for 10 %).
     *                                        Defaults to [BigDecimal.ZERO].
     * @return The derived base cost in minor units, never negative.
     */
    fun calculateIncludedBaseCost(
        totalAmountCents: Long,
        includedExactCents: Long,
        totalIncludedPercentage: BigDecimal,
        includedExactDiscountCents: Long = 0L,
        totalIncludedDiscountPercentage: BigDecimal = BigDecimal.ZERO
    ): Long {
        val noExact = includedExactCents == 0L && includedExactDiscountCents == 0L
        val noPercentage = totalIncludedPercentage.compareTo(BigDecimal.ZERO) == 0 &&
            totalIncludedDiscountPercentage.compareTo(BigDecimal.ZERO) == 0
        if (noExact && noPercentage) return totalAmountCents

        // Discounts are added back: total = original − discount, so original = total + discount
        val afterExact = totalAmountCents - includedExactCents + includedExactDiscountCents

        if (noPercentage) return afterExact.coerceAtLeast(0L)

        val nonDiscountFraction = totalIncludedPercentage.divide(
            BigDecimal("100"),
            RATE_PRECISION,
            RoundingMode.HALF_UP
        )
        val discountFraction = totalIncludedDiscountPercentage.divide(
            BigDecimal("100"),
            RATE_PRECISION,
            RoundingMode.HALF_UP
        )
        // Non-discount add-ons increase the base, discounts decrease it
        val divisor = BigDecimal.ONE.add(nonDiscountFraction).subtract(discountFraction)

        // Guard against non-positive divisors (e.g., discount ≥ 100% → divisor ≤ 0)
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) {
            return afterExact.coerceAtLeast(0L)
        }

        return BigDecimal(afterExact)
            .divide(divisor, 0, RoundingMode.HALF_UP)
            .toLong()
            .coerceAtLeast(0L)
    }

    /**
     * Computes the effective deducted amount for a cash withdrawal, including ATM fee add-ons.
     *
     * ATM fee add-ons are ON_TOP by nature — they increase the real cost of the withdrawal
     * that should be reflected in the group's pocket balance.
     *
     * @param baseDeductedAmount The withdrawal's raw `deductedBaseAmount` (in group currency minor units).
     * @param addOns The structured add-ons list (typically a single ATM fee).
     * @return The effective deducted amount in minor units.
     */
    fun calculateEffectiveDeductedAmount(baseDeductedAmount: Long, addOns: List<AddOn>): Long {
        if (addOns.isEmpty()) return baseDeductedAmount
        val addOnTotal = addOns
            .filter { it.mode == AddOnMode.ON_TOP }
            .sumOf { it.groupAmountCents }
        return baseDeductedAmount + addOnTotal
    }

    // ── Utility ─────────────────────────────────────────────────────────

    /**
     * Converts an amount in group currency cents back to the add-on's own currency
     * using the add-on's exchange rate.
     *
     * `result = groupAmountCents / exchangeRate`
     *
     * @param groupAmountCents The amount in the group currency's minor units.
     * @param exchangeRate     The add-on's exchange rate (add-on currency → group currency).
     * @return The equivalent amount in the add-on's own currency, or [groupAmountCents]
     *         if the exchange rate is zero.
     */
    fun convertGroupToSourceCents(groupAmountCents: Long, exchangeRate: BigDecimal): Long {
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) return groupAmountCents
        return BigDecimal(groupAmountCents)
            .divide(exchangeRate, 0, RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Sums the parsed percentage values from a list of raw amount input strings.
     *
     * Each input is normalized via [CurrencyConverter.normalizeAmountString] and
     * parsed to [BigDecimal]. Unparseable inputs are treated as zero.
     *
     * Centralizes the `fold(BigDecimal.ZERO) { acc, … → acc.add(…) }` pattern
     * that was previously duplicated in presentation-layer handlers.
     *
     * @param amountInputs The raw user-entered percentage strings (may contain locale separators).
     * @return The sum of all parseable percentages.
     */
    fun sumPercentagesFromInputs(amountInputs: List<String>): BigDecimal =
        amountInputs.fold(BigDecimal.ZERO) { acc, input ->
            val normalized = CurrencyConverter.normalizeAmountString(input.trim())
            acc.add(normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO)
        }
}
