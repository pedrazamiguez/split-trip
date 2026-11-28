package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.WriteBatch
import es.pedrazamiguez.splittrip.core.performance.PerformanceMonitor
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashTransferDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.SubunitDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.domain.enums.PayerType
import es.pedrazamiguez.splittrip.domain.enums.PaymentMethod
import es.pedrazamiguez.splittrip.domain.model.CashTranche
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FirestoreSubcollectionWriteSyncTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var authService: AuthenticationService
    private lateinit var performanceMonitor: PerformanceMonitor

    private val groupId = "group-1"
    private val userId = "user-1"

    @BeforeEach
    fun setUp() {
        firestore = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        performanceMonitor = mockk(relaxed = true) {
            every { trace<Any?>(any(), any()) } answers { secondArg<() -> Any?>().invoke() }
            coEvery { traceAsync<Any?>(any(), any()) } coAnswers { secondArg<suspend () -> Any?>().invoke() }
        }
        every { authService.currentUserId() } returns userId
        every { authService.requireUserId() } returns userId
    }

    private fun mockSubcollectionDocRef(subcollection: String, docId: String): DocumentReference {
        val groupsCol = mockk<CollectionReference>(relaxed = true)
        val groupDoc = mockk<DocumentReference>(relaxed = true)
        val subCol = mockk<CollectionReference>(relaxed = true)
        val docRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection(GroupDocument.COLLECTION_PATH) } returns groupsCol
        every { groupsCol.document(groupId) } returns groupDoc
        every { groupDoc.collection(subcollection) } returns subCol
        every { subCol.document(docId) } returns docRef
        every { docRef.id } returns docId
        return docRef
    }

    @Test
    fun `deleteExpense writes soft-delete tombstone with merge instead of hard delete`() = runTest {
        val docRef = mockSubcollectionDocRef(ExpenseDocument.COLLECTION_PATH, "exp-1")
        val payloadSlot = slot<Map<String, Any>>()
        every { docRef.set(capture(payloadSlot), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreExpenseDataSourceImpl(firestore, authService)
        ds.deleteExpense(groupId, "exp-1")

        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
        assertEquals(true, payloadSlot.captured[FirestoreSyncFields.IS_DELETED])
        assertEquals(FieldValue.serverTimestamp(), payloadSlot.captured[FirestoreSyncFields.SERVER_UPDATED_AT])
        assertEquals(userId, payloadSlot.captured[FirestoreSyncFields.LAST_UPDATED_BY])
    }

    @Test
    fun `addExpenseWithCashPreconditions updates withdrawal remainingAmount and serverUpdatedAt`() = runTest {
        val groupsCol = mockk<CollectionReference>(relaxed = true)
        val groupDoc = mockk<DocumentReference>(relaxed = true)
        val expensesCol = mockk<CollectionReference>(relaxed = true)
        val expenseDocRef = mockk<DocumentReference>(relaxed = true)
        val withdrawalsCol = mockk<CollectionReference>(relaxed = true)
        val withdrawalDocRef = mockk<DocumentReference>(relaxed = true)
        val withdrawalSnap = mockk<DocumentSnapshot>(relaxed = true)
        val transaction = mockk<Transaction>(relaxed = true)

        every { firestore.collection(GroupDocument.COLLECTION_PATH) } returns groupsCol
        every { groupsCol.document(groupId) } returns groupDoc
        every { groupDoc.collection(ExpenseDocument.COLLECTION_PATH) } returns expensesCol
        every { expensesCol.document("exp-1") } returns expenseDocRef
        every { groupDoc.collection(CashWithdrawalDocument.COLLECTION_PATH) } returns withdrawalsCol
        every { withdrawalsCol.document("w-1") } returns withdrawalDocRef
        every { withdrawalDocRef.id } returns "w-1"
        every { withdrawalSnap.exists() } returns true
        every { withdrawalSnap.getLong("remainingAmount") } returns 5000L

        every { transaction.get(withdrawalDocRef) } returns withdrawalSnap
        every { firestore.runTransaction(any<Transaction.Function<Unit>>()) } answers {
            val fn = firstArg<Transaction.Function<Unit>>()
            fn.apply(transaction)
            Tasks.forResult(null)
        }

        val expense = Expense(
            id = "exp-1",
            groupId = groupId,
            title = "Cash Dinner",
            sourceAmount = 2000L,
            sourceCurrency = "EUR",
            groupAmount = 2000L,
            groupCurrency = "EUR",
            exchangeRate = BigDecimal.ONE,
            paymentMethod = PaymentMethod.CASH,
            createdBy = userId,
            payerType = PayerType.GROUP,
            cashTranches = listOf(CashTranche(withdrawalId = "w-1", amountConsumed = 2000L))
        )

        val ds = FirestoreExpenseDataSourceImpl(firestore, authService)
        ds.addExpenseWithCashPreconditions(groupId, expense, mapOf("w-1" to 5000L))

        val updateSlot = slot<Map<String, Any>>()
        verify(exactly = 1) { transaction.update(withdrawalDocRef, capture(updateSlot)) }
        assertEquals(3000L, updateSlot.captured["remainingAmount"])
        assertEquals(FieldValue.serverTimestamp(), updateSlot.captured[FirestoreSyncFields.SERVER_UPDATED_AT])
    }

    @Test
    fun `deleteContribution writes soft-delete tombstone with merge`() = runTest {
        val docRef = mockSubcollectionDocRef(ContributionDocument.COLLECTION_PATH, "c-1")
        every { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreContributionDataSourceImpl(firestore, authService)
        ds.deleteContribution(groupId, "c-1")

        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `deleteWithdrawal writes soft-delete tombstone with merge`() = runTest {
        val docRef = mockSubcollectionDocRef(CashWithdrawalDocument.COLLECTION_PATH, "w-1")
        every { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreCashWithdrawalDataSourceImpl(firestore, authService)
        ds.deleteWithdrawal(groupId, "w-1")

        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `deleteCashTransfer writes soft-delete tombstone with merge`() = runTest {
        val docRef = mockSubcollectionDocRef(CashTransferDocument.COLLECTION_PATH, "ct-1")
        every { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreCashTransferDataSourceImpl(firestore)
        val result = ds.deleteCashTransfer(groupId, "ct-1")

        assertTrue(result.isSuccess)
        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `deleteSubunit writes soft-delete tombstone with merge`() = runTest {
        val docRef = mockSubcollectionDocRef(SubunitDocument.COLLECTION_PATH, "sub-1")
        every { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreSubunitDataSourceImpl(firestore, authService)
        ds.deleteSubunit(groupId, "sub-1")

        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `deleteSettlement writes soft-delete tombstone with merge`() = runTest {
        val docRef = mockSubcollectionDocRef("settlements", "set-1")
        every { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) } returns Tasks.forResult(null)

        val ds = FirestoreSettlementDataSourceImpl(firestore)
        ds.deleteSettlement(groupId, "set-1")

        verify(exactly = 0) { docRef.delete() }
        verify(exactly = 1) { docRef.set(any<Map<String, Any>>(), SetOptions.merge()) }
    }

    @Test
    fun `verifyOnServer returns false when document is tombstoned`() = runTest {
        val docRef = mockSubcollectionDocRef(ExpenseDocument.COLLECTION_PATH, "exp-tombstone")
        val snap = mockk<DocumentSnapshot>(relaxed = true)
        every { snap.exists() } returns true
        every { snap.getBoolean(FirestoreSyncFields.IS_DELETED) } returns true
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snap)

        val ds = FirestoreExpenseDataSourceImpl(firestore, authService)
        assertFalse(ds.verifyExpenseOnServer(groupId, "exp-tombstone"))
    }

    @Test
    fun `removeUserFromSubunits applies limit500 and sets serverUpdatedAt`() = runTest {
        val groupsCol = mockk<CollectionReference>(relaxed = true)
        val groupDoc = mockk<DocumentReference>(relaxed = true)
        val subunitsCol = mockk<CollectionReference>(relaxed = true)
        val orderedQuery = mockk<Query>(relaxed = true)
        val limitedQuery = mockk<Query>(relaxed = true)
        val subDocRef = mockk<DocumentReference>(relaxed = true)
        val subDocSnap = mockk<DocumentSnapshot>(relaxed = true)
        val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
        val batch = mockk<WriteBatch>(relaxed = true)

        every { firestore.collection(GroupDocument.COLLECTION_PATH) } returns groupsCol
        every { groupsCol.document(groupId) } returns groupDoc
        every { groupDoc.collection(SubunitDocument.COLLECTION_PATH) } returns subunitsCol
        every { subunitsCol.orderBy(FieldPath.documentId()) } returns orderedQuery
        every { orderedQuery.limit(500L) } returns limitedQuery
        every { limitedQuery.get() } returns Tasks.forResult(querySnapshot)

        every { subDocSnap.reference } returns subDocRef
        every { subDocSnap.getBoolean(FirestoreSyncFields.IS_DELETED) } returns false
        every { subDocSnap.get("memberIds") } returns listOf(userId, "user-2")
        every { querySnapshot.documents } returns listOf(subDocSnap)

        every { firestore.batch() } returns batch
        every { batch.update(any(), any<Map<String, Any>>()) } returns batch
        every { batch.commit() } returns Tasks.forResult(null)

        val ds = FirestoreGroupDataSourceImpl(firestore, authService, performanceMonitor)
        ds.removeUserFromSubunits(groupId, userId)

        verify(exactly = 1) { orderedQuery.limit(500L) }
        val updateSlot = slot<Map<String, Any>>()
        verify(exactly = 1) { batch.update(subDocRef, capture(updateSlot)) }
        assertEquals(FieldValue.serverTimestamp(), updateSlot.captured[FirestoreSyncFields.SERVER_UPDATED_AT])
        assertTrue(updateSlot.captured.containsKey("memberIds"))
    }
}
