package es.pedrazamiguez.splittrip.domain.usecase.auth

import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface IsUserAnonymousUseCase : UseCase {
    operator fun invoke(): Flow<Boolean>
}
