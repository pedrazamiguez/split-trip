package es.pedrazamiguez.splittrip.domain.usecase.auth.impl

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.EmailValidationService
import es.pedrazamiguez.splittrip.domain.usecase.auth.SendPasswordResetEmailUseCase

class SendPasswordResetEmailUseCaseImpl(
    private val authService: AuthenticationService,
    private val emailValidationService: EmailValidationService
) : SendPasswordResetEmailUseCase {
    override suspend operator fun invoke(email: String): Result<Unit> {
        val trimmedEmail = email.trim()
        if (!emailValidationService.isValidEmail(trimmedEmail)) {
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }
        val normalizedEmail = User.normalizeEmail(trimmedEmail)
        return authService.sendPasswordResetEmail(normalizedEmail)
    }
}
