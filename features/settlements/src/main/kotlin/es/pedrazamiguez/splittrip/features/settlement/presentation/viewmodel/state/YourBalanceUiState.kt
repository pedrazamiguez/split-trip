package es.pedrazamiguez.splittrip.features.settlement.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.settlement.presentation.model.MemberSpendingChartUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.PersonalPositionUiModel
import es.pedrazamiguez.splittrip.features.settlement.presentation.model.SettlementConsensusItemUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class YourBalanceUiState(
    val isLoading: Boolean = true,
    val isOffline: Boolean = false,
    val personalPosition: PersonalPositionUiModel? = null,
    val isCashBreakdownVisible: Boolean = false,
    val settlementConsensus: ImmutableList<SettlementConsensusItemUiModel> = persistentListOf(),
    val spendingChart: MemberSpendingChartUiModel? = null,
    val isChartCashOnly: Boolean = true
)
