package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface SearchUsersByEmailUseCase : UseCase {
    suspend operator fun invoke(email: String): Result<List<User>>
}
