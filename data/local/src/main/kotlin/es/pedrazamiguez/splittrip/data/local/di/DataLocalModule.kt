package es.pedrazamiguez.splittrip.data.local.di

import android.app.Application
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import es.pedrazamiguez.splittrip.data.local.dao.CashWithdrawalDao
import es.pedrazamiguez.splittrip.data.local.dao.ContributionDao
import es.pedrazamiguez.splittrip.data.local.dao.CurrencyDao
import es.pedrazamiguez.splittrip.data.local.dao.ExchangeRateDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseSplitDao
import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.dao.SubunitDao
import es.pedrazamiguez.splittrip.data.local.dao.UserDao
import es.pedrazamiguez.splittrip.data.local.database.ALL_MIGRATIONS
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalCashWithdrawalDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalContributionDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalCurrencyDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalExpenseDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalGroupDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalSubunitDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalUserDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.datastore.NotificationUserPreferences
import es.pedrazamiguez.splittrip.data.local.datastore.UserPreferences
import es.pedrazamiguez.splittrip.data.local.service.GroupImageStorageServiceImpl
import es.pedrazamiguez.splittrip.data.local.service.ProfileImageStorageServiceImpl
import es.pedrazamiguez.splittrip.data.local.service.ReceiptStorageServiceImpl
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalQueryDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCashWithdrawalWriteDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalContributionDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalCurrencyDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalExpenseDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalGroupDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalSubunitDataSource
import es.pedrazamiguez.splittrip.domain.datasource.local.LocalUserDataSource
import es.pedrazamiguez.splittrip.domain.repository.AppConfigRepository
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import es.pedrazamiguez.splittrip.domain.service.GroupImageStorageService
import es.pedrazamiguez.splittrip.domain.service.ProfileImageStorageService
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataLocalModule = module {

    single {
        UserPreferences(
            context = androidContext(),
            authenticationService = get<AuthenticationService>(),
            appConfigRepository = get<AppConfigRepository>()
        )
    }

    single {
        NotificationUserPreferences(
            context = androidContext(),
            authenticationService = get<AuthenticationService>()
        )
    }

    single<AppDatabase> {
        Room
            .databaseBuilder(
                context = get<Application>(),
                klass = AppDatabase::class.java,
                name = "splittrip_db"
            )
            .apply { ALL_MIGRATIONS.forEach { addMigrations(it) } }
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Enable foreign key constraints for all connections
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            })
            .build()
    }

    single<CurrencyDao> { get<AppDatabase>().currencyDao() }

    single<ExchangeRateDao> { get<AppDatabase>().exchangeRateDao() }

    single<GroupDao> { get<AppDatabase>().groupDao() }

    single<ExpenseDao> { get<AppDatabase>().expenseDao() }

    single<ExpenseSplitDao> { get<AppDatabase>().expenseSplitDao() }

    single<ContributionDao> { get<AppDatabase>().contributionDao() }

    single<CashWithdrawalDao> { get<AppDatabase>().cashWithdrawalDao() }

    single<LocalCurrencyDataSource> {
        LocalCurrencyDataSourceImpl(
            currencyDao = get<CurrencyDao>(),
            exchangeRateDao = get<ExchangeRateDao>()
        )
    }

    single<LocalGroupDataSource> {
        LocalGroupDataSourceImpl(
            groupDao = get<GroupDao>()
        )
    }

    single<LocalExpenseDataSource> {
        LocalExpenseDataSourceImpl(
            appDatabase = get<AppDatabase>(),
            expenseDao = get<ExpenseDao>(),
            expenseSplitDao = get<ExpenseSplitDao>()
        )
    }

    single<LocalContributionDataSource> {
        LocalContributionDataSourceImpl(
            contributionDao = get<ContributionDao>()
        )
    }

    // Register the single impl as all three types so either segregated interface or the
    // combined LocalCashWithdrawalDataSource can be injected without duplicate instances.
    single<LocalCashWithdrawalDataSourceImpl> {
        LocalCashWithdrawalDataSourceImpl(
            cashWithdrawalDao = get<CashWithdrawalDao>()
        )
    }
    single<LocalCashWithdrawalQueryDataSource> { get<LocalCashWithdrawalDataSourceImpl>() }
    single<LocalCashWithdrawalWriteDataSource> { get<LocalCashWithdrawalDataSourceImpl>() }
    single<LocalCashWithdrawalDataSource> { get<LocalCashWithdrawalDataSourceImpl>() }

    single<UserDao> { get<AppDatabase>().userDao() }

    single<SubunitDao> { get<AppDatabase>().subunitDao() }

    single<LocalUserDataSource> {
        LocalUserDataSourceImpl(
            userDao = get<UserDao>()
        )
    }

    single<LocalSubunitDataSource> {
        LocalSubunitDataSourceImpl(
            subunitDao = get<SubunitDao>()
        )
    }

    single<ReceiptStorageService> {
        ReceiptStorageServiceImpl(context = androidContext())
    }

    single<ProfileImageStorageService> {
        ProfileImageStorageServiceImpl(context = androidContext())
    }

    single<GroupImageStorageService> {
        GroupImageStorageServiceImpl(context = androidContext())
    }
}
