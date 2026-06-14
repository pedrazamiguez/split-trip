package es.pedrazamiguez.splittrip.data.di

import androidx.work.WorkManager
import es.pedrazamiguez.splittrip.data.repository.impl.GroupRepositoryImpl
import es.pedrazamiguez.splittrip.data.worker.GroupDeletionRetryScheduler
import es.pedrazamiguez.splittrip.data.worker.GroupDeletionRetrySchedulerImpl
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.repository.GroupRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val groupsDataModule = module {

    single<WorkManager> { WorkManager.getInstance(androidContext()) }

    single<GroupDeletionRetryScheduler> {
        GroupDeletionRetrySchedulerImpl(workManager = get<WorkManager>())
    }

    single<GroupRepository> {
        GroupRepositoryImpl(
            cloudGroupDataSource = get<CloudGroupDataSource>(),
            localGroupDataSource = get<LocalGroupDataSource>(),
            authenticationService = get<AuthenticationService>(),
            groupDeletionRetryScheduler = get<GroupDeletionRetryScheduler>(),
            groupImageStorageService = get<GroupImageStorageService>(),
            cloudStorageDataSource = get<CloudStorageDataSource>()
        )
    }
}
