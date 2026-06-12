package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UpdateUserProfileUseCase : UseCase {
    suspend operator fun invoke(
        userId: String,
        displayName: String?,
        bio: String?,
        localAvatarUri: String?
    ): Result<Unit>
}
