package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sourceAmount` INTEGER NOT NULL,
                `sourceCurrency` TEXT NOT NULL,
                `sourceTipAmount` INTEGER NOT NULL,
                `sourceFeeAmount` INTEGER NOT NULL,
                `groupAmount` INTEGER NOT NULL,
                `groupCurrency` TEXT NOT NULL,
                `exchangeRate` REAL NOT NULL,
                `paymentMethod` TEXT NOT NULL,
                `createdBy` TEXT NOT NULL,
                `payerType` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_groupId` ON `expenses` (`groupId`)")
    }
}
