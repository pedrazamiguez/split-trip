package es.pedrazamiguez.splittrip.features.balance.presentation.viewmodel

import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.expense.GetGroupExpensesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.GetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.setting.SetLastSeenBalanceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase

/**
 * Bundles all use cases required by [BalancesViewModel] to reduce
 * constructor parameter count below the detekt `LongParameterList` threshold.
 */
data class BalancesUseCases(
    val getGroupPocketBalanceFlowUseCase: GetGroupPocketBalanceFlowUseCase,
    val getGroupContributionsFlowUseCase: GetGroupContributionsFlowUseCase,
    val getCashWithdrawalsFlowUseCase: GetCashWithdrawalsFlowUseCase,
    val getGroupExpensesFlowUseCase: GetGroupExpensesFlowUseCase,
    val getMemberBalancesFlowUseCase: GetMemberBalancesFlowUseCase,
    val getGroupSubunitsFlowUseCase: GetGroupSubunitsFlowUseCase,
    val getGroupByIdUseCase: GetGroupByIdUseCase,
    val getLastSeenBalanceUseCase: GetLastSeenBalanceUseCase,
    val setLastSeenBalanceUseCase: SetLastSeenBalanceUseCase,
    val getMemberProfilesUseCase: GetMemberProfilesUseCase,
    val deleteContributionUseCase: DeleteContributionUseCase,
    val deleteCashWithdrawalUseCase: DeleteCashWithdrawalUseCase,
    val observeGroupUseCase: es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
)
