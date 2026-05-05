package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 1. Recreates `expenses` table to drop unused `sourceTipAmount` / `sourceFeeAmount` columns
 *    and add `addOnsJson` for the new structured add-ons model.
 * 2. Adds `addOnsJson` column to `cash_withdrawals` via ALTER TABLE.
 */
internal val MIGRATION_18_19 = object : Migration(18, 19) {
    @Suppress("LongMethod") // Room DDL migration — recreates the expenses table; cannot be meaningfully split
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── 1. Recreate expenses table ───────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE `expenses_new` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `sourceAmount` INTEGER NOT NULL,
                `sourceCurrency` TEXT NOT NULL,
                `groupAmount` INTEGER NOT NULL,
                `groupCurrency` TEXT NOT NULL,
                `exchangeRate` TEXT NOT NULL,
                `category` TEXT,
                `vendor` TEXT,
                `notes` TEXT,
                `paymentMethod` TEXT NOT NULL,
                `paymentStatus` TEXT,
                `dueDateMillis` INTEGER,
                `receiptLocalUri` TEXT,
                `createdBy` TEXT NOT NULL,
                `payerType` TEXT NOT NULL,
                `splitType` TEXT NOT NULL DEFAULT 'EQUAL',
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                `cashTranchesJson` TEXT,
                `addOnsJson` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `expenses_new` (
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `groupAmount`, `groupCurrency`, `exchangeRate`,
                `category`, `vendor`, `notes`, `paymentMethod`, `paymentStatus`,
                `dueDateMillis`, `receiptLocalUri`, `createdBy`, `payerType`, `splitType`,
                `createdAtMillis`, `lastUpdatedAtMillis`, `cashTranchesJson`
            )
            SELECT
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `groupAmount`, `groupCurrency`, `exchangeRate`,
                `category`, `vendor`, `notes`, `paymentMethod`, `paymentStatus`,
                `dueDateMillis`, `receiptLocalUri`, `createdBy`, `payerType`, `splitType`,
                `createdAtMillis`, `lastUpdatedAtMillis`, `cashTranchesJson`
            FROM `expenses`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
        db.execSQL("CREATE INDEX `index_expenses_groupId` ON `expenses` (`groupId`)")

        // ── 2. Add addOnsJson to cash_withdrawals ───────────────────────
        db.execSQL("ALTER TABLE `cash_withdrawals` ADD COLUMN `addOnsJson` TEXT DEFAULT NULL")
    }
}
