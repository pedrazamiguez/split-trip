package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInAnonymouslyUseCase

class SignInAnonymouslyUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val userPreferenceRepository: UserPreferenceRepository
) : SignInAnonymouslyUseCase {

    override suspend operator fun invoke(): Result<String> {
        return authenticationService.signInAnonymously().onSuccess {
            userPreferenceRepository.setHasSignedOut(false)
        }
    }
}
