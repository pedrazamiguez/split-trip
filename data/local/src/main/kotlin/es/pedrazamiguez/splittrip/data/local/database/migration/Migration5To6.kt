package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create cash_withdrawals table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `cash_withdrawals` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `withdrawnBy` TEXT NOT NULL,
                `amountWithdrawn` INTEGER NOT NULL,
                `remainingAmount` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `deductedBaseAmount` INTEGER NOT NULL,
                `exchangeRate` REAL NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX `index_cash_withdrawals_groupId` ON `cash_withdrawals` (`groupId`)")

        // 2. Add cashTranchesJson column to expenses table
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `cashTranchesJson` TEXT")
    }
}
