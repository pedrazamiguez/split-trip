package es.pedrazamiguez.splittrip.data.di

import es.pedrazamiguez.splittrip.data.repository.impl.UserRepositoryImpl
import es.pedrazamiguez.splittrip.data.service.FeatureGateServiceImpl
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.repository.UserRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.featuregate.FeatureGateService
import org.koin.dsl.module

val authenticationDataModule = module {
    single<UserRepository> {
        UserRepositoryImpl(
            cloudUserDataSource = get<CloudUserDataSource>(),
            localUserDataSource = get<LocalUserDataSource>(),
            cloudStorageDataSource = get<CloudStorageDataSource>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<FeatureGateService> {
        FeatureGateServiceImpl(
            authenticationService = get<AuthenticationService>()
        )
    }
}
