package es.pedrazamiguez.splittrip.features.balance.presentation.preview

import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.MemberBalance
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashBreakdownUiModel
import kotlinx.collections.immutable.ImmutableList

internal object BalancesPreviewInputs {
    data class ActivityInput(
        val contributions: List<Contribution>,
        val withdrawals: List<CashWithdrawal>
    )

    data class CashBreakdownInput(
        val memberBalance: MemberBalance,
        val withdrawals: List<CashWithdrawal>,
        val groupCurrency: String
    )

    data class CashBreakdownOutput(
        val breakdown: ImmutableList<CashBreakdownUiModel>,
        val formattedTotal: String
    )
}
