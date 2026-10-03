package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.data.datasource.GroupDashboardDataSourceImpl
import es.pedrazamiguez.splittrip.domain.datasource.GroupDashboardDataSource
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementNudgeRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AddOnCalculationService
import es.pedrazamiguez.splittrip.domain.service.AppConfigService
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CashDebtScalingService
import es.pedrazamiguez.splittrip.domain.service.DebtSimplificationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.PocketDebtDistributionService
import es.pedrazamiguez.splittrip.domain.service.RemainderDistributionService
import es.pedrazamiguez.splittrip.domain.service.SettlementReconciliationService
import es.pedrazamiguez.splittrip.domain.service.impl.CashDebtScalingServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.DebtSimplificationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.PocketDebtDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.SettlementReconciliationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AreGroupSettlementsResolvedUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.AreMemberSettlementsResolvedUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.ConfirmSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DeleteContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.DisputeSettlementUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetBalancesDashboardFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetCashWithdrawalsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetContributionByExpenseIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupContributionsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupPocketBalanceFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetGroupSettlementsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AreGroupSettlementsResolvedUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AreMemberSettlementsResolvedUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.ConfirmSettlementUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteCashWithdrawalUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DeleteContributionUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.DisputeSettlementUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetBalancesDashboardFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetCashWithdrawalsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetContributionByExpenseIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupContributionsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupPocketBalanceFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetGroupSettlementsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetMemberBalancesFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.GetSettlementSuggestionsUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.settlement.GetNudgeTimestampsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.NudgeDebtorUseCase
import es.pedrazamiguez.splittrip.domain.usecase.settlement.impl.GetNudgeTimestampsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.settlement.impl.NudgeDebtorUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
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

    factory<SettlementReconciliationService> {
        SettlementReconciliationServiceImpl()
    }

    factory<GetMemberBalancesFlowUseCase> {
        GetMemberBalancesFlowUseCaseImpl(
            addOnCalculationService = get<AddOnCalculationService>(),
            settlementReconciliationService = get<SettlementReconciliationService>()
        )
    }

    factory<DeleteContributionUseCase> {
        DeleteContributionUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            groupRepository = get<GroupRepository>()
        )
    }

    factory<DeleteCashWithdrawalUseCase> {
        DeleteCashWithdrawalUseCaseImpl(
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            groupRepository = get<GroupRepository>()
        )
    }

    factory<GetContributionByExpenseIdUseCase> {
        GetContributionByExpenseIdUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            groupMembershipService = get<GroupMembershipService>()
        )
    }

    factory<CashDebtScalingService> {
        CashDebtScalingServiceImpl(
            remainderDistributionService = get<RemainderDistributionService>()
        )
    }

    factory<PocketDebtDistributionService> {
        PocketDebtDistributionServiceImpl()
    }

    factory<DebtSimplificationService> {
        val cashDebtScalingService = get<CashDebtScalingService>()
        val pocketDebtDistributionService = get<PocketDebtDistributionService>()
        DebtSimplificationServiceImpl(
            cashDebtScalingService = cashDebtScalingService,
            pocketDebtDistributionService = pocketDebtDistributionService
        )
    }

    factory<GetSettlementSuggestionsUseCase> {
        GetSettlementSuggestionsUseCaseImpl(
            debtSimplificationService = get<DebtSimplificationService>(),
            settlementRepository = get<SettlementRepository>(),
            groupRepository = get<GroupRepository>(),
            expenseRepository = get<ExpenseRepository>(),
            contributionRepository = get<ContributionRepository>(),
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            subunitRepository = get<SubunitRepository>(),
            cashTransferRepository = get<CashTransferRepository>(),
            getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>()
        )
    }

    factory<ConfirmSettlementUseCase> {
        ConfirmSettlementUseCaseImpl(
            settlementRepository = get<SettlementRepository>(),
            authenticationService = get<AuthenticationService>(),
            groupRepository = get<GroupRepository>(),
            contributionRepository = get<ContributionRepository>(),
            groupDashboardDataSource = get<GroupDashboardDataSource>(),
            getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>()
        )
    }

    factory<DisputeSettlementUseCase> {
        DisputeSettlementUseCaseImpl(
            settlementRepository = get<SettlementRepository>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    factory<AreMemberSettlementsResolvedUseCase> {
        AreMemberSettlementsResolvedUseCaseImpl(
            settlementRepository = get<SettlementRepository>()
        )
    }

    factory<GetGroupSettlementsFlowUseCase> {
        GetGroupSettlementsFlowUseCaseImpl(
            settlementRepository = get<SettlementRepository>()
        )
    }

    factory<AreGroupSettlementsResolvedUseCase> {
        AreGroupSettlementsResolvedUseCaseImpl(
            settlementRepository = get<SettlementRepository>()
        )
    }

    factory<GroupDashboardDataSource> {
        GroupDashboardDataSourceImpl(
            groupRepository = get<GroupRepository>(),
            contributionRepository = get<ContributionRepository>(),
            withdrawalRepository = get<CashWithdrawalRepository>(),
            expenseRepository = get<ExpenseRepository>(),
            settlementRepository = get<SettlementRepository>(),
            subunitRepository = get<SubunitRepository>(),
            cashTransferRepository = get<CashTransferRepository>()
        )
    }

    factory<GetBalancesDashboardFlowUseCase> {
        GetBalancesDashboardFlowUseCaseImpl(
            groupDashboardDataSource = get<GroupDashboardDataSource>(),
            getGroupPocketBalanceFlowUseCase = get<GetGroupPocketBalanceFlowUseCase>(),
            getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>(),
            getSettlementSuggestionsUseCase = get<GetSettlementSuggestionsUseCase>(),
            getMemberProfilesUseCase = get<GetMemberProfilesUseCase>(),
            appConfigService = get<AppConfigService>()
        )
    }

    factory<GetNudgeTimestampsFlowUseCase> {
        GetNudgeTimestampsFlowUseCaseImpl(
            settlementNudgeRepository = get<SettlementNudgeRepository>()
        )
    }

    factory<NudgeDebtorUseCase> {
        NudgeDebtorUseCaseImpl(
            settlementRepository = get<SettlementRepository>(),
            settlementNudgeRepository = get<SettlementNudgeRepository>(),
            appConfigService = get<AppConfigService>(),
            authenticationService = get<AuthenticationService>()
        )
    }
}
