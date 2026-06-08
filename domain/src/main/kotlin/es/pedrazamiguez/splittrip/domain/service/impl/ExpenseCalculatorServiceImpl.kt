package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService
import es.pedrazamiguez.splittrip.domain.service.ExpenseCalculatorService.FifoCashResult
import java.math.BigDecimal
import java.math.RoundingMode

class ExpenseCalculatorServiceImpl : ExpenseCalculatorService {

    // ── Cents Conversion ─────────────────────────────────────────────────

    /**
     * Converts cents to BigDecimal amount.
     * Centralizes the conversion logic to handle currencies with different decimal places.
     *
     * @param cents The amount in smallest currency unit
     * @param decimalPlaces Number of decimal places for the currency (default 2)
     * @return BigDecimal representation of the amount
     */
    override fun centsToBigDecimal(cents: Long, decimalPlaces: Int): BigDecimal {
        val divisor = BigDecimal.TEN.pow(decimalPlaces)
        return BigDecimal(cents).divide(divisor, decimalPlaces, RoundingMode.HALF_UP)
    }

    /**
     * Converts cents to a plain decimal string suitable for display formatting.
     *
     * Convenience wrapper that converts cents → BigDecimal → String in one call,
     * centralizing the `BigDecimal.TEN.pow()` → `divide()` → `toPlainString()` pattern
     * that was previously duplicated across presentation-layer handlers.
     *
     * @param cents         The amount in the smallest currency unit.
     * @param decimalPlaces Number of decimal places for the currency (default 2).
     * @return The plain string representation (e.g., 1550 with 2 decimal places → "15.50").
     */
    override fun centsToBigDecimalString(cents: Long, decimalPlaces: Int): String =
        centsToBigDecimal(cents, decimalPlaces).toPlainString()

    // ── Proportional & Distribution ──────────────────────────────────────

    /**
     * Computes a proportional amount using cross-multiplication.
     *
     * `result = amount × targetAmount / totalAmount`
     *
     * Used to derive the source-currency base cost from the group-currency base cost:
     * `baseCostSource = sourceAmount × baseCostGroup / groupAmount`.
     *
     * @param amount       The value to scale (e.g., source amount).
     * @param targetAmount The target proportional value (e.g., base cost in group currency).
     * @param totalAmount  The reference total (e.g., group amount).
     * @return The proportionally scaled value, or [targetAmount] if [totalAmount] is zero
     *         or equals [amount].
     */
    override fun computeProportionalAmount(amount: Long, targetAmount: Long, totalAmount: Long): Long {
        if (amount == totalAmount || totalAmount == 0L) return targetAmount
        return BigDecimal(amount)
            .multiply(BigDecimal(targetAmount))
            .divide(BigDecimal(totalAmount), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    /**
     * Distributes a total amount equally among a number of users, ensuring
     * the sum of all allocations equals the total exactly (conservation of currency).
     *
     * Uses floor division to compute a base share, then distributes the
     * fractional remainder (in smallest currency units) sequentially to
     * the first users.
     *
     * Example: distributeAmount(BigDecimal("10.00"), 3, 2) →
     *   [BigDecimal("3.34"), BigDecimal("3.33"), BigDecimal("3.33")]
     *   Sum = 10.00 ✓
     *
     * @param totalAmount The total amount to distribute.
     * @param numberOfUsers The number of users to split among. Must be > 0.
     * @param decimalPlaces Number of decimal places for the currency (default 2).
     * @return A list of BigDecimal allocations whose sum equals totalAmount exactly.
     * @throws IllegalArgumentException if numberOfUsers <= 0.
     */
    override fun distributeAmount(
        totalAmount: BigDecimal,
        numberOfUsers: Int,
        decimalPlaces: Int
    ): List<BigDecimal> {
        require(numberOfUsers > 0) { "Number of users must be greater than zero" }

        // Normalize totalAmount to the target scale to prevent sub-smallest-unit fractions
        val normalizedTotal = totalAmount.setScale(decimalPlaces, RoundingMode.HALF_UP)

        val divisor = BigDecimal(numberOfUsers)

        // Floor-divide: truncate (round down) to the target decimal places
        val baseShare = normalizedTotal.divide(divisor, decimalPlaces, RoundingMode.DOWN)

        // Remainder = total - (baseShare * numberOfUsers)
        val allocatedTotal = baseShare.multiply(divisor)
        val remainder = normalizedTotal.subtract(allocatedTotal)

        // Express remainder in smallest currency units (e.g., cents).
        // Use movePointRight for exact integer conversion and RoundingMode.DOWN
        // to guarantee extraUnits never exceeds the actual remainder.
        val extraUnits = remainder.movePointRight(decimalPlaces)
            .setScale(0, RoundingMode.DOWN)
            .intValueExact()

        // Build result: first `extraUnits` users get baseShare + 1 smallest unit
        val smallestUnit = BigDecimal.ONE.movePointLeft(decimalPlaces)
        return List(numberOfUsers) { index ->
            if (index < extraUnits) {
                baseShare.add(smallestUnit)
            } else {
                baseShare
            }
        }
    }

    // ── FIFO Cash Operations ─────────────────────────────────────────────

    /**
     * Checks whether the available cash withdrawals are insufficient to cover the requested amount.
     *
     * @param amountToCover The expense amount in the cash currency (in cents).
     * @param availableWithdrawals List of withdrawals with remaining balance, ordered by createdAt asc.
     * @return true if total remaining is less than amountToCover.
     */
    override fun hasInsufficientCash(amountToCover: Long, availableWithdrawals: List<CashWithdrawal>): Boolean {
        val totalAvailable = availableWithdrawals.sumOf { it.remainingAmount }
        return totalAvailable < amountToCover
    }

    /**
     * Applies the FIFO algorithm to determine how an expense should consume cash from
     * multiple ATM withdrawals, each with their own historical exchange rate.
     *
     * Iterates over withdrawals ordered by createdAt ascending (oldest first), deducting
     * from each until the expense is fully covered.
     *
     * @param amountToCover The total expense amount in the cash currency (in cents).
     * @param availableWithdrawals Withdrawals with remaining balance > 0, ordered by createdAt asc.
     * @return A [FifoCashResult] containing the blended base currency cost and the tranches consumed.
     * @throws IllegalStateException if available cash is insufficient.
     */
    override fun calculateFifoCashAmount(
        amountToCover: Long,
        availableWithdrawals: List<CashWithdrawal>
    ): FifoCashResult {
        require(amountToCover > 0) { "Amount to cover must be greater than zero" }
        check(!hasInsufficientCash(amountToCover, availableWithdrawals)) {
            "Insufficient cash. Required: $amountToCover, Available: ${availableWithdrawals.sumOf {
                it.remainingAmount
            }}"
        }

        var remaining = amountToCover
        val tranches = mutableListOf<CashTranche>()
        var totalBaseAmountCents = 0L

        for (withdrawal in availableWithdrawals) {
            if (remaining <= 0) break

            val consumed = minOf(remaining, withdrawal.remainingAmount)
            tranches.add(
                CashTranche(
                    withdrawalId = withdrawal.id,
                    amountConsumed = consumed
                )
            )

            // Calculate the base currency equivalent for this tranche using
            // the withdrawal's historical exchange rate.
            // rate = deductedBaseAmount / amountWithdrawn (base per cash unit)
            val rate = if (withdrawal.amountWithdrawn > 0) {
                BigDecimal(withdrawal.deductedBaseAmount)
                    .divide(
                        BigDecimal(withdrawal.amountWithdrawn),
                        DomainConstants.RATE_PRECISION,
                        RoundingMode.HALF_UP
                    )
            } else {
                BigDecimal.ZERO
            }

            val baseAmountForTranche = BigDecimal(consumed)
                .multiply(rate)
                .setScale(0, RoundingMode.HALF_UP)
                .toLong()

            totalBaseAmountCents += baseAmountForTranche
            remaining -= consumed
        }

        return FifoCashResult(
            groupAmountCents = totalBaseAmountCents,
            tranches = tranches
        )
    }
}
