package es.pedrazamiguez.splittrip.data.local.datasource

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseDao
import es.pedrazamiguez.splittrip.data.local.dao.ExpenseSplitDao
import es.pedrazamiguez.splittrip.data.local.dao.GroupDao
import es.pedrazamiguez.splittrip.data.local.database.AppDatabase
import es.pedrazamiguez.splittrip.data.local.datasource.impl.LocalExpenseDataSourceImpl
import es.pedrazamiguez.splittrip.data.local.entity.GroupEntity
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
@OptIn(ExperimentalCoroutinesApi::class)
class LocalExpenseDataSourceImplTest {
    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var expenseSplitDao: ExpenseSplitDao
    private lateinit var groupDao: GroupDao
    private lateinit var localDataSource: LocalExpenseDataSourceImpl

    private val testGroupId = "group-123"
    private val testExpense1 = Expense(
        id = "expense-1",
        groupId = testGroupId,
        title = "Dinner",
        sourceAmount = 5000L, // 50.00
        sourceCurrency = "EUR",
        groupAmount = 5500L, // 55.00
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        createdBy = "user-1",
        payerType = PayerType.GROUP,
        createdAt = LocalDateTime.of(2024, 1, 15, 12, 30),
        lastUpdatedAt = LocalDateTime.of(2024, 1, 15, 12, 30)
    )

    private val testExpense2 = Expense(
        id = "expense-2",
        groupId = testGroupId,
        title = "Taxi",
        sourceAmount = 2000L, // 20.00
        sourceCurrency = "EUR",
        groupAmount = 2100L, // 21.00
        groupCurrency = "EUR",
        exchangeRate = BigDecimal.ONE,
        paymentMethod = PaymentMethod.CASH,
        createdBy = "user-2",
        payerType = PayerType.GROUP,
        createdAt = LocalDateTime.of(2024, 1, 16, 10, 0),
        lastUpdatedAt = LocalDateTime.of(2024, 1, 16, 10, 0)
    )

    private val testExpense3 = Expense(
        id = "expense-3",
        groupId = "group-456",
        title = "Hotel",
        sourceAmount = 15000L, // 150.00
        sourceCurrency = "USD",
        groupAmount = 13500L, // 135.00 EUR
        groupCurrency = "EUR",
        exchangeRate = BigDecimal("0.9"),
        paymentMethod = PaymentMethod.DEBIT_CARD,
        createdBy = "user-1",
        payerType = PayerType.GROUP,
        createdAt = LocalDateTime.of(2024, 1, 17, 14, 20),
        lastUpdatedAt = LocalDateTime.of(2024, 1, 17, 14, 20)
    )

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries() // okay for tests
            .build()
        expenseDao = db.expenseDao()
        expenseSplitDao = db.expenseSplitDao()
        groupDao = db.groupDao()
        localDataSource = LocalExpenseDataSourceImpl(db, expenseDao, expenseSplitDao)

        // Create parent groups to satisfy foreign key constraints
        groupDao.insertGroups(
            listOf(
                GroupEntity(
                    id = testGroupId,
                    name = "Test Group 123",
                    description = "Test group",
                    currencyCode = "EUR",
                    extraCurrencies = emptyList(),
                    memberIds = listOf("user-1", "user-2"),
                    mainImagePath = null,
                    createdAtMillis = System.currentTimeMillis(),
                    lastUpdatedAtMillis = System.currentTimeMillis()
                ),
                GroupEntity(
                    id = "group-456",
                    name = "Test Group 456",
                    description = "Another test group",
                    currencyCode = "EUR",
                    extraCurrencies = emptyList(),
                    memberIds = listOf("user-1"),
                    mainImagePath = null,
                    createdAtMillis = System.currentTimeMillis(),
                    lastUpdatedAtMillis = System.currentTimeMillis()
                )
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun saveAndGetExpenseById() = runTest {
        // When
        localDataSource.saveExpense(testExpense1)

        // Then
        val result = localDataSource.getExpenseById("expense-1")
        assertNotNull(result)
        assertEquals("expense-1", result?.id)
        assertEquals("Dinner", result?.title)
        assertEquals(5000L, result?.sourceAmount)
        assertEquals(PaymentMethod.CREDIT_CARD, result?.paymentMethod)
    }

    @Test
    fun saveAndGetExpenseById_preservesSplitOrder() = runTest {
        val expenseWithSplits = testExpense1.copy(
            id = "expense-with-splits",
            splits = listOf(
                ExpenseSplit(userId = "user-2", amountCents = 1500L),
                ExpenseSplit(userId = "user-1", amountCents = 3500L),
                ExpenseSplit(userId = "user-3", amountCents = 0L, isExcluded = true)
            )
        )

        localDataSource.saveExpense(expenseWithSplits)

        val result = localDataSource.getExpenseById("expense-with-splits")

        assertNotNull(result)
        assertEquals(listOf("user-2", "user-1", "user-3"), result?.splits?.map { it.userId })
    }

    @Test
    fun getExpenseById_returnsNull_whenNotFound() = runTest {
        // When
        val result = localDataSource.getExpenseById("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun saveMultipleExpenses_andGetByGroupId() = runTest {
        // When
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2))

        // Then
        val result = localDataSource.getExpensesByGroupIdFlow(testGroupId).first()
        assertEquals(2, result.size)
        assertEquals("expense-2", result[0].id) // Most recent first (DESC order)
        assertEquals("expense-1", result[1].id)
    }

    @Test
    fun getExpensesByGroupIdFlow_returnsOnlyMatchingGroup() = runTest {
        // Given
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2, testExpense3))

        // When
        val resultGroup123 = localDataSource.getExpensesByGroupIdFlow(testGroupId).first()
        val resultGroup456 = localDataSource.getExpensesByGroupIdFlow("group-456").first()

        // Then
        assertEquals(2, resultGroup123.size)
        assertEquals("expense-2", resultGroup123[0].id)
        assertEquals("expense-1", resultGroup123[1].id)

        assertEquals(1, resultGroup456.size)
        assertEquals("expense-3", resultGroup456[0].id)
    }

    @Test
    fun getExpensesByGroupIdFlow_returnsEmptyList_whenNoExpenses() = runTest {
        // When
        val result = localDataSource.getExpensesByGroupIdFlow("non-existent-group").first()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getExpensesByGroupIdFlow_orderedByCreatedAtDesc() = runTest {
        // Given - Create expenses with different timestamps
        val expense1Old = testExpense1.copy(
            id = "expense-old",
            createdAt = LocalDateTime.of(2024, 1, 10, 10, 0)
        )
        val expense2New = testExpense2.copy(
            id = "expense-new",
            createdAt = LocalDateTime.of(2024, 1, 20, 10, 0)
        )
        val expense3Mid = testExpense1.copy(
            id = "expense-mid",
            createdAt = LocalDateTime.of(2024, 1, 15, 10, 0)
        )

        // When
        localDataSource.saveExpenses(listOf(expense1Old, expense2New, expense3Mid))
        val result = localDataSource.getExpensesByGroupIdFlow(testGroupId).first()

        // Then - Should be ordered newest to oldest
        assertEquals(3, result.size)
        assertEquals("expense-new", result[0].id)
        assertEquals("expense-mid", result[1].id)
        assertEquals("expense-old", result[2].id)
    }

    @Test
    fun saveExpense_replacesExisting_onConflict() = runTest {
        // Given - Save initial expense
        localDataSource.saveExpense(testExpense1)

        // When - Save updated version
        val updatedExpense = testExpense1.copy(
            title = "Updated Dinner",
            sourceAmount = 6000L
        )
        localDataSource.saveExpense(updatedExpense)

        // Then
        val result = localDataSource.getExpenseById("expense-1")
        assertNotNull(result)
        assertEquals("Updated Dinner", result?.title)
        assertEquals(6000L, result?.sourceAmount)
    }

    @Test
    fun deleteExpense_removesExpense() = runTest {
        // Given
        localDataSource.saveExpense(testExpense1)

        // When
        localDataSource.deleteExpense("expense-1")

        // Then
        val result = localDataSource.getExpenseById("expense-1")
        assertNull(result)
    }

    @Test
    fun deleteExpensesByGroupId_removesAllGroupExpenses() = runTest {
        // Given
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2, testExpense3))

        // When
        localDataSource.deleteExpensesByGroupId(testGroupId)

        // Then
        val resultGroup123 = localDataSource.getExpensesByGroupIdFlow(testGroupId).first()
        val resultGroup456 = localDataSource.getExpensesByGroupIdFlow("group-456").first()

        assertEquals(0, resultGroup123.size)
        assertEquals(1, resultGroup456.size) // Other group unaffected
    }

    @Test
    fun clearAllExpenses_removesAllExpenses() = runTest {
        // Given
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2, testExpense3))

        // When
        localDataSource.clearAllExpenses()

        // Then
        val resultGroup123 = localDataSource.getExpensesByGroupIdFlow(testGroupId).first()
        val resultGroup456 = localDataSource.getExpensesByGroupIdFlow("group-456").first()

        assertEquals(0, resultGroup123.size)
        assertEquals(0, resultGroup456.size)
    }

    @Test
    fun getExpenseIdsByGroup_returnsOnlyIdsForSpecificGroup() = runTest {
        // Given
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2, testExpense3))

        // When
        val result = localDataSource.getExpenseIdsByGroup(testGroupId)

        // Then - Should return only IDs for the specified group
        assertEquals(2, result.size)
        assertEquals(true, result.contains("expense-1"))
        assertEquals(true, result.contains("expense-2"))
        assertEquals(false, result.contains("expense-3")) // Different group
    }

    @Test
    fun getExpenseIdsByGroup_returnsEmptyListForNonExistentGroup() = runTest {
        // Given
        localDataSource.saveExpenses(listOf(testExpense1, testExpense2))

        // When
        val result = localDataSource.getExpenseIdsByGroup("non-existent-group")

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getExpenseIdsByGroup_returnsEmptyListForEmptyDatabase() = runTest {
        // When
        val result = localDataSource.getExpenseIdsByGroup(testGroupId)

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun saveExpense_withNullTimestamps_generatesTimestamps() = runTest {
        // Given - Expense without timestamps (as created in UI)
        val expenseWithoutTimestamps = testExpense1.copy(
            id = "expense-no-timestamps",
            createdAt = null,
            lastUpdatedAt = null
        )

        // When
        localDataSource.saveExpense(expenseWithoutTimestamps)

        // Then
        val result = localDataSource.getExpenseById("expense-no-timestamps")
        assertNotNull(result)
        assertNotNull("createdAt should be auto-generated", result?.createdAt)
        assertNotNull("lastUpdatedAt should be auto-generated", result?.lastUpdatedAt)
    }

    @Test
    fun mapper_preservesAllFields() = runTest {
        // When
        localDataSource.saveExpense(testExpense1)
        val result = localDataSource.getExpenseById("expense-1")

        // Then - Verify all fields are preserved
        assertNotNull(result)
        assertEquals(testExpense1.id, result?.id)
        assertEquals(testExpense1.groupId, result?.groupId)
        assertEquals(testExpense1.title, result?.title)
        assertEquals(testExpense1.sourceAmount, result?.sourceAmount)
        assertEquals(testExpense1.sourceCurrency, result?.sourceCurrency)
        assertEquals(testExpense1.groupAmount, result?.groupAmount)
        assertEquals(testExpense1.groupCurrency, result?.groupCurrency)
        assertNotNull(testExpense1.exchangeRate)
        assertNotNull(result?.exchangeRate)
        assertEquals(0, testExpense1.exchangeRate.compareTo(result!!.exchangeRate))
        assertEquals(testExpense1.paymentMethod, result.paymentMethod)
        assertEquals(testExpense1.createdBy, result.createdBy)
        assertEquals(testExpense1.payerType, result.payerType)
        assertEquals(testExpense1.createdAt, result.createdAt)
        assertEquals(testExpense1.lastUpdatedAt, result.lastUpdatedAt)
    }
}
