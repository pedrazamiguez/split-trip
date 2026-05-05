package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds a composite index on (groupId, linkedExpenseId) to `contributions` table.
 * Optimises the `deleteByLinkedExpenseId` and `findByLinkedExpenseId` queries
 * that filter by both columns.
 */
internal val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_contributions_groupId_linkedExpenseId` " +
                "ON `contributions` (`groupId`, `linkedExpenseId`)"
        )
    }
}
