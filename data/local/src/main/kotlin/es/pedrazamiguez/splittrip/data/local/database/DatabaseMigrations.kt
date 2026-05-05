package es.pedrazamiguez.splittrip.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * No-op migration: memberShares is stored as TEXT (JSON), e.g. {"u1":0.5,"u2":0.5}.
 * The new StringBigDecimalMapConverter parses the same format — BigDecimal("0.5")
 * reads the existing serialized values correctly. No data transformation required.
 * Version bump is needed because the TypeConverter class reference changed
 * from StringDoubleMapConverter to StringBigDecimalMapConverter.
 */
internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op: existing TEXT column data is compatible with BigDecimal parsing
    }
}

/**
 * Adds optional subunitId column to contributions table.
 * When non-null, the contribution was made on behalf of a subunit.
 */
internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contributions ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}

/**
 * Adds withdrawalScope and subunitId columns to cash_withdrawals table.
 * withdrawalScope defaults to 'GROUP' for backward compatibility.
 * subunitId is only set when withdrawalScope is 'SUBUNIT'.
 */
internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cash_withdrawals ADD COLUMN withdrawalScope TEXT NOT NULL DEFAULT 'GROUP'")
        db.execSQL("ALTER TABLE cash_withdrawals ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}

/**
 * Adds subunitId column to expense_splits table.
 * When non-null, indicates the user's split belongs to a subunit entity.
 */
internal val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense_splits ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}

/**
 * Adds `contributionScope` column to contributions table.
 * Defaults to 'USER' for individual contributions and infers 'SUBUNIT'
 * for existing contributions that have a non-null subunitId.
 */
internal val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contributions ADD COLUMN contributionScope TEXT NOT NULL DEFAULT 'USER'")
        db.execSQL("UPDATE contributions SET contributionScope = 'SUBUNIT' WHERE subunitId IS NOT NULL")
    }
}

/**
 * 1. Recreates `expenses` table to drop unused `sourceTipAmount` / `sourceFeeAmount` columns
 *    and add `addOnsJson` for the new structured add-ons model.
 * 2. Adds `addOnsJson` column to `cash_withdrawals` via ALTER TABLE.
 */
internal val MIGRATION_18_19 = object : Migration(18, 19) {
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

/**
 * Adds `title`, `notes`, and `receiptLocalUri` nullable columns to `cash_withdrawals`.
 * These are optional metadata fields for annotating where/why cash was obtained.
 */
internal val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `cash_withdrawals` ADD COLUMN `title` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `cash_withdrawals` ADD COLUMN `notes` TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE `cash_withdrawals` ADD COLUMN `receiptLocalUri` TEXT DEFAULT NULL")
    }
}

/**
 * Adds `createdBy` column to `contributions` and `cash_withdrawals` tables.
 * Tracks the authenticated user who performed the action (actor),
 * separately from the target member (`userId` / `withdrawnBy`).
 * Defaults to empty string for existing rows (backward-compatible).
 */
internal val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `contributions` ADD COLUMN `createdBy` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `cash_withdrawals` ADD COLUMN `createdBy` TEXT NOT NULL DEFAULT ''")
    }
}

/**
 * Adds `payerId` nullable column to `expenses` table.
 * Tracks which member personally funded an out-of-pocket expense.
 * NULL when `payerType = GROUP` (the default).
 */
internal val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `payerId` TEXT DEFAULT NULL")
    }
}

/**
 * Adds `linkedExpenseId` nullable column to `contributions` table.
 * Links an auto-generated paired contribution to the out-of-pocket expense that created it.
 * NULL for manual (user-initiated) contributions.
 */
internal val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `contributions` ADD COLUMN `linkedExpenseId` TEXT DEFAULT NULL")
    }
}

/**
 * Adds a composite index on (groupId, linkedExpenseId) to `contributions` table.
 * Optimises the `deleteByLinkedExpenseId` and `findByLinkedExpenseId` queries
 * that filter by both columns.
 */
internal val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_contributions_groupId_linkedExpenseId` " +
                "ON `contributions` (`groupId`, `linkedExpenseId`)"
        )
    }
}

/**
 * Adds `syncStatus` column to all 5 entity tables for offline-first sync tracking.
 * Defaults to 'SYNCED' so all existing rows (which arrived via cloud snapshot
 * reconciliation) are correctly marked as already synchronized.
 *
 * This column is local-only metadata — it is NOT synced to Firestore.
 */
internal val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE groups ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE contributions ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE cash_withdrawals ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
        db.execSQL("ALTER TABLE subunits ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
    }
}

/**
 * Adds composite indexes to speed up balance-screen and sync-tracking queries.
 *
 * **Why these indexes?**
 *
 * 1. `expenses (groupId, syncStatus)` — eliminates full-table scans on
 *    `getPendingSyncExpenseIds` and `getUnsyncedExpenseStatuses`, which filter
 *    by both columns on every Firestore reconciliation cycle.
 *
 * 2. `contributions (groupId, syncStatus)` — same rationale for contribution
 *    sync tracking queries.
 *
 * 3. `cash_withdrawals (groupId, syncStatus)` — same rationale for withdrawal
 *    sync tracking queries.
 *
 * 4–6. FIFO pool indexes on `cash_withdrawals` — one per scoped query variant:
 *
 *    - **(groupId, currency, withdrawalScope, remainingAmount)** — covers GROUP-scoped
 *      and scope-blind (reconciliation/test-only) FIFO queries. GROUP-scoped filters
 *      by all four leading equality columns + range predicate `remainingAmount > 0`.
 *
 *    - **(groupId, currency, withdrawalScope, withdrawnBy, remainingAmount)** — covers
 *      USER-scoped FIFO queries. USER-scoped additionally filters by `withdrawnBy`,
 *      which is not present in the GROUP index; this index satisfies all predicates.
 *
 *    - **(groupId, currency, withdrawalScope, subunitId, remainingAmount)** — covers
 *      SUBUNIT-scoped FIFO queries. SUBUNIT-scoped additionally filters by `subunitId`;
 *      this index satisfies all predicates.
 *
 *    Note: `ORDER BY createdAtMillis ASC` follows a range predicate (`remainingAmount > 0`),
 *    so SQLite cannot use the index for ordering. An explicit sort on the filtered result
 *    set is acceptable given the small size of the pool (only non-exhausted withdrawals
 *    for one group + currency + scope).
 *
 * See issue #1012 for EXPLAIN QUERY PLAN analysis and benchmarking context.
 */
internal val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. expenses: composite index for sync-status queries
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_groupId_syncStatus` " +
                "ON `expenses` (`groupId`, `syncStatus`)"
        )

        // 2. contributions: composite index for sync-status queries
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_contributions_groupId_syncStatus` " +
                "ON `contributions` (`groupId`, `syncStatus`)"
        )

        // 3. cash_withdrawals: composite index for sync-status queries
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_cash_withdrawals_groupId_syncStatus` " +
                "ON `cash_withdrawals` (`groupId`, `syncStatus`)"
        )

        // 4. cash_withdrawals: GROUP-scoped and scope-blind FIFO queries
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                "`index_cash_withdrawals_groupId_currency_withdrawalScope_remainingAmount` " +
                "ON `cash_withdrawals` (`groupId`, `currency`, `withdrawalScope`, `remainingAmount`)"
        )

        // 5. cash_withdrawals: USER-scoped FIFO queries (adds withdrawnBy predicate)
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                "`index_cash_withdrawals_groupId_currency_withdrawalScope_withdrawnBy_remainingAmount` " +
                "ON `cash_withdrawals` " +
                "(`groupId`, `currency`, `withdrawalScope`, `withdrawnBy`, `remainingAmount`)"
        )

        // 6. cash_withdrawals: SUBUNIT-scoped FIFO queries (adds subunitId predicate)
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS " +
                "`index_cash_withdrawals_groupId_currency_withdrawalScope_subunitId_remainingAmount` " +
                "ON `cash_withdrawals` " +
                "(`groupId`, `currency`, `withdrawalScope`, `subunitId`, `remainingAmount`)"
        )
    }
}

/**
 * All Room database migrations, ordered sequentially.
 * Early migrations (1–12) are defined in [DatabaseMigrationsEarly].
 * Referenced by [es.pedrazamiguez.splittrip.data.local.di.dataLocalModule]
 * when building the [androidx.room.RoomDatabase].
 */
internal val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
    MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15,
    MIGRATION_15_16,
    MIGRATION_16_17,
    MIGRATION_17_18,
    MIGRATION_18_19,
    MIGRATION_19_20,
    MIGRATION_20_21,
    MIGRATION_21_22,
    MIGRATION_22_23,
    MIGRATION_23_24,
    MIGRATION_24_25,
    MIGRATION_25_26
)
