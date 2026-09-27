package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.data.BuildConfig
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.datastore.SettlementNudgePreferences
import es.pedrazamiguez.splittrip.data.local.service.LocalDatabaseCleanerServiceImpl
import es.pedrazamiguez.splittrip.data.repository.impl.CurrencyRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.SettlementNudgeRepositoryImpl
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.remote.RemoteCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.repository.CurrencyRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementNudgeRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.LocalDatabaseCleanerService
import es.pedrazamiguez.splittrip.domain.service.SyncTeardownService
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val dataCommonModule = module {

    single<CurrencyRepository> {
        CurrencyRepositoryImpl(
            localDataSource = get<LocalCurrencyDataSource>(),
            remoteDataSource = get<RemoteCurrencyDataSource>(),
            cacheDuration = Duration.ofHours(BuildConfig.EXCHANGE_RATES_CACHE_DURATION_HOURS)
        )
    }

    single<LocalDatabaseCleanerService> {
        LocalDatabaseCleanerServiceImpl(
            appDatabase = get<AppDatabase>(),
            ioDispatcher = Dispatchers.IO
        )
    }

    single<SettlementNudgeRepository> {
        SettlementNudgeRepositoryImpl(
            settlementNudgePreferences = get<SettlementNudgePreferences>(),
            cloudSettlementDataSource = get<CloudSettlementDataSource>(),
            localSettlementDataSource = get<LocalSettlementDataSource>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single {
        SyncTeardownCoordinator()
    }

    single<SyncTeardownService> {
        get<SyncTeardownCoordinator>()
    }
}
