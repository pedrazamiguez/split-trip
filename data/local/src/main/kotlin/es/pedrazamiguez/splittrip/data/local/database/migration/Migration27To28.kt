package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the receipt attachment columns to [expenses] required by [ReceiptAttachment]:
 * - [receiptMimeType]         — MIME type of the attached file (e.g. "image/webp")
 * - [receiptCapturedAtMillis] — Epoch millis when the file was attached locally
 * - [receiptRemoteUrl]        — Firebase Storage download URL; NULL until upload completes
 *
 * **Backward-compatibility for pre-v28 rows:**
 * Rows that already have a non-null [receiptLocalUri] are backfilled with
 * `receiptMimeType = 'image/jpeg'` and `receiptCapturedAtMillis = 0` so that
 * [buildReceiptAttachment] can still reconstruct a valid [ReceiptAttachment] for them.
 * Without this backfill the mapper would treat them as "no attachment" because [mimeType]
 * would be null, and the receipt would silently disappear from the UI after the upgrade.
 */
internal val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptMimeType TEXT")
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptCapturedAtMillis INTEGER")
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptRemoteUrl TEXT")
        // Backfill legacy rows that already had a receipt before schema v28.
        db.execSQL(
            """UPDATE expenses
               SET receiptMimeType = 'image/jpeg', receiptCapturedAtMillis = 0
               WHERE receiptLocalUri IS NOT NULL"""
        )
    }
}
