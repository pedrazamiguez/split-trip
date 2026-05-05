package es.pedrazamiguez.splittrip.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `groups` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `currencyCode` TEXT NOT NULL,
                `extraCurrencies` TEXT NOT NULL,
                `memberIds` TEXT NOT NULL,
                `mainImagePath` TEXT,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}

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

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // SQLite doesn't support adding foreign keys to existing tables
        // We need to recreate the table with the foreign key constraint

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

        // 3. Drop old table
        db.execSQL("DROP TABLE `expenses`")

        // 4. Rename new table to original name
        db.execSQL("ALTER TABLE `expenses_new` RENAME TO `expenses`")

        // 5. Recreate the index (existed in version 3, see MIGRATION_2_3)
        db.execSQL("CREATE INDEX `index_expenses_groupId` ON `expenses` (`groupId`)")
    }
}

internal val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `contributions` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX `index_contributions_groupId` ON `contributions` (`groupId`)")
    }
}

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
        // SQLite supports ALTER TABLE ADD COLUMN for nullable columns without defaults
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `cashTranchesJson` TEXT")
    }
}

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `category` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `vendor` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `paymentStatus` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `dueDateMillis` INTEGER")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `receiptLocalUri` TEXT")
    }
}

/**
 * Migration 7 → 8: Convert exchangeRate from REAL (Double) to TEXT (String/BigDecimal)
 * in both `expenses` and `cash_withdrawals` tables.
 *
 * SQLite does not support ALTER COLUMN, so we recreate each table with the new schema,
 * copy data (casting REAL to TEXT), drop the old table, and rename.
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

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add splitType column to expenses table
        db.execSQL(
            "ALTER TABLE `expenses` ADD COLUMN `splitType` TEXT NOT NULL DEFAULT 'EQUAL'"
        )

        // Create expense_splits table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expense_splits` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `expenseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `amountCents` INTEGER NOT NULL,
                `percentage` TEXT,
                `isExcluded` INTEGER NOT NULL,
                `isCoveredById` TEXT,
                FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)")
    }
}

internal val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `notes` TEXT DEFAULT NULL")
    }
}

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `userId` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `displayName` TEXT,
                `profileImagePath` TEXT,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent()
        )
    }
}

internal val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `users` ADD COLUMN `createdAtMillis` INTEGER DEFAULT NULL")
    }
}

internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `subunits` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `memberIds` TEXT NOT NULL,
                `memberShares` TEXT NOT NULL,
                `createdBy` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subunits_groupId` ON `subunits` (`groupId`)")
    }
}
