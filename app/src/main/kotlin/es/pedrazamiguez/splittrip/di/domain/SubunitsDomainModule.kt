package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.SubunitShareDistributionService
import es.pedrazamiguez.splittrip.domain.service.SubunitValidationService
import es.pedrazamiguez.splittrip.domain.usecase.subunit.CreateSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.DeleteSubunitUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.GetGroupSubunitsUseCase
import es.pedrazamiguez.splittrip.domain.usecase.subunit.UpdateSubunitUseCase
import org.koin.dsl.module

val subunitsDomainModule = module {
    factory { SubunitValidationService() }
    factory { SubunitShareDistributionService() }
    factory {
        CreateSubunitUseCase(
            subunitRepository = get<SubunitRepository>(),
            groupRepository = get<GroupRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            subunitValidationService = get<SubunitValidationService>()
        )
    }
    factory {
        GetGroupSubunitsFlowUseCase(
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory {
        GetGroupSubunitsUseCase(
            subunitRepository = get<SubunitRepository>()
        )
    }
    factory {
        UpdateSubunitUseCase(
            subunitRepository = get<SubunitRepository>(),
            groupRepository = get<GroupRepository>(),
            groupMembershipService = get<GroupMembershipService>(),
            subunitValidationService = get<SubunitValidationService>()
        )
    }
    factory {
        DeleteSubunitUseCase(
            subunitRepository = get<SubunitRepository>(),
            groupMembershipService = get<GroupMembershipService>()
        )
    }
}
