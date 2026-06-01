package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import java.time.LocalDateTime

class SignInWithEmailUseCase(
    private val authenticationService: AuthenticationService,
    private val userRepository: UserRepository,
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase
) {

    suspend operator fun invoke(email: String, password: String): Result<String> = runCatching {
        val userId = authenticationService
            .signIn(email, password)
            .getOrThrow()

        val profile = userRepository.getCurrentUserProfile()
        if (profile == null) {
            val defaultUser = User(
                userId = userId,
                email = email,
                displayName = email.substringBefore('@'),
                profileImagePath = null,
                createdAt = LocalDateTime.now()
            )
            userRepository.saveUser(defaultUser).getOrThrow()
        }

        registerDeviceTokenUseCase()
            .onFailure {
                // Device token registration is best-effort and should not
                // cause the email sign-in flow to fail.
            }

        userId
    }
}
