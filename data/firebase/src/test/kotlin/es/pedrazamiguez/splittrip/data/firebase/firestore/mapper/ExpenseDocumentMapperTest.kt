package es.pedrazamiguez.splittrip.data.firebase.firestore.mapper

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AddOnDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.AttachmentDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument
import es.pedrazamiguez.splittrip.domain.enums.AddOnMode
import es.pedrazamiguez.splittrip.domain.enums.AddOnType
import es.pedrazamiguez.splittrip.domain.enums.AddOnValueType
import es.pedrazamiguez.splittrip.domain.enums.ExpenseCategory
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.enums.PaymentStatus
import es.pedrazamiguez.splittrip.domain.enums.SplitType
import es.pedrazamiguez.splittrip.domain.model.AddOn
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.model.ExpenseSplit
import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class ExpenseDocumentMapperTest {

    private val testExpenseId = "expense-123"
    private val testGroupId = "group-456"
    private val testUserId = "user-789"
    private val testGroupDocRef: DocumentReference = mockk(relaxed = true)
    private val testTimestamp = LocalDateTime.of(2026, 1, 15, 12, 30, 0)
    private val testFirebaseTimestamp = testTimestamp.toTimestampUtc()!!

    private val fullExpense = Expense(
        id = testExpenseId,
        groupId = testGroupId,
        title = "Dinner",
        sourceAmount = 5000L,
        sourceCurrency = "EUR",
        groupAmount = 6000L,
        groupCurrency = "USD",
        exchangeRate = BigDecimal("1.20"),
        category = ExpenseCategory.FOOD,
        vendor = "Restaurant XYZ",
        notes = "Birthday dinner",
        paymentMethod = PaymentMethod.CREDIT_CARD,
        paymentStatus = PaymentStatus.FINISHED,
        dueDate = testTimestamp,
        splitType = SplitType.EQUAL,
        splits = listOf(
            ExpenseSplit(userId = "user-1", amountCents = 3000L, percentage = BigDecimal("50.0")),
            ExpenseSplit(userId = "user-2", amountCents = 3000L, isExcluded = true)
        ),
        cashTranches = listOf(
            CashTranche(withdrawalId = "w-1", amountConsumed = 2000L),
            CashTranche(withdrawalId = "w-2", amountConsumed = 3000L)
        ),
        createdBy = testUserId,
        payerType = PayerType.GROUP,
        payerId = null,
        createdAt = testTimestamp,
        lastUpdatedAt = testTimestamp
    )

    @Nested
    inner class ToDocument {

        @Test
        fun `maps all core fields correctly`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals(testExpenseId, document.expenseId)
            assertEquals(testGroupId, document.groupId)
            assertEquals(testGroupDocRef, document.groupRef)
            assertEquals("Dinner", document.title)
            assertEquals(5000L, document.amountCents)
            assertEquals("EUR", document.currency)
            assertEquals("USD", document.groupCurrency)
            assertEquals(6000L, document.groupAmountCents)
            assertEquals("1.20", document.exchangeRate)
            assertEquals("Restaurant XYZ", document.vendor)
            assertEquals("Birthday dinner", document.notes)
        }

        @Test
        fun `maps payerType to document`() {
            val expense = fullExpense.copy(payerType = PayerType.USER)
            val document = expense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals("USER", document.payerType)
        }

        @Test
        fun `maps payerId to document when non-null`() {
            val expense = fullExpense.copy(payerType = PayerType.USER, payerId = "payer-123")
            val document = expense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals("payer-123", document.payerId)
        }

        @Test
        fun `maps null payerId to null in document`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals("GROUP", document.payerType)
            assertNull(document.payerId)
        }

        @Test
        fun `maps enum fields by name`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals("FOOD", document.expenseCategory)
            assertEquals("CREDIT_CARD", document.paymentMethod)
            assertEquals("FINISHED", document.paymentStatus)
            assertEquals("EQUAL", document.splitType)
        }

        @Test
        fun `maps createdAt and lastUpdatedAt when present`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertNotNull(document.createdAt)
            assertNotNull(document.lastUpdatedAt)
            assertEquals(testFirebaseTimestamp, document.createdAt)
            assertEquals(testFirebaseTimestamp, document.lastUpdatedAt)
        }

        @Test
        fun `createdAt and lastUpdatedAt are null when domain timestamps are null`() {
            val expenseWithoutTimestamps = fullExpense.copy(createdAt = null, lastUpdatedAt = null)

            val document = expenseWithoutTimestamps.toDocument(
                testExpenseId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertNull(document.createdAt)
            assertNull(document.lastUpdatedAt)
        }

        @Test
        fun `maps userId to createdBy and lastUpdatedBy`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals(testUserId, document.createdBy)
            assertEquals(testUserId, document.lastUpdatedBy)
        }

        @Test
        fun `maps dueDate correctly`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertNotNull(document.dueDate)
            assertEquals(testFirebaseTimestamp, document.dueDate)
        }

        @Test
        fun `maps null dueDate to null`() {
            val expenseNoDueDate = fullExpense.copy(dueDate = null)
            val document = expenseNoDueDate.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertNull(document.dueDate)
        }

        @Test
        fun `maps cashTranches to list of maps`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals(2, document.cashTranches.size)

            val first = document.cashTranches[0]
            assertEquals("w-1", first["withdrawalId"])
            assertEquals(2000L, first["amountConsumed"])

            val second = document.cashTranches[1]
            assertEquals("w-2", second["withdrawalId"])
            assertEquals(3000L, second["amountConsumed"])
        }

        @Test
        fun `maps empty cashTranches to empty list`() {
            val expenseNoCash = fullExpense.copy(cashTranches = emptyList())
            val document = expenseNoCash.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertTrue(document.cashTranches.isEmpty())
        }

        @Test
        fun `maps splits to split documents`() {
            val document = fullExpense.toDocument(testExpenseId, testGroupId, testGroupDocRef, testUserId)

            assertEquals(2, document.splits.size)
            assertEquals("user-1", document.splits[0].userId)
            assertEquals(3000L, document.splits[0].amountCents)
            assertEquals("50.0", document.splits[0].percentage)
            assertEquals("user-2", document.splits[1].userId)
            assertTrue(document.splits[1].isExcluded)
        }
    }

    @Nested
    inner class ToDomain {

        private val fullDocument = ExpenseDocument(
            expenseId = testExpenseId,
            groupId = testGroupId,
            title = "Dinner",
            expenseCategory = "FOOD",
            vendor = "Restaurant XYZ",
            notes = "Birthday dinner",
            amountCents = 5000L,
            currency = "EUR",
            groupCurrency = "USD",
            groupAmountCents = 6000L,
            exchangeRate = "1.20",
            paymentMethod = "CREDIT_CARD",
            paymentStatus = "FINISHED",
            dueDate = testFirebaseTimestamp,
            splitType = "EQUAL",
            splits = listOf(
                ExpenseSplitDocument(userId = "user-1", amountCents = 3000L, percentage = "50"),
                ExpenseSplitDocument(userId = "user-2", amountCents = 3000L, isExcluded = true)
            ),
            cashTranches = listOf(
                mapOf("withdrawalId" to "w-1", "amountConsumed" to 2000L),
                mapOf("withdrawalId" to "w-2", "amountConsumed" to 3000L)
            ),
            createdBy = testUserId,
            payerType = "GROUP",
            payerId = null,
            createdAt = testFirebaseTimestamp,
            lastUpdatedAt = testFirebaseTimestamp
        )

        @Test
        fun `maps all core fields correctly`() {
            val expense = fullDocument.toDomain()

            assertEquals(testExpenseId, expense.id)
            assertEquals(testGroupId, expense.groupId)
            assertEquals("Dinner", expense.title)
            assertEquals(5000L, expense.sourceAmount)
            assertEquals("EUR", expense.sourceCurrency)
            assertEquals(6000L, expense.groupAmount)
            assertEquals("USD", expense.groupCurrency)
            assertEquals(0, BigDecimal("1.20").compareTo(expense.exchangeRate))
            assertEquals("Restaurant XYZ", expense.vendor)
            assertEquals("Birthday dinner", expense.notes)
            assertEquals(testUserId, expense.createdBy)
            assertEquals(PayerType.GROUP, expense.payerType)
        }

        @Test
        fun `maps payerId from document to domain when non-null`() {
            val document = fullDocument.copy(payerType = "USER", payerId = "payer-456")
            val expense = document.toDomain()

            assertEquals(PayerType.USER, expense.payerType)
            assertEquals("payer-456", expense.payerId)
        }

        @Test
        fun `maps null payerId from document to domain`() {
            val expense = fullDocument.toDomain()

            assertNull(expense.payerId)
        }

        @Test
        fun `falls back to GROUP when payerType is unknown string`() {
            val document = fullDocument.copy(payerType = "INVALID_VALUE")
            val expense = document.toDomain()

            assertEquals(PayerType.GROUP, expense.payerType)
        }

        @Test
        fun `normalizes payerId to null when payerType falls back to GROUP`() {
            val document = fullDocument.copy(payerType = "INVALID_VALUE", payerId = "some-user")
            val expense = document.toDomain()

            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `normalizes payerId to null when payerType is GROUP`() {
            val document = fullDocument.copy(payerType = "GROUP", payerId = "stale-user")
            val expense = document.toDomain()

            assertEquals(PayerType.GROUP, expense.payerType)
            assertNull(expense.payerId)
        }

        @Test
        fun `maps enum fields correctly`() {
            val expense = fullDocument.toDomain()

            assertEquals(ExpenseCategory.FOOD, expense.category)
            assertEquals(PaymentMethod.CREDIT_CARD, expense.paymentMethod)
            assertEquals(PaymentStatus.FINISHED, expense.paymentStatus)
            assertEquals(SplitType.EQUAL, expense.splitType)
        }

        @Test
        fun `falls back to default enums for invalid strings`() {
            val documentWithBadEnums = fullDocument.copy(
                expenseCategory = "INVALID_CATEGORY",
                paymentMethod = "INVALID_METHOD",
                paymentStatus = "INVALID_STATUS",
                splitType = "INVALID_SPLIT"
            )

            val expense = documentWithBadEnums.toDomain()

            assertEquals(ExpenseCategory.OTHER, expense.category)
            assertEquals(PaymentMethod.OTHER, expense.paymentMethod)
            assertEquals(PaymentStatus.FINISHED, expense.paymentStatus)
            assertEquals(SplitType.EQUAL, expense.splitType)
        }

        @Test
        fun `maps timestamps correctly`() {
            val expense = fullDocument.toDomain()

            assertEquals(testTimestamp, expense.createdAt)
            assertEquals(testTimestamp, expense.lastUpdatedAt)
            assertEquals(testTimestamp, expense.dueDate)
        }

        @Test
        fun `null timestamps map to null domain fields`() {
            val documentWithNullTimestamps = fullDocument.copy(
                createdAt = null,
                lastUpdatedAt = null,
                dueDate = null
            )

            val expense = documentWithNullTimestamps.toDomain()

            assertNull(expense.createdAt)
            assertNull(expense.lastUpdatedAt)
            assertNull(expense.dueDate)
        }

        @Test
        fun `uses amountCents as fallback when groupAmountCents is null`() {
            val documentNoGroupAmount = fullDocument.copy(groupAmountCents = null)

            val expense = documentNoGroupAmount.toDomain()

            assertEquals(fullDocument.amountCents, expense.groupAmount)
        }

        @Test
        fun `uses BigDecimal ONE as fallback when exchangeRate is null`() {
            val documentNoRate = fullDocument.copy(exchangeRate = null)

            val expense = documentNoRate.toDomain()

            assertEquals(0, BigDecimal.ONE.compareTo(expense.exchangeRate))
        }

        @Test
        fun `uses BigDecimal ONE as fallback when exchangeRate is non-numeric`() {
            val documentBadRate = fullDocument.copy(exchangeRate = "not-a-number")

            val expense = documentBadRate.toDomain()

            assertEquals(0, BigDecimal.ONE.compareTo(expense.exchangeRate))
        }

        @Test
        fun `maps cashTranches correctly`() {
            val expense = fullDocument.toDomain()

            assertEquals(2, expense.cashTranches.size)
            assertEquals("w-1", expense.cashTranches[0].withdrawalId)
            assertEquals(2000L, expense.cashTranches[0].amountConsumed)
            assertEquals("w-2", expense.cashTranches[1].withdrawalId)
            assertEquals(3000L, expense.cashTranches[1].amountConsumed)
        }

        @Test
        fun `filters out malformed cashTranches entries`() {
            val documentWithBadTranches = fullDocument.copy(
                cashTranches = listOf(
                    mapOf("withdrawalId" to "w-1", "amountConsumed" to 2000L),
                    mapOf("withdrawalId" to "w-2"), // Missing amountConsumed
                    mapOf("amountConsumed" to 3000L), // Missing withdrawalId
                    emptyMap()
                )
            )

            val expense = documentWithBadTranches.toDomain()

            assertEquals(1, expense.cashTranches.size)
            assertEquals("w-1", expense.cashTranches[0].withdrawalId)
        }

        @Test
        fun `maps splits correctly`() {
            val expense = fullDocument.toDomain()

            assertEquals(2, expense.splits.size)
            assertEquals("user-1", expense.splits[0].userId)
            assertEquals(3000L, expense.splits[0].amountCents)
            assertEquals(0, BigDecimal("50.0").compareTo(expense.splits[0].percentage))
            assertEquals("user-2", expense.splits[1].userId)
            assertTrue(expense.splits[1].isExcluded)
        }

        @Test
        fun `maps addOns from document to domain`() {
            val addOnDoc = AddOnDocument(
                id = "addon-1",
                type = "FEE",
                mode = "ON_TOP",
                valueType = "EXACT",
                amountCents = 500L,
                currency = "EUR",
                exchangeRate = "1.2",
                groupAmountCents = 600L,
                paymentMethod = "CREDIT_CARD",
                description = "Processing fee"
            )
            val documentWithAddOns = fullDocument.copy(addOns = listOf(addOnDoc))

            val expense = documentWithAddOns.toDomain()

            assertEquals(1, expense.addOns.size)
            val addOn = expense.addOns[0]
            assertEquals("addon-1", addOn.id)
            assertEquals(AddOnType.FEE, addOn.type)
            assertEquals(AddOnMode.ON_TOP, addOn.mode)
            assertEquals(AddOnValueType.EXACT, addOn.valueType)
            assertEquals(500L, addOn.amountCents)
            assertEquals("EUR", addOn.currency)
            assertEquals(0, BigDecimal("1.2").compareTo(addOn.exchangeRate))
            assertEquals("Processing fee", addOn.description)
        }

        @Test
        fun `uses BigDecimal ONE as addOn exchangeRate fallback when null`() {
            val addOnDoc = AddOnDocument(
                id = "addon-null-rate",
                type = "FEE",
                mode = "ON_TOP",
                valueType = "EXACT",
                amountCents = 100L,
                currency = "EUR",
                exchangeRate = null,
                groupAmountCents = 100L,
                paymentMethod = "CASH"
            )
            val documentWithAddOn = fullDocument.copy(addOns = listOf(addOnDoc))

            val expense = documentWithAddOn.toDomain()

            assertEquals(0, BigDecimal.ONE.compareTo(expense.addOns[0].exchangeRate))
        }

        @Test
        fun `maps receiptAttachment when remote URL is present`() {
            val capturedAtMillis = 1_700_000_000_000L
            val attachmentDoc = AttachmentDocument(
                path = "https://storage.example.com/receipts/photo.jpg",
                mime = "image/jpeg",
                uploadedAt = Timestamp(java.util.Date(capturedAtMillis))
            )
            val documentWithAttachment = fullDocument.copy(attachments = listOf(attachmentDoc))

            val expense = documentWithAttachment.toDomain()

            assertNotNull(expense.receiptAttachment)
            assertEquals(
                "https://storage.example.com/receipts/photo.jpg",
                expense.receiptAttachment!!.remoteUrl
            )
            assertEquals("image/jpeg", expense.receiptAttachment!!.mimeType)
            // localUri is intentionally blank — file does not exist on this device
            assertEquals("", expense.receiptAttachment!!.localUri)
        }

        @Test
        fun `maps null receiptAttachment when attachments list is empty`() {
            val documentNoAttachments = fullDocument.copy(attachments = emptyList())

            val expense = documentNoAttachments.toDomain()

            assertNull(expense.receiptAttachment)
        }

        @Test
        fun `maps null receiptAttachment when attachment has empty path`() {
            val attachmentDoc = AttachmentDocument(
                path = "",
                mime = "image/jpeg",
                uploadedAt = null
            )
            val documentWithBlankPath = fullDocument.copy(attachments = listOf(attachmentDoc))

            val expense = documentWithBlankPath.toDomain()

            assertNull(expense.receiptAttachment)
        }
    }

    @Nested
    inner class ToDocumentAddOns {

        private val sampleAddOn = AddOn(
            id = "addon-1",
            type = AddOnType.FEE,
            mode = AddOnMode.ON_TOP,
            valueType = AddOnValueType.EXACT,
            amountCents = 500L,
            currency = "EUR",
            exchangeRate = BigDecimal("1.2"),
            groupAmountCents = 600L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            description = "Processing fee"
        )

        @Test
        fun `maps expense addOns to document`() {
            val expenseWithAddOn = fullExpense.copy(addOns = listOf(sampleAddOn))

            val document = expenseWithAddOn.toDocument(
                testExpenseId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(1, document.addOns.size)
            val addOnDoc = document.addOns[0]
            assertEquals("addon-1", addOnDoc.id)
            assertEquals("FEE", addOnDoc.type)
            assertEquals("ON_TOP", addOnDoc.mode)
            assertEquals("EXACT", addOnDoc.valueType)
            assertEquals("1.2", addOnDoc.exchangeRate)
        }

        @Test
        fun `maps receiptAttachment with remote URL to attachment document`() {
            val attachment = ReceiptAttachment(
                localUri = "content://media/photo/1",
                mimeType = "image/jpeg",
                capturedAtMillis = 1_700_000_000_000L,
                remoteUrl = "https://storage.example.com/receipts/photo.jpg"
            )
            val expenseWithAttachment = fullExpense.copy(receiptAttachment = attachment)

            val document = expenseWithAttachment.toDocument(
                testExpenseId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertEquals(1, document.attachments.size)
            assertEquals("https://storage.example.com/receipts/photo.jpg", document.attachments[0].path)
            assertEquals("image/jpeg", document.attachments[0].mime)
        }

        @Test
        fun `does not include attachment when remoteUrl is null`() {
            val attachment = ReceiptAttachment(
                localUri = "content://media/photo/1",
                mimeType = "image/jpeg",
                capturedAtMillis = 1_700_000_000_000L,
                remoteUrl = null
            )
            val expenseWithLocalAttachment = fullExpense.copy(receiptAttachment = attachment)

            val document = expenseWithLocalAttachment.toDocument(
                testExpenseId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertTrue(document.attachments.isEmpty())
        }

        @Test
        fun `does not include attachment when receiptAttachment is null`() {
            val expenseNoAttachment = fullExpense.copy(receiptAttachment = null)

            val document = expenseNoAttachment.toDocument(
                testExpenseId,
                testGroupId,
                testGroupDocRef,
                testUserId
            )

            assertTrue(document.attachments.isEmpty())
        }
    }
}
