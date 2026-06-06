package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import org.koin.dsl.module

val groupsDomainModule = module {
    factory {
        GroupMembershipService(
            groupRepository = get<GroupRepository>(),
            authenticationService = get<AuthenticationService>()
        )
    }
    factory<CreateGroupUseCase> { CreateGroupUseCase(groupRepository = get<GroupRepository>()) }
    factory { EmailValidationService() }
    factory<DeleteGroupUseCase> { DeleteGroupUseCase(groupRepository = get<GroupRepository>()) }
    factory<GetGroupByIdUseCase> { GetGroupByIdUseCase(groupRepository = get<GroupRepository>()) }
    factory<GetUserGroupsFlowUseCase> { GetUserGroupsFlowUseCase(groupRepository = get<GroupRepository>()) }
}
