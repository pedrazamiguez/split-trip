package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
