package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.domain.model.Settlement

interface PocketDebtDistributionService {
    /**
     * Distributes virtual pocket deficits (where [MemberBalance.pocketBalance] < 0)
     * fairly and proportionally across members with available pocket capacity
     * (where [MemberBalance.pocketBalance] > 0).
     */
    fun distributePocketDebts(
        memberBalances: List<MemberBalance>,
        groupCurrency: String
    ): List<Settlement>

    /**
     * Core distribution engine operating on raw deficits and capacities.
     * Reusable for pocket balances and cash-only chart projections.
     */
    fun distribute(
        deficits: List<Pair<String, Long>>,
        capacities: List<Pair<String, Long>>,
        groupCurrency: String
    ): List<Settlement>
}
