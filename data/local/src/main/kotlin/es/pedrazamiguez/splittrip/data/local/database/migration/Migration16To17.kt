package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds subunitId column to expense_splits table.
 * When non-null, indicates the user's split belongs to a subunit entity.
 */
internal val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense_splits ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}
