package es.pedrazamiguez.splittrip.domain.service

import java.math.BigDecimal

interface RemainderDistributionService {
    fun distributeByWeights(total: Long, weights: List<BigDecimal>): List<Long>
    fun rescaleAmounts(
        originalTotal: Long,
        newTotal: Long,
        amounts: List<Long>,
        isExcluded: List<Boolean> = emptyList()
    ): List<Long>
    fun distributePercentages(
        remainingPercentage: BigDecimal,
        amounts: List<Long>,
        totalCents: Long
    ): List<BigDecimal>
}
