package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `users` (
                `userId` TEXT NOT NULL,
                `email` TEXT NOT NULL,
                `displayName` TEXT,
                `profileImagePath` TEXT,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`userId`)
            )
            """.trimIndent()
        )
    }
}
