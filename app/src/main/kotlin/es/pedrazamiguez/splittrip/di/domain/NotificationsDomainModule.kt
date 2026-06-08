package es.pedrazamiguez.splittrip.di.domain

import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationPreferencesRepository
import es.pedrazamiguez.splittrip.domain.repository.NotificationRepository
import es.pedrazamiguez.splittrip.domain.usecase.notification.GetNotificationPreferencesUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.RegisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UnregisterDeviceTokenUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.UpdateNotificationPreferenceUseCase
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.GetNotificationPreferencesUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.RegisterDeviceTokenUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.UnregisterDeviceTokenUseCaseImpl
import es.pedrazamiguez.splittrip.domain.usecase.notification.impl.UpdateNotificationPreferenceUseCaseImpl
import org.koin.dsl.module

val notificationsDomainModule = module {
    factory<RegisterDeviceTokenUseCase> {
        RegisterDeviceTokenUseCaseImpl(
            deviceRepository = get<DeviceRepository>(),
            notificationRepository = get<NotificationRepository>()
        )
    }

    factory<UnregisterDeviceTokenUseCase> {
        UnregisterDeviceTokenUseCaseImpl(
            notificationRepository = get<NotificationRepository>()
        )
    }

    factory<GetNotificationPreferencesUseCase> {
        GetNotificationPreferencesUseCaseImpl(
            repository = get<NotificationPreferencesRepository>()
        )
    }

    factory<UpdateNotificationPreferenceUseCase> {
        UpdateNotificationPreferenceUseCaseImpl(
            repository = get<NotificationPreferencesRepository>()
        )
    }
}
