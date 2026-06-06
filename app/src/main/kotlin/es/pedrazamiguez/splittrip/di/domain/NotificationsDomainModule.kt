package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import org.koin.dsl.module

val notificationsDomainModule = module {
    factory<RegisterDeviceTokenUseCase> {
        RegisterDeviceTokenUseCase(
            deviceRepository = get<DeviceRepository>(),
            notificationRepository = get<NotificationRepository>()
        )
    }

    factory<UnregisterDeviceTokenUseCase> {
        UnregisterDeviceTokenUseCase(
            notificationRepository = get<NotificationRepository>()
        )
    }

    factory<GetNotificationPreferencesUseCase> {
        GetNotificationPreferencesUseCase(
            repository = get<NotificationPreferencesRepository>()
        )
    }

    factory<UpdateNotificationPreferenceUseCase> {
        UpdateNotificationPreferenceUseCase(
            repository = get<NotificationPreferencesRepository>()
        )
    }
}
