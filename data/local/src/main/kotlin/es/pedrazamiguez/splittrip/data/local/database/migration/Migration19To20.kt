package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
