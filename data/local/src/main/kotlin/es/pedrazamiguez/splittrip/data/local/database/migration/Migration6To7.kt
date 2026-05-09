package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `category` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `vendor` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `paymentStatus` TEXT")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `dueDateMillis` INTEGER")
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `receiptLocalUri` TEXT")
    }
}
