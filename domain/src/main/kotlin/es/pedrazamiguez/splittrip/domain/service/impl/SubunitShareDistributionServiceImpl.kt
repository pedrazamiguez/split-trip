package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.converter.CurrencyConverter
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService.ShareTextValidation
import java.math.BigDecimal
import java.math.RoundingMode

class SubunitShareDistributionServiceImpl : SubunitShareDistributionService {

    companion object {
        private const val SHARE_SCALE = 10
        private val ONE = BigDecimal.ONE
        private val HUNDRED = BigDecimal("100")
        private val SHARE_SUM_TOLERANCE = BigDecimal("0.001")
    }

    /**
     * Distributes shares evenly among [memberIds].
     *
     * Uses [BigDecimal] for precise division.
     *
     * @return Map of userId → share where all values sum to ~1.
     *         E.g., 3 members → {A: 0.3333333333, B: 0.3333333333, C: 0.3333333333}
     */
    override fun distributeEvenly(memberIds: List<String>): Map<String, BigDecimal> {
        if (memberIds.isEmpty()) return emptyMap()

        val count = BigDecimal(memberIds.size)
        val equalShare = ONE.divide(count, SHARE_SCALE, RoundingMode.DOWN)

        return memberIds.associateWith { equalShare }
    }

    /**
     * Redistributes the remaining share (1 − [editedShare] − sum([lockedShares]))
     * evenly among [otherMemberIds] when a user manually edits their own share.
     *
     * Locked members (whose shares were previously set by the user) are excluded
     * from redistribution. Their share values are subtracted from the remaining
     * budget before the even split.
     *
     * @param editedShare The share value (0–1) the user typed, as [BigDecimal].
     * @param otherMemberIds The other selected members (excluding the editor).
     * @param lockedShares Map of userId → locked share (0–1) for members whose
     *                     values should not be overwritten. Default empty (backward-compatible).
     * @return Map of userId → share for the unlocked other members.
     *         Returns empty map if there are no unlocked other members.
     */
    override fun redistributeRemaining(
        editedShare: BigDecimal,
        otherMemberIds: List<String>,
        lockedShares: Map<String, BigDecimal>
    ): Map<String, BigDecimal> {
        if (otherMemberIds.isEmpty()) return emptyMap()

        // Only consider locked shares for members that are actually in otherMemberIds
        val otherMemberIdSet = otherMemberIds.toSet()
        val filteredLockedShares = lockedShares.filterKeys { it in otherMemberIdSet }

        val lockedTotal = filteredLockedShares.values.fold(BigDecimal.ZERO) { acc, v -> acc.add(v) }
        val remaining = ONE.subtract(editedShare).subtract(lockedTotal).coerceAtLeast(BigDecimal.ZERO)

        val unlockedIds = otherMemberIds.filter { it !in filteredLockedShares }
        if (unlockedIds.isEmpty()) return emptyMap()

        val count = BigDecimal(unlockedIds.size)
        val otherShare = remaining.divide(count, SHARE_SCALE, RoundingMode.DOWN)

        return unlockedIds.associateWith { otherShare }
    }

    /**
     * Parses user-entered percentage text values into domain-level share [BigDecimal]s (0–1).
     *
     * Uses [CurrencyConverter.normalizeAmountString] to handle locale-specific
     * decimal separators (e.g., "33,33" → "33.33") before parsing.
     *
     * - If all entries are blank or map is empty, returns empty map
     *   (signals auto-normalization to [SubunitValidationService]).
     * - If any selected member has an unparseable non-blank entry,
     *   returns empty map (fall back to auto-normalization).
     * - Otherwise converts each "50" → 0.5, "33.33" → 0.3333, etc.
     *
     * @param selectedMemberIds The currently selected member IDs.
     * @param memberShareTexts Map of userId → raw percentage text from the form.
     * @return Map of userId → share (0–1), or empty map for auto-normalization.
     */
    override fun parseShareTexts(
        selectedMemberIds: List<String>,
        memberShareTexts: Map<String, String>
    ): Map<String, BigDecimal> {
        if (memberShareTexts.isEmpty()) return emptyMap()

        val allBlank = memberShareTexts.values.all { it.isBlank() }
        if (allBlank) return emptyMap()

        val parsed = selectedMemberIds.associate { userId ->
            val shareText = memberShareTexts[userId] ?: ""
            val normalized = CurrencyConverter.normalizeAmountString(shareText)
            val shareValue = normalized.toBigDecimalOrNull()
                ?.divide(HUNDRED, SHARE_SCALE, RoundingMode.HALF_UP)
            userId to shareValue
        }

        // If any selected member has an unparseable (non-blank) entry,
        // fall back to auto-normalization rather than silently using 0.
        if (parsed.any { (userId, value) ->
                value == null && memberShareTexts[userId]?.isNotBlank() == true
            }
        ) {
            return emptyMap()
        }

        return parsed.mapValues { it.value ?: BigDecimal.ZERO }
    }

    /**
     * Validates raw share percentage texts before the user advances past the
     * Shares wizard step.
     *
     * Delegates to [parseShareTexts] for parsing, then checks:
     * 1. Each share is in [0, 1] (i.e. 0 %–100 %).
     * 2. All shares sum to ≈ 1.0 within [SHARE_SUM_TOLERANCE].
     *
     * @return A [ShareTextValidation] result the ViewModel can map to a UI message.
     */
    override fun validateShareTexts(
        selectedMemberIds: List<String>,
        memberShareTexts: Map<String, String>
    ): ShareTextValidation {
        val hasNonBlankShares = memberShareTexts.values.any { it.isNotBlank() }
        val parsed = parseShareTexts(selectedMemberIds, memberShareTexts)
        val total = parsed.values.fold(BigDecimal.ZERO) { acc, s -> acc.add(s) }

        return when {
            // All blank → will be auto-normalized at save time; valid for advancing
            parsed.isEmpty() && !hasNonBlankShares -> ShareTextValidation.Valid
            // Non-blank but parsing returned empty → unparseable input
            parsed.isEmpty() -> ShareTextValidation.Unparseable
            // Range check: each share must be in [0, 1]
            parsed.any { (_, share) -> share < BigDecimal.ZERO || share > ONE } ->
                ShareTextValidation.OutOfRange
            // Sum check: shares must add up to ~1.0
            total.subtract(ONE).abs() > SHARE_SUM_TOLERANCE -> ShareTextValidation.SumMismatch
            else -> ShareTextValidation.Valid
        }
    }

    private fun BigDecimal.coerceAtLeast(minimum: BigDecimal): BigDecimal = if (this < minimum) minimum else this
}
