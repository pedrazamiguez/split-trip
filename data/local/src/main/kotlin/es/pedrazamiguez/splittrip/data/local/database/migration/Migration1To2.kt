package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `groups` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT,
                `currencyCode` TEXT NOT NULL,
                `extraCurrencies` TEXT NOT NULL,
                `memberIds` TEXT NOT NULL,
                `mainImagePath` TEXT,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }
}
