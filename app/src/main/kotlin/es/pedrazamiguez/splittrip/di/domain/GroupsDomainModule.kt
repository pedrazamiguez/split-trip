package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.core.logging.LogTag
import es.pedrazamiguez.splittrip.core.logging.createLoggingProxy
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.ExchangeRateCalculationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.GroupMembershipServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AreGroupSettlementsResolvedUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.AreMemberSettlementsResolvedUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetMemberBalancesFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.GetSettlementSuggestionsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.ResolveCashOnLeaveUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.ResolveCashOnLeaveUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.currency.GetExchangeRateUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.AddGroupMembersUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ArchiveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdLocalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.LeaveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.ObserveSelectedGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.RemoveGroupMemberUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.UpdateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.AddGroupMembersUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ArchiveGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.CreateGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.DeleteGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetGroupByIdLocalUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetGroupByIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetUserGroupsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.LeaveGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ObserveGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.ObserveSelectedGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.RemoveGroupMemberUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.UpdateGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.ReassignSubunitSharesUseCase
import org.koin.dsl.module

val groupsDomainModule = module {
    factory<GroupMembershipService> {
        createLoggingProxy<GroupMembershipService>(
            GroupMembershipServiceImpl(
                groupRepository = get<GroupRepository>(),
                authenticationService = get<AuthenticationService>()
            ),
            LogTag.SERVICE
        )
    }
    factory<CreateGroupUseCase> {
        createLoggingProxy<CreateGroupUseCase>(
            CreateGroupUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                userRepository = get<UserRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<EmailValidationService> {
        createLoggingProxy<EmailValidationService>(
            EmailValidationServiceImpl(),
            LogTag.SERVICE
        )
    }
    factory<DeleteGroupUseCase> {
        createLoggingProxy<DeleteGroupUseCase>(
            DeleteGroupUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                areGroupSettlementsResolvedUseCase = get<AreGroupSettlementsResolvedUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<GetGroupByIdUseCase> {
        createLoggingProxy<GetGroupByIdUseCase>(
            GetGroupByIdUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<GetGroupByIdLocalUseCase> {
        createLoggingProxy<GetGroupByIdLocalUseCase>(
            GetGroupByIdLocalUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<GetUserGroupsFlowUseCase> {
        createLoggingProxy<GetUserGroupsFlowUseCase>(
            GetUserGroupsFlowUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<UpdateGroupUseCase> {
        createLoggingProxy<UpdateGroupUseCase>(
            UpdateGroupUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<ArchiveGroupUseCase> {
        createLoggingProxy<ArchiveGroupUseCase>(
            ArchiveGroupUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                authenticationService = get<AuthenticationService>(),
                getSettlementSuggestionsUseCase = get<GetSettlementSuggestionsUseCase>(),
                areGroupSettlementsResolvedUseCase = get<AreGroupSettlementsResolvedUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<ObserveSelectedGroupUseCase> {
        createLoggingProxy<ObserveSelectedGroupUseCase>(
            ObserveSelectedGroupUseCaseImpl(
                groupPreferenceRepository = get<GroupPreferenceRepository>(),
                groupRepository = get<GroupRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<ObserveGroupUseCase> {
        createLoggingProxy<ObserveGroupUseCase>(
            ObserveGroupUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<AddGroupMembersUseCase> {
        createLoggingProxy<AddGroupMembersUseCase>(
            AddGroupMembersUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                userRepository = get<UserRepository>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<RemoveGroupMemberUseCase> {
        createLoggingProxy<RemoveGroupMemberUseCase>(
            RemoveGroupMemberUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                expenseRepository = get<ExpenseRepository>(),
                contributionRepository = get<ContributionRepository>(),
                cashWithdrawalRepository = get<CashWithdrawalRepository>(),
                subunitRepository = get<SubunitRepository>(),
                settlementRepository = get<SettlementRepository>(),
                cashTransferRepository = get<CashTransferRepository>(),
                getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<ResolveCashOnLeaveUseCase> {
        createLoggingProxy<ResolveCashOnLeaveUseCase>(
            ResolveCashOnLeaveUseCaseImpl(
                cashWithdrawalRepository = get<CashWithdrawalRepository>(),
                exchangeRateCalculationService = get<ExchangeRateCalculationService>(),
                getExchangeRateUseCase = get<GetExchangeRateUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
    factory<LeaveGroupUseCase> {
        createLoggingProxy<LeaveGroupUseCase>(
            LeaveGroupUseCaseImpl(
                groupRepository = get<GroupRepository>(),
                authenticationService = get<AuthenticationService>(),
                getSettlementSuggestionsUseCase = get<GetSettlementSuggestionsUseCase>(),
                areMemberSettlementsResolvedUseCase = get<AreMemberSettlementsResolvedUseCase>(),
                reassignSubunitSharesUseCase = get<ReassignSubunitSharesUseCase>(),
                expenseRepository = get<ExpenseRepository>(),
                contributionRepository = get<ContributionRepository>(),
                cashWithdrawalRepository = get<CashWithdrawalRepository>(),
                subunitRepository = get<SubunitRepository>(),
                settlementRepository = get<SettlementRepository>(),
                cashTransferRepository = get<CashTransferRepository>(),
                getMemberBalancesFlowUseCase = get<GetMemberBalancesFlowUseCase>(),
                resolveCashOnLeaveUseCase = get<ResolveCashOnLeaveUseCase>()
            ),
            LogTag.USE_CASE
        )
    }
}
