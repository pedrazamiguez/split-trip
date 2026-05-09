package es.pedrazamiguez.splittrip.data.local.datasource

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.dao.ContributionDao
import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalContributionDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.model.Contribution
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class LocalContributionDataSourceImplTest {
    private lateinit var db: AppDatabase
    private lateinit var contributionDao: ContributionDao
    private lateinit var groupDao: GroupDao
    private lateinit var localDataSource: LocalContributionDataSourceImpl

    private val testGroupId = "group-123"

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()
        contributionDao = db.contributionDao()
        groupDao = db.groupDao()
        localDataSource = LocalContributionDataSourceImpl(contributionDao)

        groupDao.insertGroup(
            GroupEntity(
                id = testGroupId,
                name = "Test Group",
                description = null,
                currencyCode = "EUR",
                extraCurrencies = emptyList(),
                memberIds = listOf("user-1", "user-2"),
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
    fun findByLinkedExpenseId_returnsMostRecentlyUpdatedContribution_whenDuplicatesExist() = runTest {
        val olderContribution = Contribution(
            id = "contribution-old",
            groupId = testGroupId,
            userId = "user-1",
            createdBy = "user-1",
            contributionScope = PayerType.USER,
            amount = 1000L,
            currency = "EUR",
            linkedExpenseId = "expense-123",
            createdAt = LocalDateTime.of(2024, 1, 10, 10, 0),
            lastUpdatedAt = LocalDateTime.of(2024, 1, 10, 10, 0)
        )
        val newerContribution = olderContribution.copy(
            id = "contribution-new",
            amount = 2000L,
            createdAt = LocalDateTime.of(2024, 1, 11, 10, 0),
            lastUpdatedAt = LocalDateTime.of(2024, 1, 11, 10, 0)
        )

        localDataSource.saveContribution(olderContribution)
        localDataSource.saveContribution(newerContribution)

        val result = localDataSource.findByLinkedExpenseId(testGroupId, "expense-123")

        assertNotNull(result)
        assertEquals("contribution-new", result?.id)
        assertEquals(2000L, result?.amount)
    }
}
