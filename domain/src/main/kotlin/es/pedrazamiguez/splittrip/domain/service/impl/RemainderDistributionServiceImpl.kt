package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import java.math.BigDecimal
import java.math.RoundingMode

class RemainderDistributionServiceImpl : RemainderDistributionService {

    companion object {
        private val SMALLEST_PERCENT_UNIT = BigDecimal("0.01")
    }

    private data class PercentageCandidate(
        val originalIndex: Int,
        val floor: Int,
        val remainder: BigDecimal
    )

    /**
     * Distributes [total] proportionally across the given [weights] using
     * floor rounding, then redistributes the remainder one unit at a time
     * to the first items.
     *
     * Example: `distributeByWeights(100, [30, 70])` → `[30, 70]`
     * Example: `distributeByWeights(10, [1, 1, 1])` → `[4, 3, 3]`
     *
     * @param total   The total amount (in minor units) to distribute.
     * @param weights The proportional weights for each item. Must not be all zero.
     * @return A list of the same size as [weights] whose sum equals [total] exactly.
     */
    override fun distributeByWeights(total: Long, weights: List<BigDecimal>): List<Long> {
        if (weights.isEmpty()) return emptyList()
        if (total <= 0) return List(weights.size) { 0L }

        val totalWeight = weights.fold(BigDecimal.ZERO, BigDecimal::add)
        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) return List(weights.size) { 0L }

        val totalBd = BigDecimal(total)
        val floorAmounts = weights.map { w ->
            totalBd.multiply(w)
                .divide(totalWeight, 0, RoundingMode.DOWN)
                .toLong()
        }

        var remainder = total - floorAmounts.sum()
        return floorAmounts.map { amount ->
            if (remainder > 0) {
                remainder--
                amount + 1L
            } else {
                amount
            }
        }
    }

    /**
     * Rescales a list of [amounts] from [originalTotal] to [newTotal] proportionally,
     * using floor rounding and one-unit remainder redistribution.
     *
     * Items marked as excluded (via [isExcluded]) are not eligible for remainder
     * distribution, matching the split rescaling behavior.
     *
     * @param originalTotal The original sum of amounts.
     * @param newTotal      The target sum after rescaling.
     * @param amounts       The individual amounts to rescale.
     * @param isExcluded    Per-item exclusion flags (defaults to all false).
     * @return Rescaled amounts whose sum equals [newTotal] exactly.
     */
    override fun rescaleAmounts(
        originalTotal: Long,
        newTotal: Long,
        amounts: List<Long>,
        isExcluded: List<Boolean>
    ): List<Long> {
        if (originalTotal == newTotal || originalTotal <= 0 || amounts.isEmpty()) {
            return amounts
        }

        val originalTotalBd = BigDecimal(originalTotal)
        val newTotalBd = BigDecimal(newTotal)

        val scaled = amounts.map { amount ->
            BigDecimal(amount)
                .multiply(newTotalBd)
                .divide(originalTotalBd, 0, RoundingMode.DOWN)
                .toLong()
        }

        var remainder = newTotal - scaled.sum()
        if (remainder <= 0) return scaled

        val eligibleIndices = scaled.indices.filter { index ->
            !isExcluded.getOrElse(index) { false }
        }
        if (eligibleIndices.isEmpty()) return scaled

        val mutableScaled = scaled.toMutableList()
        var idx = 0
        while (remainder > 0) {
            mutableScaled[eligibleIndices[idx]] += 1
            remainder--
            idx = (idx + 1) % eligibleIndices.size
        }

        return mutableScaled.toList()
    }

    /**
     * Distributes [remainingPercentage] across splits proportionally to their
     * [amounts] relative to [totalCents], using DOWN rounding to 2 decimal places
     * and 0.01-unit remainder redistribution.
     *
     * Used when subunit entity members don't have explicit percentages and need
     * effective per-user percentages that sum to exactly [remainingPercentage].
     *
     * @param remainingPercentage The total percentage to distribute (e.g., 100.00 if no
     *                            splits have claimed percentages, or a smaller value if some have).
     * @param amounts             The per-split amount in cents (used as weights).
     * @param totalCents          The total amount in cents across all splits.
     * @return A list of [BigDecimal] percentages whose sum equals [remainingPercentage] exactly.
     */
    override fun distributePercentages(
        remainingPercentage: BigDecimal,
        amounts: List<Long>,
        totalCents: Long
    ): List<BigDecimal> {
        if (amounts.isEmpty() || totalCents <= 0) return emptyList()

        val totalBd = BigDecimal(totalCents)

        val rawPercentages = amounts.map { amount ->
            BigDecimal(amount)
                .multiply(remainingPercentage)
                .divide(totalBd, 2, RoundingMode.DOWN)
        }

        val allocated = rawPercentages.fold(BigDecimal.ZERO, BigDecimal::add)
        var remainderUnits = remainingPercentage.subtract(allocated)
            .divide(SMALLEST_PERCENT_UNIT, 0, RoundingMode.DOWN)
            .toInt()
            .coerceAtLeast(0)

        return rawPercentages.map { pct ->
            if (remainderUnits > 0) {
                remainderUnits--
                pct.add(SMALLEST_PERCENT_UNIT)
            } else {
                pct
            }
        }
    }

    override fun distributeBreakdownPercentages(
        amounts: List<Long>,
        totalCents: Long
    ): List<BreakdownPercentage> {
        if (amounts.isEmpty()) return emptyList()
        if (totalCents <= 0L) return List(amounts.size) { BreakdownPercentage.Zero }

        val totalBd = BigDecimal.valueOf(totalCents)
        val hundred = BigDecimal.valueOf(100)
        val result = Array<BreakdownPercentage>(amounts.size) { BreakdownPercentage.Zero }
        val candidates = mutableListOf<PercentageCandidate>()

        for (i in amounts.indices) {
            val amount = amounts[i]
            if (amount <= 0L) {
                result[i] = BreakdownPercentage.Zero
                continue
            }
            val product = BigDecimal.valueOf(amount).multiply(hundred)
            if (product < totalBd) {
                result[i] = BreakdownPercentage.SubOnePercent
            } else {
                val floor = product.divide(totalBd, 0, RoundingMode.DOWN).toInt()
                val remainder = product.remainder(totalBd)
                candidates.add(
                    PercentageCandidate(
                        originalIndex = i,
                        floor = floor,
                        remainder = remainder
                    )
                )
            }
        }

        if (candidates.isEmpty()) {
            return result.toList()
        }

        val sumOfFloors = candidates.sumOf { it.floor }
        val remainderUnits = (100 - sumOfFloors).coerceIn(0, candidates.size)

        val sortedCandidates = candidates.sortedWith(
            compareByDescending<PercentageCandidate> { it.remainder }
                .thenBy { it.originalIndex }
        )

        val bonusIndices = sortedCandidates
            .take(remainderUnits)
            .filter { it.remainder > BigDecimal.ZERO }
            .map { it.originalIndex }
            .toSet()

        for (candidate in candidates) {
            val percentage = if (candidate.originalIndex in bonusIndices) {
                candidate.floor + 1
            } else {
                candidate.floor
            }
            result[candidate.originalIndex] = BreakdownPercentage.Whole(percentage)
        }

        return result.toList()
    }
}
