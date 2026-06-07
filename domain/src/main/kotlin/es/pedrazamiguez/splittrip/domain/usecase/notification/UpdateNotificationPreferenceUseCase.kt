package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.enums.NotificationCategory
import es.pedrazamiguez.splittrip.domain.usecase.UseCase

interface UpdateNotificationPreferenceUseCase : UseCase {
    suspend operator fun invoke(category: NotificationCategory, enabled: Boolean)
}
