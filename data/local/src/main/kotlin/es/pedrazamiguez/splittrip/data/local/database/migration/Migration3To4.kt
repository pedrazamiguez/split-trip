package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support adding foreign keys to existing tables — recreate the table.

        // 1. Create new expenses table with foreign key constraint
        db.execSQL(
            """
            CREATE TABLE `expenses_new` (
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
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // 2. Copy data from old table to new table (explicit column listing for safety)
        db.execSQL(
            """
            INSERT INTO `expenses_new` (
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `sourceTipAmount`, `sourceFeeAmount`, `groupAmount`, `groupCurrency`,
                `exchangeRate`, `paymentMethod`, `createdBy`, `payerType`,
                `createdAtMillis`, `lastUpdatedAtMillis`
            )
            SELECT
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `sourceTipAmount`, `sourceFeeAmount`, `groupAmount`, `groupCurrency`,
                `exchangeRate`, `paymentMethod`, `createdBy`, `payerType`,
                `createdAtMillis`, `lastUpdatedAtMillis`
            FROM `expenses`
            """.trimIndent()
        )

        // 3. Drop old table, rename new table, recreate index
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
        db.execSQL("CREATE INDEX `index_expenses_groupId` ON `expenses` (`groupId`)")
    }
}
