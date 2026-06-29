package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CashWithdrawalValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.impl.CashWithdrawalValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddCashWithdrawalUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AddCashWithdrawalUseCaseImpl
import org.koin.dsl.module

val withdrawalsDomainModule = module {
    factory<CashWithdrawalValidationService> { CashWithdrawalValidationServiceImpl() }

    factory<AddCashWithdrawalUseCase> {
        AddCashWithdrawalUseCaseImpl(
            cashWithdrawalRepository = get<CashWithdrawalRepository>(),
            validationService = get<CashWithdrawalValidationService>(),
            groupMembershipService = get<GroupMembershipService>(),
            subunitRepository = get<SubunitRepository>(),
            authenticationService = get<AuthenticationService>(),
            groupRepository = get<GroupRepository>()
        )
    }
}
