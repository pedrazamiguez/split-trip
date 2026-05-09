package es.pedrazamiguez.splittrip.data.local.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * No-op migration: memberShares is stored as TEXT (JSON), e.g. {"u1":0.5,"u2":0.5}.
 * The new StringBigDecimalMapConverter parses the same format — BigDecimal("0.5")
 * reads the existing serialized values correctly. No data transformation required.
 * Version bump is needed because the TypeConverter class reference changed
 * from StringDoubleMapConverter to StringBigDecimalMapConverter.
 */
internal val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No-op: existing TEXT column data is compatible with BigDecimal parsing
    }
}
