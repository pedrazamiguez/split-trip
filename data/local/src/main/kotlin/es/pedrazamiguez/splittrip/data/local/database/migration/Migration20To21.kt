package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
