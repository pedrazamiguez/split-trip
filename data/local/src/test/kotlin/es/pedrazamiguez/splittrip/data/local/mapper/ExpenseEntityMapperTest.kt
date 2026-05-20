package es.pedrazamiguez.splittrip.data.local.mapper

import es.pedrazamiguez.splittrip.data.local.entity.ExpenseEntity
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.enums.SyncStatus
import es.pedrazamiguez.splittrip.domain.model.Expense
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseEntityMapperTest {

    private val testTimestamp = LocalDateTime.of(2026, 3, 15, 10, 30, 0)
    private val testTimestampMillis = testTimestamp
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli()

    private val fullEntity = ExpenseEntity(
        id = "exp-1",
        groupId = "grp-1",
        title = "Lunch",
        sourceAmount = 5000L,
        sourceCurrency = "USD",
        groupAmount = 4500L,
        groupCurrency = "EUR",
        exchangeRate = "0.9",
        category = "FOOD",
        vendor = "Restaurant",
        notes = "Team lunch",
        paymentMethod = "CREDIT_CARD",
        paymentStatus = "FINISHED",
        dueDateMillis = testTimestampMillis,
        receiptLocalUri = "file://receipt.jpg",
        receiptMimeType = "image/webp",
        receiptCapturedAtMillis = testTimestampMillis,
        createdBy = "user-1",
        payerType = "USER",
        payerId = "user-payer",
        splitType = "EXACT",
        createdAtMillis = testTimestampMillis,
        lastUpdatedAtMillis = testTimestampMillis,
        cashTranchesJson = null,
        addOnsJson = null
    )

    @Nested
    inner class ToDomain {

        @Test
        fun `maps all core fields correctly`() {
            val expense = fullEntity.toDomain()

            assertEquals("exp-1", expense.id)
            assertEquals("grp-1", expense.groupId)
            assertEquals("Lunch", expense.title)
            assertEquals(5000L, expense.sourceAmount)
            assertEquals("USD", expense.sourceCurrency)
            assertEquals(4500L, expense.groupAmount)
            assertEquals("EUR", expense.groupCurrency)
            assertEquals(0, BigDecimal("0.9").compareTo(expense.exchangeRate))
        }

        @Test
        fun `maps enums correctly`() {
            val expense = fullEntity.toDomain()

            assertEquals(ExpenseCategory.FOOD, expense.category)
            assertEquals(PaymentMethod.CREDIT_CARD, expense.paymentMethod)
            assertEquals(PaymentStatus.FINISHED, expense.paymentStatus)
            assertEquals(SplitType.EXACT, expense.splitType)
            assertEquals(PayerType.USER, expense.payerType)
        }

        @Test
        fun `null category defaults to OTHER`() {
            val entity = fullEntity.copy(category = null)
            val expense = entity.toDomain()
            assertEquals(ExpenseCategory.OTHER, expense.category)
        }

        @Test
        fun `invalid category defaults to OTHER`() {
            val entity = fullEntity.copy(category = "NONEXISTENT")
            val expense = entity.toDomain()
            assertEquals(ExpenseCategory.OTHER, expense.category)
        }

        @Test
        fun `null paymentStatus defaults to FINISHED`() {
            val entity = fullEntity.copy(paymentStatus = null)
            val expense = entity.toDomain()
            assertEquals(PaymentStatus.FINISHED, expense.paymentStatus)
        }

        @Test
        fun `invalid exchangeRate defaults to ONE`() {
            val entity = fullEntity.copy(exchangeRate = "not-a-number")
            val expense = entity.toDomain()
            assertEquals(0, BigDecimal.ONE.compareTo(expense.exchangeRate))
        }

        @Test
        fun `invalid splitType defaults to EQUAL`() {
            val entity = fullEntity.copy(splitType = "NONEXISTENT")
            val expense = entity.toDomain()
            assertEquals(SplitType.EQUAL, expense.splitType)
        }

        @Test
        fun `null dueDateMillis maps to null dueDate`() {
            val entity = fullEntity.copy(dueDateMillis = null)
            val expense = entity.toDomain()
            assertNull(expense.dueDate)
        }

        @Test
        fun `maps timestamps correctly`() {
            val expense = fullEntity.toDomain()
            assertEquals(testTimestamp, expense.createdAt)
            assertEquals(testTimestamp, expense.lastUpdatedAt)
        }

        @Test
        fun `null timestamps map to null`() {
            val entity = fullEntity.copy(createdAtMillis = null, lastUpdatedAtMillis = null)
            val expense = entity.toDomain()
            assertNull(expense.createdAt)
            assertNull(expense.lastUpdatedAt)
        }

        @Test
        fun `null cashTranches and addOns default to empty lists`() {
            val expense = fullEntity.toDomain()
            assertTrue(expense.cashTranches.isEmpty())
            assertTrue(expense.addOns.isEmpty())
        }

        @Test
        fun `maps optional string fields`() {
            val expense = fullEntity.toDomain()
            assertEquals("Restaurant", expense.vendor)
            assertEquals("Team lunch", expense.notes)
            assertEquals("file://receipt.jpg", expense.receiptAttachment?.localUri)
            assertEquals("image/webp", expense.receiptAttachment?.mimeType)
        }

        @Test
        fun `maps receiptAttachment with null or blank mimeType using default image jpeg`() {
            val entityWithNullMime = fullEntity.copy(receiptMimeType = null)
            val expenseNull = entityWithNullMime.toDomain()
            assertNotNull(expenseNull.receiptAttachment)
            assertEquals("image/jpeg", expenseNull.receiptAttachment?.mimeType)

            val entityWithBlankMime = fullEntity.copy(receiptMimeType = "")
            val expenseBlank = entityWithBlankMime.toDomain()
            assertNotNull(expenseBlank.receiptAttachment)
            assertEquals("image/jpeg", expenseBlank.receiptAttachment?.mimeType)
        }

        @Test
        fun `maps payerId from entity to domain when non-null`() {
            val expense = fullEntity.toDomain()
            assertEquals("user-payer", expense.payerId)
        }

        @Test
        fun `maps null payerId from entity to domain`() {
            val entity = fullEntity.copy(payerId = null)
            val expense = entity.toDomain()
            assertNull(expense.payerId)
        }

        @Test
        fun `maps valid payerType string to PayerType enum`() {
            val entity = fullEntity.copy(payerType = "USER")
            val expense = entity.toDomain()
            assertEquals(PayerType.USER, expense.payerType)
        }

        @Test
        fun `preserves payerId when payerType is USER`() {
            val entity = fullEntity.copy(payerType = "USER", payerId = "user-payer")
            val expense = entity.toDomain()
            assertEquals("user-payer", expense.payerId)
        }

        @Test
        fun `falls back to GROUP when payerType is unknown string`() {
            val entity = fullEntity.copy(payerType = "NONEXISTENT")
            val expense = entity.toDomain()
            assertEquals(PayerType.GROUP, expense.payerType)
        }

        @Test
        fun `falls back to GROUP when payerType is not a valid PayerType entry`() {
            val entity = fullEntity.copy(payerType = "MEMBER")
            val expense = entity.toDomain()
            assertEquals(PayerType.GROUP, expense.payerType)
        }

        @Test
        fun `normalizes payerId to null when payerType falls back to GROUP`() {
            val entity = fullEntity.copy(payerType = "INVALID", payerId = "some-user")
            val expense = entity.toDomain()
            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `normalizes payerId to null when payerType is GROUP`() {
            val entity = fullEntity.copy(payerType = "GROUP", payerId = "stale-user")
            val expense = entity.toDomain()
            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `default syncStatus maps to SYNCED`() {
            val expense = fullEntity.toDomain()

            assertEquals(SyncStatus.SYNCED, expense.syncStatus)
        }

        @Test
        fun `PENDING_SYNC syncStatus maps correctly`() {
            val entity = fullEntity.copy(syncStatus = "PENDING_SYNC")

            assertEquals(SyncStatus.PENDING_SYNC, entity.toDomain().syncStatus)
        }

        @Test
        fun `SYNC_FAILED syncStatus maps correctly`() {
            val entity = fullEntity.copy(syncStatus = "SYNC_FAILED")

            assertEquals(SyncStatus.SYNC_FAILED, entity.toDomain().syncStatus)
        }
    }

    @Nested
    inner class ToEntity {

        private val fullExpense = Expense(
            id = "exp-1",
            groupId = "grp-1",
            title = "Dinner",
            sourceAmount = 8000L,
            sourceCurrency = "EUR",
            groupAmount = 8000L,
            groupCurrency = "EUR",
            exchangeRate = BigDecimal.ONE,
            category = ExpenseCategory.FOOD,
            vendor = "Bistro",
            notes = null,
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.SCHEDULED,
            dueDate = testTimestamp,
            receiptAttachment = null,
            cashTranches = emptyList(),
            addOns = emptyList(),
            splitType = SplitType.PERCENT,
            createdBy = "user-1",
            payerType = PayerType.USER,
            payerId = "user-payer",
            createdAt = testTimestamp,
            lastUpdatedAt = testTimestamp
        )

        @Test
        fun `maps all core fields correctly`() {
            val entity = fullExpense.toEntity()

            assertEquals("exp-1", entity.id)
            assertEquals("grp-1", entity.groupId)
            assertEquals("Dinner", entity.title)
            assertEquals(8000L, entity.sourceAmount)
            assertEquals("EUR", entity.sourceCurrency)
            assertEquals("1", entity.exchangeRate)
        }

        @Test
        fun `maps payerId from domain to entity when non-null`() {
            val entity = fullExpense.toEntity()
            assertEquals("user-payer", entity.payerId)
        }

        @Test
        fun `maps null payerId from domain to entity`() {
            val expense = fullExpense.copy(payerId = null)
            val entity = expense.toEntity()
            assertNull(entity.payerId)
        }

        @Test
        fun `maps enums to their name`() {
            val entity = fullExpense.toEntity()

            assertEquals("FOOD", entity.category)
            assertEquals("CASH", entity.paymentMethod)
            assertEquals("SCHEDULED", entity.paymentStatus)
            assertEquals("PERCENT", entity.splitType)
            assertEquals("USER", entity.payerType)
        }

        @Test
        fun `maps dueDate to millis`() {
            val entity = fullExpense.toEntity()
            assertEquals(testTimestampMillis, entity.dueDateMillis)
        }

        @Test
        fun `null createdAt uses system time`() {
            val expense = fullExpense.copy(createdAt = null)
            val entity = expense.toEntity()
            assertNotNull(entity.createdAtMillis)
            assertTrue(entity.createdAtMillis!! > 0)
        }

        @Test
        fun `null lastUpdatedAt falls back to createdAt millis`() {
            val expense = fullExpense.copy(lastUpdatedAt = null)
            val entity = expense.toEntity()
            assertEquals(entity.createdAtMillis, entity.lastUpdatedAtMillis)
        }
    }

    @Nested
    inner class ListExtensions {

        @Test
        fun `toDomain maps list of entities`() {
            val entities = listOf(fullEntity, fullEntity.copy(id = "exp-2"))
            val expenses = entities.toDomain()
            assertEquals(2, expenses.size)
            assertEquals("exp-1", expenses[0].id)
            assertEquals("exp-2", expenses[1].id)
        }

        @Test
        fun `toEntity maps list of expenses`() {
            val expense = fullEntity.toDomain()
            val entities = listOf(expense, expense.copy(id = "exp-2")).toEntity()
            assertEquals(2, entities.size)
            assertEquals("exp-1", entities[0].id)
            assertEquals("exp-2", entities[1].id)
        }

        @Test
        fun `maps syncStatus to entity string`() {
            val expense = fullEntity.toDomain().copy(syncStatus = SyncStatus.SYNC_FAILED)

            val entity = expense.toEntity()

            assertEquals("SYNC_FAILED", entity.syncStatus)
        }
    }
}
