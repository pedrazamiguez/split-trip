package es.pedrazamiguez.splittrip.domain.service.split

import es.pedrazamiguez.splittrip.domain.constant.DomainConstants
import es.pedrazamiguez.splittrip.domain.model.SplitPreviewShare
import java.math.BigDecimal

interface SplitPreviewService {
    fun distributePercentagesEvenly(sourceAmountCents: Long, participantIds: List<String>): List<SplitPreviewShare>
    fun redistributeRemainingPercentage(
        editedPercentage: BigDecimal,
        sourceAmountCents: Long,
        otherParticipantIds: List<String>,
        lockedPercentages: Map<String, BigDecimal> = emptyMap()
    ): List<SplitPreviewShare>
    fun calculateAmountFromPercentage(percentage: BigDecimal, sourceAmountCents: Long): Long
    fun parseAmountToCents(input: String, decimalDigits: Int = DomainConstants.DEFAULT_DECIMAL_PLACES): Long
    fun parseToDecimal(input: String): BigDecimal
    fun parseToDecimalOrNull(input: String): BigDecimal?
}
