package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
