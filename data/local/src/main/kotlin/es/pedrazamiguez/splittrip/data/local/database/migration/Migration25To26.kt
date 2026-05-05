package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds composite indexes to speed up balance-screen and sync-tracking queries.
 *
 * 1. `expenses (groupId, syncStatus)` — eliminates full-table scans on
 *    `getPendingSyncExpenseIds` and `getUnsyncedExpenseStatuses`.
 * 2. `contributions (groupId, syncStatus)` — same rationale for contribution sync tracking.
 * 3. `cash_withdrawals (groupId, syncStatus)` — same rationale for withdrawal sync tracking.
 * 4. `cash_withdrawals (groupId, currency, withdrawalScope, remainingAmount)` — GROUP-scoped FIFO.
 * 5. `cash_withdrawals (groupId, currency, withdrawalScope, withdrawnBy, remainingAmount)` — USER-scoped FIFO.
 * 6. `cash_withdrawals (groupId, currency, withdrawalScope, subunitId, remainingAmount)` — SUBUNIT-scoped FIFO.
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
