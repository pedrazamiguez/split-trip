package es.pedrazamiguez.splittrip.domain.usecase.notification.impl

import es.pedrazamiguez.splittrip.domain.enums.NotificationCategory
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase

class UpdateNotificationPreferenceUseCaseImpl(
    private val repository: NotificationPreferencesRepository
) : UpdateNotificationPreferenceUseCase {

    override suspend operator fun invoke(category: NotificationCategory, enabled: Boolean) {
        repository.updatePreference(category, enabled)
    }
}
