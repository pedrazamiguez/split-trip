package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.LinkEmailPasswordUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class LinkEmailPasswordUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
) : LinkEmailPasswordUseCase {

    override suspend operator fun invoke(email: String, password: String): Result<Unit> = runCatching {
        val normalizedEmail = User.normalizeEmail(email)
        authenticationService.linkEmailPassword(normalizedEmail, password).getOrThrow()
        val userId = authenticationService.requireUserId()

        val existingProfile = userRepository.getCurrentUserProfile()
        val profileToSave = existingProfile?.copy(email = normalizedEmail) ?: run {
            val creationTimestamp = authenticationService.getCurrentUserCreationTimestamp()
            val createdAt = if (creationTimestamp != null) {
                LocalDateTime.ofInstant(Instant.ofEpochMilli(creationTimestamp), ZoneOffset.UTC)
            } else {
                LocalDateTime.now(ZoneOffset.UTC)
            }
            User(
                userId = userId,
                email = normalizedEmail,
                displayName = normalizedEmail.substringBefore("@"),
                profileImagePath = null,
                createdAt = createdAt
            )
        }
        userRepository.saveUser(profileToSave).getOrThrow()

        reconcileUnregisteredUserUseCase(normalizedEmail, userId)
    }
}
