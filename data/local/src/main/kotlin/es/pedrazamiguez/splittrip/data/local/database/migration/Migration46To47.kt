package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_46_47 = object : Migration(46, 47) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `group_sync_checkpoints` (
                `groupId` TEXT NOT NULL,
                `collectionName` TEXT NOT NULL,
                `lastServerUpdatedSeconds` INTEGER NOT NULL,
                `lastServerUpdatedNanos` INTEGER NOT NULL,
                `lastSyncedAtClientMillis` INTEGER NOT NULL,
                PRIMARY KEY(`groupId`, `collectionName`),
                FOREIGN KEY(`groupId`) REFERENCES `groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_group_sync_checkpoints_groupId` " +
                "ON `group_sync_checkpoints` (`groupId`)"
        )
    }
}
