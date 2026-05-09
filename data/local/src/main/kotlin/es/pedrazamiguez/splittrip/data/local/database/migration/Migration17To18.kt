package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
