package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds optional subunitId column to contributions table.
 * When non-null, the contribution was made on behalf of a subunit.
 */
internal val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contributions ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}
