package es.pedrazamiguez.splittrip.domain.service.split.impl

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.model.SplitPreviewShare
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import java.math.BigDecimal
import java.math.RoundingMode

class SplitPreviewServiceImpl : SplitPreviewService {

    companion object {
        private val HUNDRED = BigDecimal("100")
        private val SMALLEST_PERCENT_UNIT = BigDecimal("0.01")
        private const val PERCENT_SCALE = 2
    }

    // ── Percentage Distribution ─────────────────────────────────────────

    /**
     * Distributes 100 % evenly among [participantIds] and computes the
     * corresponding [SplitPreviewShare.amountCents] from [sourceAmountCents].
     *
     * Remainder cents (from rounding 100 / N down to 2 dp) are distributed
     * one-by-one to the first participants (by sorted userId) so percentages
     * always sum to 100.00.
     *
     * Participants are sorted by userId internally for deterministic remainder
     * allocation across runs/devices.
     *
     * @param sourceAmountCents Total expense amount in smallest currency unit.
     * @param participantIds    Active (non-excluded) participant user IDs.
     * @return One [SplitPreviewShare] per participant, sorted by userId.
     */
    override fun distributePercentagesEvenly(
        sourceAmountCents: Long,
        participantIds: List<String>
    ): List<SplitPreviewShare> {
        if (participantIds.isEmpty()) return emptyList()

        val sortedIds = participantIds.sorted()
        val count = sortedIds.size
        val basePercent = HUNDRED.divide(BigDecimal(count), PERCENT_SCALE, RoundingMode.DOWN)

        val allocatedPercent = basePercent.multiply(BigDecimal(count))
        var remainderUnits = HUNDRED.subtract(allocatedPercent)
            .movePointRight(PERCENT_SCALE)
            .setScale(0, RoundingMode.DOWN)
            .toInt()

        val shares = sortedIds.map { userId ->
            val pct = if (remainderUnits > 0) {
                remainderUnits--
                basePercent.add(SMALLEST_PERCENT_UNIT)
            } else {
                basePercent
            }
            SplitPreviewShare(
                userId = userId,
                amountCents = calculateAmountFromPercentage(pct, sourceAmountCents),
                percentage = pct
            )
        }

        return distributeAmountRemainder(shares, sourceAmountCents)
    }

    /**
     * Redistributes the remaining percentage (100 − [editedPercentage] − sum([lockedPercentages]))
     * evenly among [otherParticipantIds] and computes their preview amounts.
     *
     * Called when a user manually types a percentage for their share — the
     * remaining percentage is spread across the other **unlocked** active members.
     *
     * @param editedPercentage     The percentage the user typed.
     * @param sourceAmountCents    Total expense amount in smallest currency unit.
     * @param otherParticipantIds  The other active participants (excluding the editor).
     * @param lockedPercentages    Map of userId → locked percentage for members whose
     *                             values should not be overwritten. Default empty (backward-compatible).
     * @return One [SplitPreviewShare] per unlocked other participant.
     */
    override fun redistributeRemainingPercentage(
        editedPercentage: BigDecimal,
        sourceAmountCents: Long,
        otherParticipantIds: List<String>,
        lockedPercentages: Map<String, BigDecimal>
    ): List<SplitPreviewShare> {
        if (otherParticipantIds.isEmpty()) return emptyList()

        // Only consider locked percentages for participants that are actually in otherParticipantIds
        val otherIdSet = otherParticipantIds.toSet()
        val filteredLocked = lockedPercentages.filterKeys { it in otherIdSet }

        val lockedTotal = filteredLocked.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        val remainingPct = HUNDRED.subtract(editedPercentage).subtract(lockedTotal).coerceAtLeast(BigDecimal.ZERO)

        val unlockedIds = otherParticipantIds.filter { it !in filteredLocked }.sorted()
        if (unlockedIds.isEmpty()) return emptyList()

        val otherCount = unlockedIds.size
        val otherBasePct = remainingPct.divide(BigDecimal(otherCount), PERCENT_SCALE, RoundingMode.DOWN)

        val allocatedOtherPct = otherBasePct.multiply(BigDecimal(otherCount))
        var remainderUnits = remainingPct.subtract(allocatedOtherPct)
            .movePointRight(PERCENT_SCALE)
            .setScale(0, RoundingMode.DOWN)
            .toInt()

        val shares = unlockedIds.map { userId ->
            val pct = if (remainderUnits > 0) {
                remainderUnits--
                otherBasePct.add(SMALLEST_PERCENT_UNIT)
            } else {
                otherBasePct
            }
            SplitPreviewShare(
                userId = userId,
                amountCents = calculateAmountFromPercentage(pct, sourceAmountCents),
                percentage = pct
            )
        }

        // Remainder for redistributed shares is relative to the remaining amount,
        // not the full sourceAmountCents, because the edited user's share is excluded.
        val remainingAmountCents = shares.sumOf { it.amountCents }
        val lockedAmountCents = filteredLocked.values.sumOf {
            calculateAmountFromPercentage(it, sourceAmountCents)
        }
        val expectedRemainingCents =
            sourceAmountCents - calculateAmountFromPercentage(editedPercentage, sourceAmountCents) - lockedAmountCents
        return distributeAmountRemainder(shares, expectedRemainingCents, remainingAmountCents)
    }

    // ── Amount Calculation ───────────────────────────────────────────────

    /**
     * Computes the amount in cents that corresponds to a given [percentage]
     * of [sourceAmountCents].
     *
     * Uses [RoundingMode.DOWN] to match the rounding strategy used by the
     * existing split calculators.
     *
     * @param percentage        The share percentage (e.g., 33.33).
     * @param sourceAmountCents The total expense amount in smallest currency unit.
     * @return The derived amount in cents.
     */
    override fun calculateAmountFromPercentage(percentage: BigDecimal, sourceAmountCents: Long): Long {
        if (sourceAmountCents <= 0) return 0L
        return sourceAmountCents.toBigDecimal()
            .multiply(percentage)
            .divide(HUNDRED, 0, RoundingMode.DOWN)
            .toLong()
    }

    // ── Parsing Helpers ─────────────────────────────────────────────────

    /**
     * Parses a locale-aware amount input string to cents (Long).
     *
     * Uses [CurrencyConverter.normalizeAmountString] to handle different decimal
     * separators (comma vs. dot), then scales by [decimalDigits] to convert to
     * the smallest currency unit.
     *
     * @param input         The raw input string (e.g., "10,50" or "10.50").
     * @param decimalDigits Number of decimal places for the currency (0 for JPY, 2 for EUR, 3 for TND).
     * @return Amount in smallest currency unit, or 0 if parsing fails.
     */
    override fun parseAmountToCents(input: String, decimalDigits: Int): Long = try {
        val normalized = CurrencyConverter.normalizeAmountString(input.trim())
        val amount = normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
        amount.movePointRight(decimalDigits).setScale(0, RoundingMode.HALF_UP).toLong()
    } catch (_: Exception) {
        0L
    }

    /**
     * Parses a locale-aware decimal input string to [BigDecimal].
     *
     * @param input The raw input string.
     * @return The parsed decimal value, or [BigDecimal.ZERO] if parsing fails.
     */
    override fun parseToDecimal(input: String): BigDecimal = try {
        val normalized = CurrencyConverter.normalizeAmountString(input.trim())
        normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    } catch (_: Exception) {
        BigDecimal.ZERO
    }

    /**
     * Parses a locale-aware decimal input string to [BigDecimal], returning `null`
     * when the input is blank or unparseable.
     *
     * Use this variant when the caller needs to distinguish "unset" from "explicitly zero"
     * (e.g., percentage split fields where `null` means auto-calculated).
     *
     * @param input The raw input string.
     * @return The parsed decimal value, or `null` if the input is blank or parsing fails.
     */
    override fun parseToDecimalOrNull(input: String): BigDecimal? = try {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null
        val normalized = CurrencyConverter.normalizeAmountString(trimmed)
        normalized.toBigDecimalOrNull()
    } catch (_: Exception) {
        null
    }

    // ── Private helpers ─────────────────────────────────────────────────

    /**
     * Distributes orphan cents that were lost to rounding (DOWN) when converting
     * percentages to amounts. Grants one extra cent to the first participants
     * until the total matches [expectedTotalCents].
     *
     * This mirrors the remainder distribution logic used by [PercentSplitCalculator]
     * at save time, ensuring the preview amounts always sum correctly.
     */
    private fun distributeAmountRemainder(
        shares: List<SplitPreviewShare>,
        expectedTotalCents: Long,
        currentTotalCents: Long = shares.sumOf { it.amountCents }
    ): List<SplitPreviewShare> {
        var remainder = expectedTotalCents - currentTotalCents
        if (remainder <= 0) return shares

        return shares.map { share ->
            val extraCent = if (remainder > 0) {
                remainder--
                1L
            } else {
                0L
            }
            if (extraCent > 0) share.copy(amountCents = share.amountCents + extraCent) else share
        }
    }

    private fun BigDecimal.coerceAtLeast(min: BigDecimal): BigDecimal = if (this < min) min else this
}
