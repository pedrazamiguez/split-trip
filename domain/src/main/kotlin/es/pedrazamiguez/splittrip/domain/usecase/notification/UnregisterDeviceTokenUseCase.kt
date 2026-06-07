package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UnregisterDeviceTokenUseCase : UseCase {
    suspend operator fun invoke(): Result<Unit>
}
