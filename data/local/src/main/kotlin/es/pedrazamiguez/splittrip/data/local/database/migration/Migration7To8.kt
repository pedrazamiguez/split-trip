package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Converts `exchangeRate` from REAL (Double) to TEXT (String/BigDecimal)
 * in both `expenses` and `cash_withdrawals` tables.
 *
 * SQLite does not support ALTER COLUMN, so each table is recreated with the new schema,
 * data is copied (casting REAL to TEXT), the old table is dropped, and the new one is renamed.
 */
internal val MIGRATION_7_8 = object : Migration(7, 8) {
    @Suppress("LongMethod") // Room DDL migration — recreates two tables; cannot be meaningfully split
    override fun migrate(db: SupportSQLiteDatabase) {
        // ── expenses table ──────────────────────────────────────────────
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
                `exchangeRate` TEXT NOT NULL,
                `category` TEXT,
                `vendor` TEXT,
                `paymentMethod` TEXT NOT NULL,
                `paymentStatus` TEXT,
                `dueDateMillis` INTEGER,
                `receiptLocalUri` TEXT,
                `createdBy` TEXT NOT NULL,
                `payerType` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                `cashTranchesJson` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `expenses_new` (
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `sourceTipAmount`, `sourceFeeAmount`, `groupAmount`, `groupCurrency`,
                `exchangeRate`, `category`, `vendor`, `paymentMethod`, `paymentStatus`,
                `dueDateMillis`, `receiptLocalUri`, `createdBy`, `payerType`,
                `createdAtMillis`, `lastUpdatedAtMillis`, `cashTranchesJson`
            )
            SELECT
                `id`, `groupId`, `title`, `sourceAmount`, `sourceCurrency`,
                `sourceTipAmount`, `sourceFeeAmount`, `groupAmount`, `groupCurrency`,
                CAST(`exchangeRate` AS TEXT), `category`, `vendor`, `paymentMethod`, `paymentStatus`,
                `dueDateMillis`, `receiptLocalUri`, `createdBy`, `payerType`,
                `createdAtMillis`, `lastUpdatedAtMillis`, `cashTranchesJson`
            FROM `expenses`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `expenses`")
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")
        db.execSQL("CREATE INDEX `index_expenses_groupId` ON `expenses` (`groupId`)")

        // ── cash_withdrawals table ──────────────────────────────────────
        db.execSQL(
            """
            CREATE TABLE `cash_withdrawals_new` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `withdrawnBy` TEXT NOT NULL,
                `amountWithdrawn` INTEGER NOT NULL,
                `remainingAmount` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `deductedBaseAmount` INTEGER NOT NULL,
                `exchangeRate` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `cash_withdrawals_new` (
                `id`, `groupId`, `withdrawnBy`, `amountWithdrawn`, `remainingAmount`,
                `currency`, `deductedBaseAmount`, `exchangeRate`,
                `createdAtMillis`, `lastUpdatedAtMillis`
            )
            SELECT
                `id`, `groupId`, `withdrawnBy`, `amountWithdrawn`, `remainingAmount`,
                `currency`, `deductedBaseAmount`, CAST(`exchangeRate` AS TEXT),
                `createdAtMillis`, `lastUpdatedAtMillis`
            FROM `cash_withdrawals`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `cash_withdrawals`")
        db.execSQL("ALTER TABLE `cash_withdrawals_new` RENAME TO `cash_withdrawals`")
        db.execSQL("CREATE INDEX `index_cash_withdrawals_groupId` ON `cash_withdrawals` (`groupId`)")
    }
}
