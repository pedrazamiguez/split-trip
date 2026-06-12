package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN bio TEXT")
        db.execSQL("ALTER TABLE users ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
    }
}
