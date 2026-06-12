package es.pedrazamiguez.splittrip.domain.usecase.user.impl

import es.pedrazamiguez.splittrip.domain.model.ValidationResult
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.UserValidationService
import es.pedrazamiguez.splittrip.domain.usecase.user.UpdateUserProfileUseCase

class UpdateUserProfileUseCaseImpl(
    private val userRepository: UserRepository,
    private val userValidationService: UserValidationService
) : UpdateUserProfileUseCase {

    override suspend operator fun invoke(
        userId: String,
        displayName: String?,
        bio: String?,
        localAvatarUri: String?
    ): Result<Unit> {
        if (displayName != null) {
            val nameValidation = userValidationService.validateDisplayName(displayName)
            if (nameValidation is ValidationResult.Invalid) {
                return Result.failure(IllegalArgumentException(nameValidation.message))
            }
        }
        val bioValidation = userValidationService.validateBio(bio)
        if (bioValidation is ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(bioValidation.message))
        }

        return userRepository.updateUserProfile(userId, displayName, bio, localAvatarUri)
    }
}
