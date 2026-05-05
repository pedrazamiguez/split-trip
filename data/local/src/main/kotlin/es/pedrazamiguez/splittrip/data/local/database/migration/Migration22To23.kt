package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
