package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds [splitType] to [expense_splits] to persist the Level 2 (intra-subunit) split strategy
 * alongside each subunit member's share.
 *
 * Existing rows receive NULL, which the read-path mapper interprets as EQUAL — consistent with
 * backward-compatibility requirements in issue #1090.
 */
internal val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expense_splits ADD COLUMN splitType TEXT")
    }
}
