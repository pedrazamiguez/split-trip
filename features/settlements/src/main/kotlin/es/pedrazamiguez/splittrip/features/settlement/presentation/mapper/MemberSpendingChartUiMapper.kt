package es.pedrazamiguez.splittrip.features.settlement.presentation.mapper

import es.pedrazamiguez.splittrip.core.common.enums.SelfIdentificationContextEnum
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.core.designsystem.presentation.formatter.formatCurrencyAmount
import es.pedrazamiguez.splittrip.core.designsystem.presentation.mapper.UserUiMapper
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingBarUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SpilloverSegment
import kotlinx.collections.immutable.toImmutableList

class MemberSpendingChartUiMapper(
    private val localeProvider: LocaleProvider,
    private val userUiMapper: UserUiMapper,
    private val pocketDebtDistributionService: PocketDebtDistributionService
) {
    fun toChartUiModel(
        memberBalances: List<MemberBalance>,
        cashOnly: Boolean,
        currentUserId: String?,
        memberProfiles: Map<String, User>,
        groupCurrencyCode: String
    ): MemberSpendingChartUiModel {
        val sortedMembers = memberBalances.sortedWith(
            compareByDescending<MemberBalance> { it.userId == currentUserId }
                .thenBy { resolveDisplayName(it.userId, memberProfiles, currentUserId) }
        )

        val analysis = analyzeMemberSpends(sortedMembers, cashOnly, groupCurrencyCode)

        val bars = sortedMembers.mapIndexed { index, balance ->
            MemberSpendingBarUiModel(
                userId = balance.userId,
                displayName = resolveDisplayName(balance.userId, memberProfiles, currentUserId),
                isCurrentUser = balance.userId == currentUserId,
                allowanceCents = analysis.allowances[index] ?: 0L,
                formattedAllowance = formatCurrencyAmount(
                    amount = analysis.allowances[index] ?: 0L,
                    currencyCode = groupCurrencyCode,
                    locale = localeProvider.getCurrentLocale()
                ),
                formattedTotalSpent = formatCurrencyAmount(
                    amount = if (cashOnly) balance.cashSpent else balance.totalSpent,
                    currencyCode = groupCurrencyCode,
                    locale = localeProvider.getCurrentLocale()
                ),
                ownSpendingCents = analysis.ownSpends[index] ?: 0L,
                spilloverSegments = (analysis.spilloverAllocations[index] ?: emptyList()).toImmutableList(),
                memberColorIndex = index
            )
        }.toImmutableList()

        return MemberSpendingChartUiModel(
            bars = bars,
            formattedGroupTotal = formatCurrencyAmount(
                amount = analysis.totalAllowance,
                currencyCode = groupCurrencyCode,
                locale = localeProvider.getCurrentLocale()
            ),
            isCashOnly = cashOnly,
            hasCashExpenses = memberBalances.any { it.withdrawn > 0L }
        )
    }

    private fun analyzeMemberSpends(
        sortedMembers: List<MemberBalance>,
        cashOnly: Boolean,
        groupCurrencyCode: String
    ): SpendAnalysis {
        val capacities = mutableListOf<Pair<Int, Long>>()
        val overspenders = mutableListOf<Pair<Int, Long>>()
        val ownSpends = mutableMapOf<Int, Long>()
        val allowances = mutableMapOf<Int, Long>()
        var totalAllowance = 0L

        sortedMembers.forEachIndexed { index, balance ->
            val allowanceCents = if (cashOnly) balance.withdrawn else balance.contributed
            val spendingCents = if (cashOnly) balance.cashSpent else balance.totalSpent
            val ownSpendingCents = minOf(spendingCents, allowanceCents)
            val overspentCents = maxOf(0L, spendingCents - allowanceCents)

            allowances[index] = allowanceCents
            ownSpends[index] = ownSpendingCents
            totalAllowance += allowanceCents

            if (overspentCents > 0) {
                overspenders.add(index to overspentCents)
            }
            val available = allowanceCents - ownSpendingCents
            if (available > 0) {
                capacities.add(index to available)
            }
        }

        val spilloverAllocations = buildSpilloverAllocations(
            sortedMembers = sortedMembers,
            overspenders = overspenders,
            capacities = capacities,
            groupCurrencyCode = groupCurrencyCode
        )

        return SpendAnalysis(
            allowances = allowances,
            ownSpends = ownSpends,
            totalAllowance = totalAllowance,
            spilloverAllocations = spilloverAllocations
        )
    }

    private data class SpendAnalysis(
        val allowances: Map<Int, Long>,
        val ownSpends: Map<Int, Long>,
        val totalAllowance: Long,
        val spilloverAllocations: Map<Int, List<SpilloverSegment>>
    )

    private fun buildSpilloverAllocations(
        sortedMembers: List<MemberBalance>,
        overspenders: List<Pair<Int, Long>>,
        capacities: List<Pair<Int, Long>>,
        groupCurrencyCode: String
    ): Map<Int, List<SpilloverSegment>> {
        val userIndexMap = sortedMembers.mapIndexed { index, balance -> balance.userId to index }.toMap()
        val deficits = overspenders.map { (index, overspent) -> sortedMembers[index].userId to overspent }
        val caps = capacities.map { (index, cap) -> sortedMembers[index].userId to cap }

        val settlements = pocketDebtDistributionService.distribute(deficits, caps, groupCurrencyCode)

        val spilloverAllocations = mutableMapOf<Int, MutableList<SpilloverSegment>>()
        for (settlement in settlements) {
            val ownerIndex = userIndexMap[settlement.fromUserId]
            val receiverIndex = userIndexMap[settlement.toUserId]
            if (ownerIndex != null && receiverIndex != null) {
                val segments = spilloverAllocations.getOrPut(receiverIndex) { mutableListOf() }
                segments.add(SpilloverSegment(ownerColorIndex = ownerIndex, amountCents = settlement.amount))
            }
        }
        return spilloverAllocations
    }

    private fun resolveDisplayName(
        userId: String,
        memberProfiles: Map<String, User>,
        currentUserId: String?
    ): String {
        return userUiMapper.mapToDisplayName(
            user = memberProfiles[userId],
            fallbackUserId = userId,
            currentUserId = currentUserId,
            selfIdentificationContext = SelfIdentificationContextEnum.NOMINATIVE
        )
    }
}
