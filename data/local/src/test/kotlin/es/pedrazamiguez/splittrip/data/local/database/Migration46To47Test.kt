package es.pedrazamiguez.splittrip.data.local.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.database.migration.MIGRATION_46_47
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration46To47Test {

    private lateinit var helper: SupportSQLiteOpenHelper
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(46) {
                override fun onCreate(db: SupportSQLiteDatabase) {
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
                            `syncStatus` TEXT NOT NULL,
                            `status` TEXT NOT NULL,
                            `archivedAtMillis` INTEGER,
                            `createdBy` TEXT NOT NULL,
                            `context` TEXT NOT NULL DEFAULT 'TRAVEL',
                            PRIMARY KEY(`id`)
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        helper = FrameworkSQLiteOpenHelperFactory().create(config)
        db = helper.writableDatabase
    }

    @After
    fun tearDown() {
        db.close()
        helper.close()
    }

    @Test
    fun migration46To47_createsGroupSyncCheckpointsTableWithExpectedColumnsAndPrimaryKey() {
        MIGRATION_46_47.migrate(db)

        val columns = mutableMapOf<String, Triple<String, Int, Int>>()
        db.query("PRAGMA table_info('group_sync_checkpoints')").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            val typeIdx = cursor.getColumnIndexOrThrow("type")
            val notNullIdx = cursor.getColumnIndexOrThrow("notnull")
            val pkIdx = cursor.getColumnIndexOrThrow("pk")
            while (cursor.moveToNext()) {
                columns[cursor.getString(nameIdx)] = Triple(
                    cursor.getString(typeIdx),
                    cursor.getInt(notNullIdx),
                    cursor.getInt(pkIdx)
                )
            }
        }

        assertEquals(5, columns.size)
        assertEquals(Triple("TEXT", 1, 1), columns["groupId"])
        assertEquals(Triple("TEXT", 1, 2), columns["collectionName"])
        assertEquals(Triple("INTEGER", 1, 0), columns["lastServerUpdatedSeconds"])
        assertEquals(Triple("INTEGER", 1, 0), columns["lastServerUpdatedNanos"])
        assertEquals(Triple("INTEGER", 1, 0), columns["lastSyncedAtClientMillis"])
    }

    @Test
    fun migration46To47_createsGroupIdIndex() {
        MIGRATION_46_47.migrate(db)

        val indexNames = mutableListOf<String>()
        db.query("PRAGMA index_list('group_sync_checkpoints')").use { cursor ->
            val nameIdx = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                indexNames.add(cursor.getString(nameIdx))
            }
        }

        assertTrue(indexNames.contains("index_group_sync_checkpoints_groupId"))
    }

    @Test
    fun migration46To47_cascadesDeleteWhenParentGroupDeleted() {
        db.execSQL("PRAGMA foreign_keys=ON")
        db.execSQL(
            """
            INSERT INTO `groups` (`id`, `name`, `currencyCode`, `extraCurrencies`, `memberIds`, `syncStatus`, `status`, `createdBy`)
            VALUES ('group-1', 'Trip', 'EUR', '[]', '[]', 'SYNCED', 'ACTIVE', 'user-1')
            """.trimIndent()
        )

        MIGRATION_46_47.migrate(db)

        db.execSQL(
            """
            INSERT INTO `group_sync_checkpoints`
            (`groupId`, `collectionName`, `lastServerUpdatedSeconds`, `lastServerUpdatedNanos`, `lastSyncedAtClientMillis`)
            VALUES ('group-1', 'expenses', 1700000000, 123456789, 1700000001000)
            """.trimIndent()
        )

        db.execSQL("DELETE FROM `groups` WHERE `id` = 'group-1'")

        db.query("SELECT COUNT(*) FROM `group_sync_checkpoints` WHERE `groupId` = 'group-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migration46To47_preservesExistingGroupRows() {
        db.execSQL(
            """
            INSERT INTO `groups` (`id`, `name`, `currencyCode`, `extraCurrencies`, `memberIds`, `syncStatus`, `status`, `createdBy`)
            VALUES ('group-existing', 'Spain Trip', 'EUR', '[]', '["u1"]', 'SYNCED', 'ACTIVE', 'u1')
            """.trimIndent()
        )

        MIGRATION_46_47.migrate(db)

        db.query("SELECT `name`, `currencyCode` FROM `groups` WHERE `id` = 'group-existing'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Spain Trip", cursor.getString(0))
            assertEquals("EUR", cursor.getString(1))
            assertFalse(cursor.moveToNext())
        }
    }
}
