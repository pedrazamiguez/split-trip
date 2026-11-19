package es.pedrazamiguez.splittrip.domain.service.split

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.exception.InvalidSplitException
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Each participant's share is specified as a percentage of the total.
 *
 * Validation: percentages must sum to 100 (with floating-point tolerance).
 * Converts percentages to [amountCents] using proper rounding with remainder distribution.
 */
class PercentSplitCalculator : ExpenseSplitCalculator() {

    private data class RawSplitAmount(
        val split: ExpenseSplit,
        val rawAmountCents: Long
    )

    companion object {
        private val HUNDRED = BigDecimal("100")
        private val TOLERANCE = BigDecimal("0.01")
    }

    override fun validate(totalAmountCents: Long, participantIds: List<String>, existingSplits: List<ExpenseSplit>) {
        if (participantIds.isEmpty()) {
            throw InvalidSplitException(
                splitType = SplitType.PERCENT,
                message = "At least one participant is required for percent split"
            )
        }

        val activeSplits = existingSplits.filter { it.userId in participantIds && !it.isExcluded }

        val percentageSum = activeSplits
            .mapNotNull { it.percentage }
            .fold(BigDecimal.ZERO) { acc, p -> acc.add(p) }

        if ((percentageSum.subtract(HUNDRED)).abs() > TOLERANCE) {
            throw InvalidSplitException(
                splitType = SplitType.PERCENT,
                message = "Percentages must sum to 100, but got $percentageSum"
            )
        }
    }

    override fun executeCalculation(
        totalAmountCents: Long,
        participantIds: List<String>,
        existingSplits: List<ExpenseSplit>
    ): List<ExpenseSplit> {
        val activeSplits = existingSplits
            .filter { it.userId in participantIds && !it.isExcluded }
            .sortedBy { it.userId }
        val totalAmount = BigDecimal(totalAmountCents)

        // Calculate raw cent amounts from percentages
        val rawAmounts = activeSplits.map { split ->
            val percentage = split.percentage ?: BigDecimal.ZERO
            val rawAmount = totalAmount.multiply(percentage)
                .divide(HUNDRED, 0, RoundingMode.DOWN)
                .toLong()
            RawSplitAmount(split = split, rawAmountCents = rawAmount)
        }

        // Distribute remainder (cents lost to rounding) to the first participants
        val allocatedTotal = rawAmounts.sumOf { it.rawAmountCents }
        var remainder = totalAmountCents - allocatedTotal

        return rawAmounts.map { (split, rawAmount) ->
            val extraCent = if (remainder > 0) {
                remainder--
                1L
            } else {
                0L
            }
            split.copy(amountCents = rawAmount + extraCent)
        }
    }
}
