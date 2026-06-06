package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.usecase.user.GetCurrentUserProfileUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.GetMemberProfilesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.SearchUsersByEmailUseCase
import org.koin.dsl.module

val profileDomainModule = module {
    factory {
        GetMemberProfilesUseCase(
            userRepository = get<UserRepository>()
        )
    }
    factory {
        GetCurrentUserProfileUseCase(
            userRepository = get<UserRepository>()
        )
    }
    factory {
        SearchUsersByEmailUseCase(
            userRepository = get<UserRepository>()
        )
    }
}
