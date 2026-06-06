package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.data.di.notificationsDataModule
import es.pedrazamiguez.splittrip.di.domain.notificationsDomainModule
import org.koin.dsl.module

val notificationModules = module {
    includes(
        notificationsDomainModule,
        notificationsDataModule
    )
}
