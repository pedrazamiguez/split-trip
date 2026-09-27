package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.local.dao.CashTransferDao
import es.pedrazamiguez.splittrip.data.repository.impl.CashTransferRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.CashWithdrawalRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.ContributionRepositoryImpl
import es.pedrazamiguez.splittrip.data.repository.impl.SettlementRepositoryImpl
import es.pedrazamiguez.splittrip.data.sync.SyncTeardownCoordinator
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashTransferDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalQueryDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalWriteDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSettlementDataSource
import es.pedrazamiguez.splittrip.domain.repository.CashTransferRepository
import es.pedrazamiguez.splittrip.domain.repository.CashWithdrawalRepository
import es.pedrazamiguez.splittrip.domain.repository.ContributionRepository
import es.pedrazamiguez.splittrip.domain.repository.SettlementRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val balancesDataModule = module {
    single<ContributionRepository> {
        ContributionRepositoryImpl(
            cloudContributionDataSource = get<CloudContributionDataSource>(),
            localContributionDataSource = get<LocalContributionDataSource>(),
            localGroupDataSource = get<LocalGroupDataSource>(),
            authenticationService = get<AuthenticationService>(),
            syncTeardownCoordinator = get<SyncTeardownCoordinator>(),
            performanceMonitor = get<PerformanceMonitor>()
        )
    }

    single<CashWithdrawalRepository> {
        CashWithdrawalRepositoryImpl(
            cloudCashWithdrawalDataSource = get<CloudCashWithdrawalDataSource>(),
            localQueryDataSource = get<LocalCashWithdrawalQueryDataSource>(),
            localWriteDataSource = get<LocalCashWithdrawalWriteDataSource>(),
            localGroupDataSource = get<LocalGroupDataSource>(),
            authenticationService = get<AuthenticationService>(),
            syncTeardownCoordinator = get<SyncTeardownCoordinator>(),
            performanceMonitor = get<PerformanceMonitor>(),
            ioDispatcher = Dispatchers.IO
        )
    }

    single<SettlementRepository> {
        SettlementRepositoryImpl(
            cloudSettlementDataSource = get<CloudSettlementDataSource>(),
            localSettlementDataSource = get<LocalSettlementDataSource>(),
            performanceMonitor = get<PerformanceMonitor>(),
            ioDispatcher = Dispatchers.IO
        )
    }

    single<CashTransferRepository> {
        CashTransferRepositoryImpl(
            cashTransferDao = get<CashTransferDao>(),
            cloudCashTransferDataSource = get<CloudCashTransferDataSource>(),
            performanceMonitor = get<PerformanceMonitor>(),
            ioDispatcher = Dispatchers.IO
        )
    }
}
