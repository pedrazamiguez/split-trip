package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.service.SubunitValidationService
import es.pedrazamiguez.splittrip.domain.service.impl.SubunitShareDistributionServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.CreateSubunitUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.DeleteSubunitUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.GetGroupSubunitsFlowUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.GetGroupSubunitsUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.subunit.impl.UpdateSubunitUseCaseImpl
import org.koin.dsl.module

val subunitsDomainModule = module {
    factory { SubunitValidationService() }
    factory<SubunitShareDistributionService> { SubunitShareDistributionServiceImpl() }
    factory<CreateSubunitUseCase> {
        CreateSubunitUseCaseImpl(
            subunitRepository = get<SubunitRepository>(),
            groupRepository = get<GroupRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            subunitValidationService = get<SubunitValidationService>()
        )
    }
    factory<GetGroupSubunitsFlowUseCase> {
        GetGroupSubunitsFlowUseCaseImpl(
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory<GetGroupSubunitsUseCase> {
        GetGroupSubunitsUseCaseImpl(
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory<UpdateSubunitUseCase> {
        UpdateSubunitUseCaseImpl(
            subunitRepository = get<SubunitRepository>(),
            groupRepository = get<GroupRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            subunitValidationService = get<SubunitValidationService>()
        )
    }
    factory<DeleteSubunitUseCase> {
        DeleteSubunitUseCaseImpl(
            subunitRepository = get<SubunitRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            groupRepository = get<GroupRepository>()
        )
    }
}
