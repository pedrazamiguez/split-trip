package es.pedrazamiguez.splittrip.domain.service.impl

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import kotlin.math.min

class DebtSimplificationServiceImpl : DebtSimplificationService {
    override fun simplify(memberBalances: List<MemberBalance>): List<Settlement> {
        val debtors = mutableListOf<Pair<String, Long>>()
        val creditors = mutableListOf<Pair<String, Long>>()

        for (mb in memberBalances) {
            val bal = mb.totalBalance
            if (bal < 0) {
                debtors.add(mb.userId to -bal)
            } else if (bal > 0) {
                creditors.add(mb.userId to bal)
            }
        }

        // Sort by absolute amount descending to pair largest debtors with largest creditors
        debtors.sortByDescending { it.second }
        creditors.sortByDescending { it.second }

        val settlements = mutableListOf<Settlement>()
        var dIdx = 0
        var cIdx = 0

        val activeDebtors = debtors.map { it.first to it.second }.toMutableList()
        val activeCreditors = creditors.map { it.first to it.second }.toMutableList()

        while (dIdx < activeDebtors.size && cIdx < activeCreditors.size) {
            val debtor = activeDebtors[dIdx]
            val creditor = activeCreditors[cIdx]

            val settleAmount = min(debtor.second, creditor.second)
            if (settleAmount > 0) {
                settlements.add(
                    Settlement(
                        fromUserId = debtor.first,
                        toUserId = creditor.first,
                        amount = settleAmount
                    )
                )
            }

            activeDebtors[dIdx] = debtor.first to (debtor.second - settleAmount)
            activeCreditors[cIdx] = creditor.first to (creditor.second - settleAmount)

            if (activeDebtors[dIdx].second == 0L) dIdx++
            if (activeCreditors[cIdx].second == 0L) cIdx++
        }

        return settlements
    }
}
