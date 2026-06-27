package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel.state

import es.pedrazamiguez.splittrip.features.balance.presentation.model.ActivityItemUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.CashWithdrawalUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ContributionUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.ExtrasBreakdownUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.GroupPocketBalanceUiModel
import es.pedrazamiguez.splittrip.features.balance.presentation.model.MemberBalanceUiModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class BalancesUiState(
    val isLoading: Boolean = true,
    val groupId: String? = null,
    val pocketBalance: GroupPocketBalanceUiModel = GroupPocketBalanceUiModel(),
    val contributions: ImmutableList<ContributionUiModel> = persistentListOf(),
    val cashWithdrawals: ImmutableList<CashWithdrawalUiModel> = persistentListOf(),
    val memberBalances: ImmutableList<MemberBalanceUiModel> = persistentListOf(),
    val activityItems: ImmutableList<ActivityItemUiModel> = persistentListOf(),
    val extrasBreakdown: ImmutableList<ExtrasBreakdownUiModel> = persistentListOf(),
    val shouldAnimateBalance: Boolean = false,
    val previousBalance: String = "",
    val balanceRollingUp: Boolean = true,
    /** Item pending deletion — drives `ActionBottomSheet` and `DestructiveConfirmationDialog` visibility. */
    val contributionToDelete: ContributionUiModel? = null,
    /** Item pending deletion — drives `ActionBottomSheet` and `DestructiveConfirmationDialog` visibility. */
    val withdrawalToDelete: CashWithdrawalUiModel? = null
)
