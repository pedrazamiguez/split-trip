package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase

class LinkGoogleAccountUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
) : LinkGoogleAccountUseCase {

    override suspend operator fun invoke(idToken: String): Result<Unit> = runCatching {
        authenticationService.linkGoogleAccount(idToken).getOrThrow()
        val email = authenticationService.currentUserEmail() ?: error("Linked Google email is null")
        val userId = authenticationService.requireUserId()

        val existingProfile = userRepository.getCurrentUserProfile()
        if (existingProfile != null) {
            val updatedProfile = existingProfile.copy(email = email)
            userRepository.saveUser(updatedProfile).getOrThrow()
        }

        reconcileUnregisteredUserUseCase(email, userId).getOrThrow()
    }
}
