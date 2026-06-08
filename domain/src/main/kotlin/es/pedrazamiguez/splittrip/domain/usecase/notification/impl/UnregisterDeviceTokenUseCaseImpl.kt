package es.pedrazamiguez.splittrip.domain.usecase.notification.impl

import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase

class UnregisterDeviceTokenUseCaseImpl(
    private val notificationRepository: NotificationRepository
) : UnregisterDeviceTokenUseCase {

    override suspend operator fun invoke(): Result<Unit> = runCatching {
        notificationRepository.unregisterCurrentDevice()
    }
}
