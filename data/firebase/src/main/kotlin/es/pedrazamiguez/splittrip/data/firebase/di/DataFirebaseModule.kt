package es.pedrazamiguez.splittrip.data.firebase.di

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import es.pedrazamiguez.splittrip.core.common.provider.AppMetadataProvider
import es.pedrazamiguez.splittrip.core.common.provider.LocaleProvider
import es.pedrazamiguez.splittrip.data.firebase.auth.service.impl.AuthenticationServiceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreCashWithdrawalDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreContributionDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreExpenseDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreGroupDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreNotificationDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreSubunitDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl.FirestoreUserDataSourceImpl
import es.pedrazamiguez.splittrip.data.firebase.installation.service.impl.CloudMetadataServiceImpl
import es.pedrazamiguez.splittrip.data.firebase.messaging.handler.factory.NotificationHandlerFactory
import es.pedrazamiguez.splittrip.data.firebase.messaging.repository.impl.FirebaseDeviceRepositoryImpl
import es.pedrazamiguez.splittrip.data.firebase.storage.CloudStorageDataSourceImpl
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudNotificationDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSubunitDataSource
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudUserDataSource
import es.pedrazamiguez.splittrip.domain.repository.DeviceRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.CloudMetadataService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataFirebaseModule = module {

    single { FirebaseAppCheck.getInstance() }

    single<FirebaseAuth> { FirebaseAuth.getInstance() }

    single<FirebaseFirestore> { FirebaseFirestore.getInstance() }

    single<FirebaseMessaging> { FirebaseMessaging.getInstance() }

    single<FirebaseStorage> { FirebaseStorage.getInstance() }

    single<AuthenticationService> {
        AuthenticationServiceImpl(
            firebaseAuth = get<FirebaseAuth>(),
            cloudUserDataSource = get<CloudUserDataSource>()
        )
    }

    single<FirebaseInstallations> { FirebaseInstallations.getInstance() }

    single<CloudGroupDataSource> {
        FirestoreGroupDataSourceImpl(
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<CloudExpenseDataSource> {
        FirestoreExpenseDataSourceImpl(
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<CloudContributionDataSource> {
        FirestoreContributionDataSourceImpl(
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<CloudCashWithdrawalDataSource> {
        FirestoreCashWithdrawalDataSourceImpl(
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<CloudSubunitDataSource> {
        FirestoreSubunitDataSourceImpl(
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<CloudMetadataService> { CloudMetadataServiceImpl(firebaseInstallations = get<FirebaseInstallations>()) }

    single<CloudNotificationDataSource> {
        FirestoreNotificationDataSourceImpl(
            appMetadataProvider = get<AppMetadataProvider>(),
            firestore = get<FirebaseFirestore>(),
            authenticationService = get<AuthenticationService>(),
            cloudMetadataService = get<CloudMetadataService>()
        )
    }

    single<NotificationHandlerFactory> {
        NotificationHandlerFactory(
            context = androidContext(),
            localeProvider = get<LocaleProvider>()
        )
    }

    single<DeviceRepository> {
        FirebaseDeviceRepositoryImpl(
            firebaseMessaging = get<FirebaseMessaging>()
        )
    }

    single<CloudUserDataSource> {
        FirestoreUserDataSourceImpl(
            firestore = get<FirebaseFirestore>()
        )
    }

    single<CloudStorageDataSource> {
        CloudStorageDataSourceImpl(storage = get<FirebaseStorage>())
    }
}
