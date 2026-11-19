package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.data.di.dataCommonModule
import es.pedrazamiguez.splittrip.data.firebase.di.dataFirebaseModule
import es.pedrazamiguez.splittrip.data.local.di.dataLocalModule
import org.koin.dsl.module

val dataModules = module {
    includes(
        dataFirebaseModule,
        dataLocalModule,
        dataCommonModule
    )
}
