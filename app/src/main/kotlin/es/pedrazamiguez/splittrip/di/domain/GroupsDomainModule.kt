package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.service.GroupMembershipService
import es.pedrazamiguez.splittrip.domain.service.impl.EmailValidationServiceImpl
import es.pedrazamiguez.splittrip.domain.service.impl.GroupMembershipServiceImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.CreateGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.DeleteGroupUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetGroupByIdUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.GetUserGroupsFlowUseCase
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.CreateGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.DeleteGroupUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetGroupByIdUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.group.impl.GetUserGroupsFlowUseCaseImpl
import org.koin.dsl.module

val groupsDomainModule = module {
    factory<GroupMembershipService> {
        GroupMembershipServiceImpl(
            groupRepository = get<GroupRepository>(),
            authenticationService = get<AuthenticationService>()
        )
    }
    factory<CreateGroupUseCase> { CreateGroupUseCaseImpl(groupRepository = get<GroupRepository>()) }
    factory<EmailValidationService> { EmailValidationServiceImpl() }
    factory<DeleteGroupUseCase> { DeleteGroupUseCaseImpl(groupRepository = get<GroupRepository>()) }
    factory<GetGroupByIdUseCase> { GetGroupByIdUseCaseImpl(groupRepository = get<GroupRepository>()) }
    factory<GetUserGroupsFlowUseCase> { GetUserGroupsFlowUseCaseImpl(groupRepository = get<GroupRepository>()) }
}
