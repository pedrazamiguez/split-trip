package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetCurrentUserProfileUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.GetMemberProfilesUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.user.impl.SearchUsersByEmailUseCaseImpl
import org.koin.dsl.module

val profileDomainModule = module {
    factory<GetMemberProfilesUseCase> {
        GetMemberProfilesUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<GetCurrentUserProfileUseCase> {
        GetCurrentUserProfileUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
    factory<SearchUsersByEmailUseCase> {
        SearchUsersByEmailUseCaseImpl(
            userRepository = get<UserRepository>()
        )
    }
}
