package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds the receipt attachment columns to [expenses] required by [ReceiptAttachment]:
 * - [receiptMimeType]         — MIME type of the attached file (e.g. "image/webp")
 * - [receiptCapturedAtMillis] — Epoch millis when the file was attached locally
 * - [receiptRemoteUrl]        — Firebase Storage download URL; NULL until upload completes
 *
 * Existing rows receive NULL for all three columns, which the read-path maps to a
 * null [ReceiptAttachment] — backward-compatible with receipts attached before this
 * migration (those rows still have a non-null [receiptLocalUri] and will continue to
 * display correctly once the mapper constructs the attachment from available columns).
 */
internal val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptMimeType TEXT")
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptCapturedAtMillis INTEGER")
        db.execSQL("ALTER TABLE expenses ADD COLUMN receiptRemoteUrl TEXT")
    }
}
