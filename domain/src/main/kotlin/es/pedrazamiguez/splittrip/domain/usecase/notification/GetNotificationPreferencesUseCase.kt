package es.pedrazamiguez.splittrip.domain.usecase.notification

import es.pedrazamiguez.splittrip.domain.model.NotificationPreferences
import es.pedrazamiguez.splittrip.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow

interface GetNotificationPreferencesUseCase : UseCase {
    operator fun invoke(): Flow<NotificationPreferences>
}
