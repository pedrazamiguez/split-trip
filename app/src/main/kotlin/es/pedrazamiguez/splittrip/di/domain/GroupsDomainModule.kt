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
import es.pedrazamiguez.splittrip.logging.LogTag
import es.pedrazamiguez.splittrip.logging.createLoggingProxy
import org.koin.dsl.module

val groupsDomainModule = module {
    factory<GroupMembershipService> {
        createLoggingProxy(
            GroupMembershipServiceImpl(
                groupRepository = get<GroupRepository>(),
                authenticationService = get<AuthenticationService>()
            ),
            LogTag.SERVICE
        )
    }
    factory<CreateGroupUseCase> {
        createLoggingProxy(
            CreateGroupUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<EmailValidationService> {
        createLoggingProxy(
            EmailValidationServiceImpl(),
            LogTag.SERVICE
        )
    }
    factory<DeleteGroupUseCase> {
        createLoggingProxy(
            DeleteGroupUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<GetGroupByIdUseCase> {
        createLoggingProxy(
            GetGroupByIdUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
    factory<GetUserGroupsFlowUseCase> {
        createLoggingProxy(
            GetUserGroupsFlowUseCaseImpl(groupRepository = get<GroupRepository>()),
            LogTag.USE_CASE
        )
    }
}
