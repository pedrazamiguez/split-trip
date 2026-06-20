package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserPreferenceRepository
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SignInWithEmailUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.user.ReconcileUnregisteredUserUseCase
import java.time.LocalDateTime
import java.time.ZoneOffset

class SignInWithEmailUseCaseImpl(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val reconcileUnregisteredUserUseCase: ReconcileUnregisteredUserUseCase
) : SignInWithEmailUseCase {

    override suspend operator fun invoke(email: String, password: String): Result<String> = runCatching {
        val normalizedEmail = User.normalizeEmail(email)
        val userId = authenticationService
            .signIn(normalizedEmail, password)
            .getOrThrow()

        val profile = userRepository.getCurrentUserProfile()
        if (profile == null) {
            val defaultUser = User(
                userId = userId,
                email = normalizedEmail,
                displayName = normalizedEmail.substringBefore('@'),
                profileImagePath = null,
                createdAt = LocalDateTime.now(ZoneOffset.UTC)
            )
            userRepository.saveUser(defaultUser).getOrThrow()
        }

        registerDeviceTokenUseCase()
            .onFailure {
                // Device token registration is best-effort and should not
                // cause the email sign-in flow to fail.
            }

        userPreferenceRepository.setHasSignedOut(false)
        reconcileUnregisteredUserUseCase(normalizedEmail, userId).getOrThrow()

        userId
    }
}
