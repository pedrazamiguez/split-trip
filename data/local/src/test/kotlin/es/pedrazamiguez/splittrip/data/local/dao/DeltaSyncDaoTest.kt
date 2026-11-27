package es.pedrazamiguez.splittrip.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.entity.CashTransferEntity
import es.pedrazamiguez.splittrip.data.local.entity.CashWithdrawalEntity
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.data.local.entity.SubunitEntity
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import java.math.BigDecimal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeltaSyncDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var cashWithdrawalDao: CashWithdrawalDao
    private lateinit var cashTransferDao: CashTransferDao
    private lateinit var subunitDao: SubunitDao

    private val groupId = "group-1"

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        cashWithdrawalDao = db.cashWithdrawalDao()
        cashTransferDao = db.cashTransferDao()
        subunitDao = db.subunitDao()

        db.groupDao().insertGroup(
            GroupEntity(
                id = groupId,
                name = "Test Group",
                description = null,
                currencyCode = "EUR",
                extraCurrencies = emptyList(),
                memberIds = listOf("u1", "u2"),
                mainImagePath = null,
                createdAtMillis = 1L,
                lastUpdatedAtMillis = 1L
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun applyWithdrawalsDeltaForGroup_upsertsPreservesUnsyncedDeletesTombstonesAndKeepsUntouched() = runTest {
        val untouched = withdrawal("w-untouched", 1000L, "SYNCED")
        val pending = withdrawal("w-pending", 2000L, "PENDING_SYNC")
        val toDelete = withdrawal("w-delete", 3000L, "SYNCED")
        cashWithdrawalDao.insertWithdrawals(listOf(untouched, pending, toDelete))

        val remotePending = withdrawal("w-pending", 2000L, "SYNCED")
        val remoteNew = withdrawal("w-new", 4000L, "SYNCED")

        cashWithdrawalDao.applyWithdrawalsDeltaForGroup(
            groupId = groupId,
            upserts = listOf(remotePending, remoteNew),
            deletedIds = listOf("w-delete")
        )

        assertNotNull(cashWithdrawalDao.getWithdrawalById("w-untouched"))
        assertEquals("PENDING_SYNC", cashWithdrawalDao.getWithdrawalById("w-pending")?.syncStatus)
        assertNull(cashWithdrawalDao.getWithdrawalById("w-delete"))
        assertEquals(4000L, cashWithdrawalDao.getWithdrawalById("w-new")?.amountWithdrawn)
    }

    @Test
    fun applyCashTransfersDeltaForGroup_upsertsPreservesUnsyncedDeletesTombstonesAndKeepsUntouched() = runTest {
        val untouched = transfer("t-untouched", 1000L, SyncStatus.SYNCED)
        val pending = transfer("t-pending", 2000L, SyncStatus.PENDING_SYNC)
        val toDelete = transfer("t-delete", 3000L, SyncStatus.SYNCED)
        cashTransferDao.insertAll(listOf(untouched, pending, toDelete))

        val remotePending = transfer("t-pending", 2000L, SyncStatus.SYNCED)
        val remoteNew = transfer("t-new", 4000L, SyncStatus.SYNCED)

        cashTransferDao.applyCashTransfersDeltaForGroup(
            groupId = groupId,
            upserts = listOf(remotePending, remoteNew),
            deletedIds = listOf("t-delete")
        )

        assertNotNull(cashTransferDao.getCashTransferById("t-untouched"))
        assertEquals(SyncStatus.PENDING_SYNC, cashTransferDao.getCashTransferById("t-pending")?.syncStatus)
        assertNull(cashTransferDao.getCashTransferById("t-delete"))
        assertEquals(4000L, cashTransferDao.getCashTransferById("t-new")?.amountCents)
    }

    @Test
    fun applySubunitsDeltaForGroup_upsertsPreservesUnsyncedDeletesTombstonesAndKeepsUntouched() = runTest {
        val untouched = subunit("s-untouched", "Alpha", "SYNCED")
        val pending = subunit("s-pending", "Beta", "PENDING_SYNC")
        val toDelete = subunit("s-delete", "Gamma", "SYNCED")
        subunitDao.insertSubunits(listOf(untouched, pending, toDelete))

        val remotePending = subunit("s-pending", "Beta", "SYNCED")
        val remoteNew = subunit("s-new", "Delta", "SYNCED")

        subunitDao.applySubunitsDeltaForGroup(
            groupId = groupId,
            upserts = listOf(remotePending, remoteNew),
            deletedIds = listOf("s-delete")
        )

        val all = subunitDao.getSubunitsByGroupIdFlow(groupId).first()
        assertEquals(3, all.size)
        assertNotNull(subunitDao.getSubunitById("s-untouched"))
        assertEquals("PENDING_SYNC", subunitDao.getSubunitById("s-pending")?.syncStatus)
        assertNull(subunitDao.getSubunitById("s-delete"))
        assertEquals("Delta", subunitDao.getSubunitById("s-new")?.name)
    }

    private fun withdrawal(id: String, amount: Long, syncStatus: String) = CashWithdrawalEntity(
        id = id,
        groupId = groupId,
        withdrawnBy = "u1",
        createdBy = "u1",
        amountWithdrawn = amount,
        remainingAmount = amount,
        currency = "EUR",
        deductedBaseAmount = amount,
        exchangeRate = "1.0",
        createdAtMillis = 100L,
        lastUpdatedAtMillis = 100L,
        syncStatus = syncStatus
    )

    private fun transfer(id: String, amount: Long, syncStatus: SyncStatus) = CashTransferEntity(
        id = id,
        groupId = groupId,
        fromUserId = "u1",
        toUserId = "u2",
        amountCents = amount,
        currency = "EUR",
        equivalentBaseAmountCents = amount,
        createdAt = 100L,
        syncStatus = syncStatus
    )

    private fun subunit(id: String, name: String, syncStatus: String) = SubunitEntity(
        id = id,
        groupId = groupId,
        name = name,
        memberIds = listOf("u1", "u2"),
        memberShares = mapOf("u1" to BigDecimal("0.5"), "u2" to BigDecimal("0.5")),
        createdBy = "u1",
        createdAtMillis = 100L,
        lastUpdatedAtMillis = 100L,
        syncStatus = syncStatus
    )
}
