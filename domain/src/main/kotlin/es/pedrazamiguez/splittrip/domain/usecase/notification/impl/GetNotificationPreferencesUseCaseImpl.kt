package es.pedrazamiguez.splittrip.domain.usecase.notification.impl

import es.pedrazamiguez.splittrip.domain.model.NotificationPreferences
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import kotlinx.coroutines.flow.Flow

class GetNotificationPreferencesUseCaseImpl(
    private val repository: NotificationPreferencesRepository
) : GetNotificationPreferencesUseCase {

    override operator fun invoke(): Flow<NotificationPreferences> = repository.getPreferencesFlow()
}
