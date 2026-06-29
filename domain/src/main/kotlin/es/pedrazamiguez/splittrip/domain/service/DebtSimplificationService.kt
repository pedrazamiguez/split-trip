package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement

/**
 * Domain service to compute the minimum number of peer-to-peer transactions
 * required to resolve all member balances to zero.
 */
interface DebtSimplificationService {
    /**
     * Simplifies the list of member balances into a list of direct settlements.
     */
    fun simplify(memberBalances: List<MemberBalance>): List<Settlement>
}
