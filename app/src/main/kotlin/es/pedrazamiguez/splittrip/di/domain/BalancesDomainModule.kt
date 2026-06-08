package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteCashWithdrawalUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteContributionUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetCashWithdrawalsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetContributionByExpenseIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupContributionsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupPocketBalanceFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetMemberBalancesFlowUseCaseImpl
import org.koin.dsl.module

val balancesDomainModule = module {

    factory<GetGroupContributionsFlowUseCase> {
        GetGroupContributionsFlowUseCaseImpl(
            contributionRepository = get<ContributionRepository>()
        )
    }

    factory<GetGroupPocketBalanceFlowUseCase> {
        GetGroupPocketBalanceFlowUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            expenseRepository = get<ExpenseRepository>(),
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            addOnCalculationService = get<AddOnCalculationService>()
        )
    }

    factory<GetCashWithdrawalsFlowUseCase> {
        GetCashWithdrawalsFlowUseCaseImpl(
            cashWithdrawalRepository = get<CashWithdrawalRepository>()
        )
    }

    factory<GetMemberBalancesFlowUseCase> {
        GetMemberBalancesFlowUseCaseImpl(
            addOnCalculationService = get<AddOnCalculationService>()
        )
    }

    factory<DeleteContributionUseCase> {
        DeleteContributionUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            groupMembershipService = get<GroupMembershipService>()
        )
    }

    factory<DeleteCashWithdrawalUseCase> {
        DeleteCashWithdrawalUseCaseImpl(
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            groupMembershipService = get<GroupMembershipService>()
        )
    }

    factory<GetContributionByExpenseIdUseCase> {
        GetContributionByExpenseIdUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            groupMembershipService = get<GroupMembershipService>()
        )
    }
}
