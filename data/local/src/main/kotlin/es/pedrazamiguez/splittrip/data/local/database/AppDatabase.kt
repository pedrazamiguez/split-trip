package es.pedrazamiguez.splittrip.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import es.pedrazamiguez.splittrip.data.local.converter.AddOnListConverter
import es.pedrazamiguez.splittrip.data.local.converter.BigDecimalConverter
import es.pedrazamiguez.splittrip.data.local.converter.CashTrancheListConverter
import es.pedrazamiguez.splittrip.data.local.converter.StringBigDecimalMapConverter
import es.pedrazamiguez.splittrip.data.local.converter.StringListConverter
import es.pedrazamiguez.splittrip.data.local.dao.CashWithdrawalDao
import es.pedrazamiguez.splittrip.data.local.dao.ContributionDao
import es.pedrazamiguez.splittrip.data.local.dao.CurrencyDao
import es.pedrazamiguez.splittrip.data.local.dao.ExchangeRateDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseSplitDao
import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.dao.SubunitDao
import es.pedrazamiguez.splittrip.data.local.dao.UserDao
import es.pedrazamiguez.splittrip.data.local.entity.CashWithdrawalEntity
import es.pedrazamiguez.splittrip.data.local.entity.ContributionEntity
import es.pedrazamiguez.splittrip.data.local.entity.CurrencyEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExchangeRateEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.data.local.entity.ExpenseSplitEntity
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
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
        UserEntity::class,
        SubunitEntity::class
    ],
    version = 29,
    exportSchema = true
)
@TypeConverters(
    BigDecimalConverter::class,
    StringListConverter::class,
    CashTrancheListConverter::class,
    StringBigDecimalMapConverter::class,
    AddOnListConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun currencyDao(): CurrencyDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun groupDao(): GroupDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseSplitDao(): ExpenseSplitDao
    abstract fun contributionDao(): ContributionDao
    abstract fun cashWithdrawalDao(): CashWithdrawalDao
    abstract fun userDao(): UserDao
    abstract fun subunitDao(): SubunitDao
}
