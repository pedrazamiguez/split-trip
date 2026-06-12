package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.model.User
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface ObserveCurrentUserProfileUseCase : UseCase {
    operator fun invoke(): Flow<User?>
}
