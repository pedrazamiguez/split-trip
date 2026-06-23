package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkGoogleAccountUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

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
        val profileToSave = existingProfile?.copy(email = email) ?: run {
            val creationTimestamp = authenticationService.getCurrentUserCreationTimestamp()
            val createdAt = if (creationTimestamp != null) {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTimestamp), ZoneOffset.UTC)
            } else {
                LocalDateTime.now(ZoneOffset.UTC)
            }
            User(
                userId = userId,
                email = email,
                displayName = authenticationService.currentUserDisplayName() ?: email.substringBefore("@"),
                profileImagePath = authenticationService.currentUserPhotoUrl(),
                createdAt = createdAt
            )
        }
        userRepository.saveUser(profileToSave).getOrThrow()

        reconcileUnregisteredUserUseCase(email, userId)
    }
}
