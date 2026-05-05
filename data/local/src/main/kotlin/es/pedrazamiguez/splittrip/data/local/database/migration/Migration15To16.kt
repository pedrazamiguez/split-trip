package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds withdrawalScope and subunitId columns to cash_withdrawals table.
 * withdrawalScope defaults to 'GROUP' for backward compatibility.
 * subunitId is only set when withdrawalScope is 'SUBUNIT'.
 */
internal val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE cash_withdrawals ADD COLUMN withdrawalScope TEXT NOT NULL DEFAULT 'GROUP'")
        db.execSQL("ALTER TABLE cash_withdrawals ADD COLUMN subunitId TEXT DEFAULT NULL")
    }
}
