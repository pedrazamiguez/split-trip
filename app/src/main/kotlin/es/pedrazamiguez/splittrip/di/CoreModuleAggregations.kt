package es.pedrazamiguez.splittrip.di

import es.pedrazamiguez.splittrip.core.common.di.coreCommonModule
import es.pedrazamiguez.splittrip.core.designsystem.di.coreDesignSystemModule
import es.pedrazamiguez.splittrip.logging.di.coreLoggingModule
import org.koin.dsl.module

val coreModules = module {
    includes(
        coreCommonModule,
        coreDesignSystemModule,
        coreLoggingModule
    )
}
