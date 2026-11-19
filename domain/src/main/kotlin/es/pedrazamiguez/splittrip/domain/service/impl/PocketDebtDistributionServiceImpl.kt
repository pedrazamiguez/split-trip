package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.MemberBalanceEntry
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.model.SettlementPocketType
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService

class PocketDebtDistributionServiceImpl : PocketDebtDistributionService {

    private data class DebtGrantKey(
        val debtorId: String,
        val creditorId: String
    )

    override fun distributePocketDebts(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement> {
        val deficits = memberBalances
            .filter { it.pocketBalance < 0L }
            .map { MemberBalanceEntry(memberId = it.userId, amountCents = -it.pocketBalance) }

        val capacities = memberBalances
            .filter { it.pocketBalance > 0L }
            .map { MemberBalanceEntry(memberId = it.userId, amountCents = it.pocketBalance) }

        return distribute(deficits, capacities, groupCurrency)
    }

    override fun distribute(
        deficits: List<MemberBalanceEntry>,
        capacities: List<MemberBalanceEntry>,
        groupCurrency: String
    ): List<Settlement> {
        val validDeficits = deficits
            .filter { it.amountCents > 0L }
            .sortedByDescending { it.amountCents }

        val remainingCapacities = initialCapacities(capacities)
        if (validDeficits.isEmpty() || remainingCapacities.isEmpty()) {
            return emptyList()
        }

        val recordedGrants = LinkedHashMap<DebtGrantKey, Long>()
        for ((debtorId, totalDeficit) in validDeficits) {
            distributeDebtorDeficit(debtorId, totalDeficit, remainingCapacities, recordedGrants)
        }

        return recordedGrants.map { (grantKey, amount) ->
            Settlement(
                fromUserId = grantKey.debtorId,
                toUserId = grantKey.creditorId,
                amount = amount,
                currency = groupCurrency,
                sourcePocket = SettlementPocketType.POCKET
            )
        }
    }

    private fun initialCapacities(capacities: List<MemberBalanceEntry>): LinkedHashMap<String, Long> {
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
        recordedGrants: LinkedHashMap<DebtGrantKey, Long>
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
        recordedGrants: LinkedHashMap<DebtGrantKey, Long>
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
                val key = DebtGrantKey(debtorId = debtorId, creditorId = creditorId)
                recordedGrants[key] = (recordedGrants[key] ?: 0L) + actualGrant
            }
        }
        return totalAllocated
    }
}
