package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.ContributionValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.impl.ContributionValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.balance.AddContributionUseCase
import es.pedrazamiguez.splittrip.domain.usecase.balance.impl.AddContributionUseCaseImpl
import org.koin.dsl.module

val contributionsDomainModule = module {
    factory<ContributionValidationService> { ContributionValidationServiceImpl() }

    factory<AddContributionUseCase> {
        AddContributionUseCaseImpl(
            contributionRepository = get<ContributionRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            contributionValidationService = get<ContributionValidationService>(),
            subunitRepository = get<SubunitRepository>(),
            authenticationService = get<AuthenticationService>()
        )
    }
}
