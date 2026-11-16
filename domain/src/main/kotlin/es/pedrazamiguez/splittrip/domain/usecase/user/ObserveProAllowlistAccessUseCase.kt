package es.pedrazamiguez.splittrip.domain.usecase.user

import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface ObserveProAllowlistAccessUseCase : UseCase {
    operator fun invoke(): Flow<Boolean>
}
