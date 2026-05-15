package es.pedrazamiguez.splittrip.features.balance.presentation.model

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Subunit

/**
 * Bundles the supplementary cash-related parameters needed by [es.pedrazamiguez.splittrip.features.balance.presentation.mapper.BalancesUiMapper.mapMemberBalances]
 * to build per-member cash breakdowns.
 *
 * Grouping these lower-frequency parameters reduces the public method's parameter count from 8 to 6,
 * keeping it within Sonar's `LongParameterList` threshold while preserving the full context needed
 * for FIFO breakdown attribution.
 *
 * @param withdrawals     Full withdrawal list for the group; used to build [MemberBalanceUiModel.cashBreakdown].
 * @param subunitsMap     Subunit lookup map required for SUBUNIT-scoped withdrawal attribution.
 * @param groupMemberIds  Full member ID list required to compute equal GROUP-scope shares.
 */
data class MemberBalanceCashContext(
    val withdrawals: List<CashWithdrawal> = emptyList(),
    val subunitsMap: Map<String, Subunit> = emptyMap(),
    val groupMemberIds: List<String> = emptyList()
)
