package es.pedrazamiguez.splittrip.data.local.datasource

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalSyncCheckpointDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.domain.enums.SyncCollectionName
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalSyncCheckpointDataSourceImplTest {

    private lateinit var db: AppDatabase
    private lateinit var dataSource: LocalSyncCheckpointDataSourceImpl

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    db.execSQL("PRAGMA foreign_keys=ON")
                }
            })
            .build()
        dataSource = LocalSyncCheckpointDataSourceImpl(db.groupSyncCheckpointDao())

        db.groupDao().insertGroups(
            listOf(
                createGroup("group-1"),
                createGroup("group-2")
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getWatermark_returnsNullWhenNoCheckpointExists() = runTest {
        assertNull(dataSource.getWatermark("group-1", SyncCollectionName.EXPENSES))
    }

    @Test
    fun saveAndGetWatermark_preservesExactSecondsAndNanoseconds() = runTest {
        val watermark = SyncWatermark(seconds = 1_712_345_678L, nanoseconds = 987_654_321)

        dataSource.saveWatermark(
            groupId = "group-1",
            collectionName = SyncCollectionName.EXPENSES,
            watermark = watermark,
            syncedAtClientMillis = 1_712_345_679_000L
        )

        assertEquals(watermark, dataSource.getWatermark("group-1", SyncCollectionName.EXPENSES))
    }

    @Test
    fun saveWatermark_upsertsExistingGroupAndCollectionCheckpoint() = runTest {
        val initial = SyncWatermark(seconds = 100L, nanoseconds = 500)
        val updated = SyncWatermark(seconds = 200L, nanoseconds = 999_000_000)

        dataSource.saveWatermark("group-1", SyncCollectionName.CONTRIBUTIONS, initial, 1_000L)
        dataSource.saveWatermark("group-1", SyncCollectionName.CONTRIBUTIONS, updated, 2_000L)

        assertEquals(updated, dataSource.getWatermark("group-1", SyncCollectionName.CONTRIBUTIONS))
    }

    @Test
    fun saveWatermark_isolatesCheckpointsAcrossCollectionsAndGroups() = runTest {
        val w1 = SyncWatermark(seconds = 10L, nanoseconds = 1)
        val w2 = SyncWatermark(seconds = 20L, nanoseconds = 2)
        val w3 = SyncWatermark(seconds = 30L, nanoseconds = 3)

        dataSource.saveWatermark("group-1", SyncCollectionName.EXPENSES, w1, 100L)
        dataSource.saveWatermark("group-1", SyncCollectionName.SETTLEMENTS, w2, 200L)
        dataSource.saveWatermark("group-2", SyncCollectionName.EXPENSES, w3, 300L)

        assertEquals(w1, dataSource.getWatermark("group-1", SyncCollectionName.EXPENSES))
        assertEquals(w2, dataSource.getWatermark("group-1", SyncCollectionName.SETTLEMENTS))
        assertEquals(w3, dataSource.getWatermark("group-2", SyncCollectionName.EXPENSES))
    }

    @Test
    fun clearCheckpoint_deletesOnlyTargetCollection() = runTest {
        val w1 = SyncWatermark(seconds = 10L, nanoseconds = 1)
        val w2 = SyncWatermark(seconds = 20L, nanoseconds = 2)

        dataSource.saveWatermark("group-1", SyncCollectionName.EXPENSES, w1, 100L)
        dataSource.saveWatermark("group-1", SyncCollectionName.SUBUNITS, w2, 200L)

        dataSource.clearCheckpoint("group-1", SyncCollectionName.EXPENSES)

        assertNull(dataSource.getWatermark("group-1", SyncCollectionName.EXPENSES))
        assertEquals(w2, dataSource.getWatermark("group-1", SyncCollectionName.SUBUNITS))
    }

    @Test
    fun clearGroupCheckpoints_deletesAllCollectionsForGroup() = runTest {
        val w1 = SyncWatermark(seconds = 10L, nanoseconds = 1)
        val w2 = SyncWatermark(seconds = 20L, nanoseconds = 2)
        val w3 = SyncWatermark(seconds = 30L, nanoseconds = 3)

        dataSource.saveWatermark("group-1", SyncCollectionName.EXPENSES, w1, 100L)
        dataSource.saveWatermark("group-1", SyncCollectionName.CASH_WITHDRAWALS, w2, 200L)
        dataSource.saveWatermark("group-2", SyncCollectionName.EXPENSES, w3, 300L)

        dataSource.clearGroupCheckpoints("group-1")

        assertNull(dataSource.getWatermark("group-1", SyncCollectionName.EXPENSES))
        assertNull(dataSource.getWatermark("group-1", SyncCollectionName.CASH_WITHDRAWALS))
        assertEquals(w3, dataSource.getWatermark("group-2", SyncCollectionName.EXPENSES))
    }

    @Test
    fun deletingParentGroup_cascadesCheckpointDeletion() = runTest {
        val w1 = SyncWatermark(seconds = 10L, nanoseconds = 1)
        dataSource.saveWatermark("group-1", SyncCollectionName.CASH_TRANSFERS, w1, 100L)

        db.groupDao().deleteGroup("group-1")

        assertNull(dataSource.getWatermark("group-1", SyncCollectionName.CASH_TRANSFERS))
    }

    private fun createGroup(id: String) = GroupEntity(
        id = id,
        name = "Group $id",
        description = null,
        currencyCode = "EUR",
        extraCurrencies = emptyList(),
        memberIds = listOf("user-1"),
        mainImagePath = null,
        createdAtMillis = 1L,
        lastUpdatedAtMillis = 1L
    )
}
