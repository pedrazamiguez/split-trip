package es.pedrazamiguez.splittrip.data.local.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import es.pedrazamiguez.splittrip.data.local.converter.AddOnListConverter
import es.pedrazamiguez.splittrip.data.local.converter.BigDecimalConverter
import es.pedrazamiguez.splittrip.data.local.converter.CashTrancheListConverter
import es.pedrazamiguez.splittrip.data.local.converter.StringBigDecimalMapConverter
import es.pedrazamiguez.splittrip.data.local.converter.StringListConverter
import es.pedrazamiguez.splittrip.data.local.converter.SubExpenseListConverter
import es.pedrazamiguez.splittrip.data.local.dao.CashTransferDao
import es.pedrazamiguez.splittrip.data.local.dao.CashWithdrawalDao
import es.pedrazamiguez.splittrip.data.local.dao.ContributionDao
import es.pedrazamiguez.splittrip.data.local.dao.CurrencyDao
import es.pedrazamiguez.splittrip.data.local.dao.ExchangeRateDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseSplitDao
import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.dao.MembershipRemovalEventDao
import es.pedrazamiguez.splittrip.data.local.dao.SettlementRecordDao
import es.pedrazamiguez.splittrip.data.local.dao.SubunitDao
import es.pedrazamiguez.splittrip.data.local.dao.UserDao
import es.pedrazamiguez.splittrip.data.local.entity.CashTransferEntity
import es.pedrazamiguez.splittrip.data.local.entity.CashWithdrawalEntity
import es.pedrazamiguez.splittrip.data.local.entity.ContributionEntity
import es.pedrazamiguez.splittrip.data.local.entity.CurrencyEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExchangeRateEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseSplitEntity
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.data.local.entity.MembershipRemovalEventEntity
import es.pedrazamiguez.splittrip.data.local.entity.SettlementRecordEntity
import es.pedrazamiguez.splittrip.data.local.entity.SubunitEntity
import es.pedrazamiguez.splittrip.data.local.entity.UserEntity

@Database(
    entities = [
        CurrencyEntity::class,
        ExchangeRateEntity::class,
        GroupEntity::class,
        ExpenseEntity::class,
        ExpenseSplitEntity::class,
        ContributionEntity::class,
        CashWithdrawalEntity::class,
        CashTransferEntity::class,
        UserEntity::class,
        SubunitEntity::class,
        SettlementRecordEntity::class,
        MembershipRemovalEventEntity::class
    ],
    version = 44,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38),
        AutoMigration(from = 39, to = 40),
        AutoMigration(from = 40, to = 41),
        AutoMigration(from = 41, to = 42),
        AutoMigration(from = 42, to = 43),
        AutoMigration(from = 43, to = 44)
    ]
)
@TypeConverters(
    BigDecimalConverter::class,
    StringListConverter::class,
    CashTrancheListConverter::class,
    StringBigDecimalMapConverter::class,
    AddOnListConverter::class,
    SubExpenseListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun contributionDao(): ContributionDao
    abstract fun cashWithdrawalDao(): CashWithdrawalDao
    abstract fun cashTransferDao(): CashTransferDao
    abstract fun userDao(): UserDao
    abstract fun subunitDao(): SubunitDao
    abstract fun settlementRecordDao(): SettlementRecordDao
    abstract fun membershipRemovalEventDao(): MembershipRemovalEventDao
}
