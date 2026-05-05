package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `subunits` (
                `id` TEXT NOT NULL,
                `groupId` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `memberIds` TEXT NOT NULL,
                `memberShares` TEXT NOT NULL,
                `createdBy` TEXT NOT NULL,
                `createdAtMillis` INTEGER,
                `lastUpdatedAtMillis` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_subunits_groupId` ON `subunits` (`groupId`)")
    }
}
