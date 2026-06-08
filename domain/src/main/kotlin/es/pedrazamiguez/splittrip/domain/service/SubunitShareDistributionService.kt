package es.pedrazamiguez.splittrip.domain.service

import java.math.BigDecimal

interface SubunitShareDistributionService {
    fun distributeEvenly(memberIds: List<String>): Map<String, BigDecimal>
    fun redistributeRemaining(
        editedShare: BigDecimal,
        otherMemberIds: List<String>,
        lockedShares: Map<String, BigDecimal> = emptyMap()
    ): Map<String, BigDecimal>
    fun parseShareTexts(
        selectedMemberIds: List<String>,
        memberShareTexts: Map<String, String>
    ): Map<String, BigDecimal>
    fun validateShareTexts(
        selectedMemberIds: List<String>,
        memberShareTexts: Map<String, String>
    ): ShareTextValidation

    sealed interface ShareTextValidation {
        /** Shares are valid (or all blank — auto-normalize at save time). */
        data object Valid : ShareTextValidation

        /** One or more share entries could not be parsed as a number. */
        data object Unparseable : ShareTextValidation

        /** A parsed share is outside the 0 %–100 % range. */
        data object OutOfRange : ShareTextValidation

        /** Parsed shares do not sum to 100 % (within tolerance). */
        data object SumMismatch : ShareTextValidation
    }
}
