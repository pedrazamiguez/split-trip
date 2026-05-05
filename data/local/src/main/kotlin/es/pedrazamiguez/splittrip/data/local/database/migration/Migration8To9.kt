package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `expenses` ADD COLUMN `splitType` TEXT NOT NULL DEFAULT 'EQUAL'")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expense_splits` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `expenseId` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `amountCents` INTEGER NOT NULL,
                `percentage` TEXT,
                `isExcluded` INTEGER NOT NULL,
                `isCoveredById` TEXT,
                FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_splits_expenseId` ON `expense_splits` (`expenseId`)")
    }
}
