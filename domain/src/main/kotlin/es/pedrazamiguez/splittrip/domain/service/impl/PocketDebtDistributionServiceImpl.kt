package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService

class PocketDebtDistributionServiceImpl : PocketDebtDistributionService {

    override fun distributePocketDebts(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> {
        val deficits = memberBalances
            .filter { it.pocketBalance < 0L }
            .map { it.userId to -it.pocketBalance }

        val capacities = memberBalances
            .filter { it.pocketBalance > 0L }
            .map { it.userId to it.pocketBalance }

        return distribute(deficits, capacities, groupCurrency)
    }

    override fun distribute(
        deficits: List<Pair<String, Long>>,
        capacities: List<Pair<String, Long>>,
        groupCurrency: String
    ): List<Settlement> {
        val validDeficits = deficits
            .filter { it.second > 0L }
            .sortedByDescending { it.second }

        val remainingCapacities = initialCapacities(capacities)
        if (validDeficits.isEmpty() || remainingCapacities.isEmpty()) {
            return emptyList()
        }

        val recordedGrants = LinkedHashMap<Pair<String, String>, Long>()
        for ((debtorId, totalDeficit) in validDeficits) {
            distributeDebtorDeficit(debtorId, totalDeficit, remainingCapacities, recordedGrants)
        }

        return recordedGrants.map { (pair, amount) ->
            Settlement(
                fromUserId = pair.first,
                toUserId = pair.second,
                amount = amount,
                currency = groupCurrency,
                sourcePocket = SettlementPocketType.POCKET
            )
        }
    }

    private fun initialCapacities(capacities: List<Pair<String, Long>>): LinkedHashMap<String, Long> {
        val map = LinkedHashMap<String, Long>()
        for ((userId, cap) in capacities) {
            if (cap > 0L) {
                map[userId] = (map[userId] ?: 0L) + cap
            }
        }
        return map
    }

    private fun distributeDebtorDeficit(
        debtorId: String,
        totalDeficit: Long,
        remainingCapacities: LinkedHashMap<String, Long>,
        recordedGrants: LinkedHashMap<Pair<String, String>, Long>
    ) {
        var unassigned = totalDeficit
        while (unassigned > 0L) {
            val allocated = allocateFairRound(debtorId, unassigned, remainingCapacities, recordedGrants)
            if (allocated <= 0L) {
                break
            }
            unassigned -= allocated
        }
    }

    private fun allocateFairRound(
        debtorId: String,
        unassigned: Long,
        remainingCapacities: LinkedHashMap<String, Long>,
        recordedGrants: LinkedHashMap<Pair<String, String>, Long>
    ): Long {
        val activeCreditors = remainingCapacities.filter { it.value > 0L }.keys.toList()
        if (activeCreditors.isEmpty()) return 0L

        val totalActiveCapacity = activeCreditors.sumOf { remainingCapacities[it] ?: 0L }
        if (totalActiveCapacity <= 0L) return 0L

        val toDistribute = minOf(unassigned, totalActiveCapacity)
        val sharePerCreditor = toDistribute / activeCreditors.size
        var remainder = (toDistribute % activeCreditors.size).toInt()

        var totalAllocated = 0L
        for (creditorId in activeCreditors) {
            val cap = remainingCapacities[creditorId] ?: 0L
            val bonus = if (remainder > 0) 1L else 0L
            if (remainder > 0) remainder--

            val actualGrant = minOf(sharePerCreditor + bonus, cap)
            if (actualGrant > 0L) {
                remainingCapacities[creditorId] = cap - actualGrant
                totalAllocated += actualGrant
                val key = debtorId to creditorId
                recordedGrants[key] = (recordedGrants[key] ?: 0L) + actualGrant
            }
        }
        return totalAllocated
    }
}
