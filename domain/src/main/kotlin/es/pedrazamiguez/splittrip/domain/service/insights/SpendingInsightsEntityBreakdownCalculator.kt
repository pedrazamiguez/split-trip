package es.pedrazamiguez.splittrip.domain.service.insights

import es.pedrazamiguez.splittrip.domain.model.BreakdownPercentage
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingInsight
import es.pedrazamiguez.splittrip.domain.model.EntitySpendingType
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.MemberSpendingInsight
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import java.math.BigDecimal
import java.math.RoundingMode

internal data class EligibleInsightExpense(
    val expense: Expense,
    val effectiveAmountCents: Long,
    val extrasCents: Long
)

internal data class MemberAndEntityBreakdown(
    val memberBreakdown: List<MemberSpendingInsight>,
    val entityBreakdown: List<EntitySpendingInsight>,
    val hasSubunits: Boolean
)

internal data class BreakdownMetrics(
    val percentages: List<BigDecimal>,
    val breakdownPercentages: List<BreakdownPercentage>,
    val totalCents: Long
)

internal class SpendingInsightsEntityBreakdownCalculator(
    private val remainderDistributionService: RemainderDistributionService
) {

    private data class MemberAggregate(
        val userId: String,
        val effectiveSpentCents: Long,
        val expenseCount: Int
    )

    private data class EntityAggregate(
        val entityId: String,
        val type: EntitySpendingType,
        val subunitName: String?,
        val memberIds: List<String>,
        val effectiveSpentCents: Long,
        val expenseCount: Int
    )

    fun calculate(
        periodExpenses: List<EligibleInsightExpense>,
        subunits: List<Subunit>,
        groupMemberIds: List<String>
    ): MemberAndEntityBreakdown {
        val memberAmounts = mutableMapOf<String, Long>()
        val memberExpenseIds = mutableMapOf<String, MutableSet<String>>()

        accumulateMemberSpending(periodExpenses, memberAmounts, memberExpenseIds)

        val allMemberIds = (groupMemberIds + memberAmounts.keys).distinct()
        val memberBreakdown = buildMemberBreakdown(allMemberIds, memberAmounts, memberExpenseIds)
        val activeSubunits = subunits.filter { it.memberIds.isNotEmpty() }
        val entityBreakdown = buildEntityBreakdown(
            activeSubunits = activeSubunits,
            allMemberIds = allMemberIds,
            memberAmounts = memberAmounts,
            memberExpenseIds = memberExpenseIds
        )

        return MemberAndEntityBreakdown(
            memberBreakdown = memberBreakdown,
            entityBreakdown = entityBreakdown,
            hasSubunits = activeSubunits.isNotEmpty()
        )
    }

    fun computeBreakdownMetrics(
        amounts: List<Long>,
        totalCents: Long
    ): BreakdownMetrics {
        if (amounts.isEmpty()) {
            return BreakdownMetrics(
                percentages = emptyList(),
                breakdownPercentages = emptyList(),
                totalCents = 0L
            )
        }
        if (totalCents <= 0L) {
            return BreakdownMetrics(
                percentages = List(amounts.size) { BigDecimal.ZERO },
                breakdownPercentages = List(amounts.size) { BreakdownPercentage.Zero },
                totalCents = 0L
            )
        }
        return BreakdownMetrics(
            percentages = remainderDistributionService.distributePercentages(
                remainingPercentage = HUNDRED_PERCENT,
                amounts = amounts,
                totalCents = totalCents
            ),
            breakdownPercentages = remainderDistributionService.distributeBreakdownPercentages(
                amounts = amounts,
                totalCents = totalCents
            ),
            totalCents = totalCents
        )
    }

    fun computeShareRatio(amountCents: Long, totalCents: Long): BigDecimal {
        if (amountCents <= 0L || totalCents <= 0L) return BigDecimal.ZERO
        return BigDecimal.valueOf(amountCents)
            .divide(BigDecimal.valueOf(totalCents), SHARE_RATIO_SCALE, RoundingMode.HALF_UP)
    }

    private fun accumulateMemberSpending(
        periodExpenses: List<EligibleInsightExpense>,
        memberAmounts: MutableMap<String, Long>,
        memberExpenseIds: MutableMap<String, MutableSet<String>>
    ) {
        for (item in periodExpenses) {
            val activeSplits = item.expense.splits.filter { !it.isExcluded && it.amountCents > 0L }
            if (activeSplits.isEmpty()) continue

            val splitSum = activeSplits.sumOf { it.amountCents }
            val rescaledAmounts = remainderDistributionService.rescaleAmounts(
                originalTotal = splitSum,
                newTotal = item.effectiveAmountCents,
                amounts = activeSplits.map { it.amountCents }
            )

            activeSplits.forEachIndexed { index, split ->
                val amount = rescaledAmounts[index]
                memberAmounts[split.userId] = (memberAmounts[split.userId] ?: 0L) + amount
                memberExpenseIds.getOrPut(split.userId) { mutableSetOf() }.add(item.expense.id)
            }
        }
    }

    private fun buildMemberBreakdown(
        allMemberIds: List<String>,
        memberAmounts: Map<String, Long>,
        memberExpenseIds: Map<String, Set<String>>
    ): List<MemberSpendingInsight> {
        if (allMemberIds.isEmpty()) return emptyList()

        val sorted = allMemberIds
            .map { userId ->
                MemberAggregate(
                    userId = userId,
                    effectiveSpentCents = memberAmounts[userId] ?: 0L,
                    expenseCount = memberExpenseIds[userId]?.size ?: 0
                )
            }
            .sortedWith(
                compareByDescending<MemberAggregate> { it.effectiveSpentCents }
                    .thenBy { it.userId }
            )

        val totalAttributedCents = sorted.sumOf { it.effectiveSpentCents }
        val metrics = computeBreakdownMetrics(sorted.map { it.effectiveSpentCents }, totalAttributedCents)

        return sorted.mapIndexed { index, member ->
            MemberSpendingInsight(
                userId = member.userId,
                effectiveSpentCents = member.effectiveSpentCents,
                expenseCount = member.expenseCount,
                percentage = metrics.percentages[index],
                shareRatio = computeShareRatio(member.effectiveSpentCents, metrics.totalCents),
                breakdownPercentage = metrics.breakdownPercentages[index]
            )
        }
    }

    private fun buildEntityBreakdown(
        activeSubunits: List<Subunit>,
        allMemberIds: List<String>,
        memberAmounts: Map<String, Long>,
        memberExpenseIds: Map<String, Set<String>>
    ): List<EntitySpendingInsight> {
        val assignedMemberIds = activeSubunits.flatMap { it.memberIds }.toSet()
        val subunitEntities = activeSubunits.map { subunit ->
            val expenseCount = subunit.memberIds
                .flatMap { memberId -> memberExpenseIds[memberId].orEmpty() }
                .toSet()
                .size
            EntityAggregate(
                entityId = subunit.id,
                type = EntitySpendingType.SUBUNIT,
                subunitName = subunit.name,
                memberIds = subunit.memberIds,
                effectiveSpentCents = subunit.memberIds.sumOf { memberAmounts[it] ?: 0L },
                expenseCount = expenseCount
            )
        }
        val soloEntities = allMemberIds
            .filterNot { it in assignedMemberIds }
            .map { userId ->
                EntityAggregate(
                    entityId = userId,
                    type = EntitySpendingType.SOLO_MEMBER,
                    subunitName = null,
                    memberIds = listOf(userId),
                    effectiveSpentCents = memberAmounts[userId] ?: 0L,
                    expenseCount = memberExpenseIds[userId]?.size ?: 0
                )
            }

        val sorted = (subunitEntities + soloEntities).sortedWith(
            compareByDescending<EntityAggregate> { it.effectiveSpentCents }
                .thenBy { it.entityId }
        )
        if (sorted.isEmpty()) return emptyList()

        val totalEntityCents = sorted.sumOf { it.effectiveSpentCents }
        val metrics = computeBreakdownMetrics(sorted.map { it.effectiveSpentCents }, totalEntityCents)

        return sorted.mapIndexed { index, entity ->
            EntitySpendingInsight(
                entityId = entity.entityId,
                type = entity.type,
                subunitName = entity.subunitName,
                memberIds = entity.memberIds,
                effectiveSpentCents = entity.effectiveSpentCents,
                expenseCount = entity.expenseCount,
                percentage = metrics.percentages[index],
                shareRatio = computeShareRatio(entity.effectiveSpentCents, metrics.totalCents),
                breakdownPercentage = metrics.breakdownPercentages[index]
            )
        }
    }

    private companion object {
        private val HUNDRED_PERCENT = BigDecimal("100.00")
        private const val SHARE_RATIO_SCALE = 4
    }
}
