package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.data.repository.impl.ExpenseRepositoryImpl
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

val expensesDataModule = module {
    single<ExpenseRepository> {
        ExpenseRepositoryImpl(
            cloudExpenseDataSource = get<CloudExpenseDataSource>(),
            localExpenseDataSource = get<LocalExpenseDataSource>(),
            authenticationService = get<AuthenticationService>(),
            cloudStorageDataSource = get<CloudStorageDataSource>(),
            ioDispatcher = Dispatchers.IO
        )
    }
}
