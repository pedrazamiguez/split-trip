package es.pedrazamiguez.splittrip.features.expense.presentation.viewmodel.handler

import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.split.SplitPreviewService
import es.pedrazamiguez.splittrip.features.expense.presentation.model.SplitUiModel
import java.math.BigDecimal

/**
 * Stateless delegate that handles the two-level entity flattening and
 * optional percentage redistribution for subunit-aware expense splits.
 *
 * Extracted from [AddExpenseSplitUiMapper.mapEntitySplitsToDomain] to reduce
 * cognitive complexity while keeping the mapping logic testable.
 */
class EntitySplitFlattenDelegate(
    private val splitPreviewService: SplitPreviewService,
    private val remainderDistributionService: RemainderDistributionService
) {

    /**
     * Flattens entity-level splits into per-user [ExpenseSplit] entries.
     *
     * Solo entities produce a single split entry. Subunit entities produce
     * one entry per nested member row.
     */
    fun flattenEntities(
        entitySplits: List<SplitUiModel>,
        splitType: SplitType
    ): List<ExpenseSplit> {
        val result = mutableListOf<ExpenseSplit>()
        for (entity in entitySplits) {
            if (entity.isExcluded) continue
            if (entity.entityMembers.isEmpty()) {
                result.add(buildSoloSplit(entity, splitType))
            } else {
                val intraType = entity.entitySplitType
                    ?.let { runCatching { SplitType.valueOf(it.id) }.getOrNull() }
                    ?: SplitType.EQUAL
                result.addAll(buildMemberSplits(entity, intraType))
            }
        }
        return result
    }

    /**
     * When [splitType] is PERCENT, distributes effective percentages across
     * members that don't have an explicit percentage set, using DOWN rounding
     * + remainder distribution so the total sums to exactly 100.00.
     *
     * Returns the adjusted list, or the original if no redistribution is needed.
     */
    fun redistributePercentagesIfNeeded(
        result: List<ExpenseSplit>,
        splitType: SplitType
    ): List<ExpenseSplit> {
        if (splitType != SplitType.PERCENT) return result

        val totalCents = result.sumOf { it.amountCents }
        if (totalCents <= 0) return result

        val hundredBd = BigDecimal("100")
        val (withPct, withoutPct) = result.partition { it.percentage != null }
        if (withoutPct.isEmpty()) return result

        val claimedPct = withPct.sumOf { it.percentage ?: BigDecimal.ZERO }
        val remainingPct = hundredBd.subtract(claimedPct)
        val withoutPctTotal = withoutPct.sumOf { it.amountCents }

        if (withoutPctTotal <= 0) return result

        val amounts = withoutPct.map { it.amountCents }
        val distributedPcts = remainderDistributionService.distributePercentages(
            remainingPercentage = remainingPct,
            amounts = amounts,
            totalCents = withoutPctTotal
        )

        val updatedWithoutPct = withoutPct.mapIndexed { index, split ->
            split.copy(percentage = distributedPcts[index])
        }

        return withPct + updatedWithoutPct
    }

    // ── Internal helpers (testable) ──────────────────────────────────────

    internal fun buildSoloSplit(entity: SplitUiModel, splitType: SplitType): ExpenseSplit =
        ExpenseSplit(
            userId = entity.userId,
            amountCents = entity.amountCents,
            percentage = if (splitType == SplitType.PERCENT) {
                splitPreviewService.parseToDecimalOrNull(entity.percentageInput)
            } else {
                null
            },
            subunitId = null
        )

    internal fun buildMemberSplits(entity: SplitUiModel, intraType: SplitType): List<ExpenseSplit> =
        entity.entityMembers.map { member ->
            ExpenseSplit(
                userId = member.userId,
                amountCents = member.amountCents,
                percentage = if (intraType == SplitType.PERCENT) {
                    splitPreviewService.parseToDecimalOrNull(member.percentageInput)
                } else {
                    null
                },
                subunitId = member.subunitId ?: entity.userId,
                splitType = intraType
            )
        }
}
