package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface GetCurrentUserProfileUseCase : UseCase {
    suspend operator fun invoke(): User?
}
