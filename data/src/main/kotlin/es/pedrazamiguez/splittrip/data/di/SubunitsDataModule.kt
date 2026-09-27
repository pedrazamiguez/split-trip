package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.repository.impl.SubunitRepositoryImpl
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSubunitDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSubunitDataSource
import es.pedrazamiguez.splittrip.domain.repository.SubunitRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val subunitsDataModule = module {
    single<SubunitRepository> {
        SubunitRepositoryImpl(
            cloudSubunitDataSource = get<CloudSubunitDataSource>(),
            localSubunitDataSource = get<LocalSubunitDataSource>(),
            localGroupDataSource = get<LocalGroupDataSource>(),
            authenticationService = get<AuthenticationService>(),
            syncTeardownCoordinator = get<SyncTeardownCoordinator>(),
            performanceMonitor = get<PerformanceMonitor>(),
            ioDispatcher = Dispatchers.IO
        )
    }
}
