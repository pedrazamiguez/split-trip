package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.Transaction
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudExpenseDataSource
import es.pedrazamiguez.splittrip.domain.exception.CashConflictException
import es.pedrazamiguez.splittrip.domain.model.Expense
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Suppress("TooManyFunctions")
class FirestoreExpenseDataSourceImpl(
    private val firestore: FirebaseFirestore,
    private val authenticationService: AuthenticationService
) : CloudExpenseDataSource {

    override suspend fun addExpense(groupId: String, expense: Expense) {
        val userId = authenticationService.requireUserId()
        val expenseId = expense.id

        val groupDocRef = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
        val expenseDocRef = groupDocRef
            .collection(ExpenseDocument.COLLECTION_PATH)
            .document(expenseId)

        val expenseDocument = expense.toDocument(
            expenseId,
            groupId,
            groupDocRef,
            userId
        )

        expenseDocRef
            .set(expenseDocument)
            .await()
    }

    override suspend fun deleteExpense(groupId: String, expenseId: String) {
        firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(ExpenseDocument.COLLECTION_PATH)
            .document(expenseId)
            .delete()
            .await()
    }

    override suspend fun fetchExpensesByGroupId(groupId: String): List<Expense> {
        val snapshot = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(ExpenseDocument.COLLECTION_PATH)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(ExpenseDocument::class.java)?.toDomain()
        }.sortedByDescending { it.createdAt ?: it.lastUpdatedAt }
    }

    override fun getExpensesByGroupIdFlow(groupId: String): Flow<List<Expense>> = callbackFlow {
        val expensesCollection = createExpensesCollection(groupId)

        val listener = createExpenseListener(expensesCollection) { snapshot ->
            launch {
                val cachedExpenses = loadExpensesFromCache(
                    expensesCollection,
                    snapshot.documents
                )

                trySend(cachedExpenses)

                val cachedExpenseIds = cachedExpenses
                    .map { it.id }
                    .toSet()
                val missingExpenseIds = snapshot.documents
                    .map { it.id }
                    .filter { it !in cachedExpenseIds }

                if (missingExpenseIds.isNotEmpty()) {
                    val serverExpenses = loadExpensesFromServer(
                        expensesCollection,
                        missingExpenseIds
                    )
                    val allExpenses = (cachedExpenses + serverExpenses).sortedByDescending {
                        it.createdAt ?: it.lastUpdatedAt
                    }
                    trySend(allExpenses)
                }
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun verifyExpenseOnServer(groupId: String, expenseId: String): Boolean {
        val doc = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(ExpenseDocument.COLLECTION_PATH)
            .document(expenseId)
            .get(Source.SERVER)
            .await()
        return doc.exists()
    }

    /**
     * Saves a cash-funded expense using an optimistic-locking Firestore transaction.
     *
     * The transaction atomically:
     * 1. Reads the current `remainingAmount` for each consumed withdrawal.
     * 2. Verifies each against [expectedRemainingAmounts] — throws [CashConflictException]
     *    on any mismatch (non-retriable; Firestore SDK does not retry non-[com.google.firebase.firestore.FirebaseFirestoreException]).
     * 3. Writes the expense document.
     * 4. Updates each consumed withdrawal's `remainingAmount` to `expected − consumed`.
     *
     * Complexity is intentionally spread across focused private helpers:
     * - [checkTranchePreconditions] — validates caller inputs before touching Firestore.
     * - [runCashTransaction] — drives the Firestore transaction + catch logic.
     * - [verifyWithdrawalSnapshots] — optimistic-locking check inside the transaction.
     * - [applyWithdrawalUpdates] — writes the deducted amounts inside the transaction.
     */
    override suspend fun addExpenseWithCashPreconditions(
        groupId: String,
        expense: Expense,
        expectedRemainingAmounts: Map<String, Long>
    ) {
        val userId = authenticationService.requireUserId()
        val expenseId = expense.id
        val trancheMap = expense.cashTranches.associate { it.withdrawalId to it.amountConsumed }

        checkTranchePreconditions(trancheMap, expectedRemainingAmounts)

        val groupDocRef = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
        val expenseDocRef = groupDocRef
            .collection(ExpenseDocument.COLLECTION_PATH)
            .document(expenseId)
        val expenseDocument = expense.toDocument(expenseId, groupId, groupDocRef, userId)

        val withdrawalRefs = expectedRemainingAmounts.keys.map { withdrawalId ->
            groupDocRef
                .collection(CashWithdrawalDocument.COLLECTION_PATH)
                .document(withdrawalId)
        }

        // Pre-compute the new remaining amounts — avoids a double-continue loop inside
        // the transaction where both trancheMap and expectedRemainingAmounts are involved.
        val withdrawalUpdates: Map<String, Long> = trancheMap.mapValues { (withdrawalId, consumed) ->
            expectedRemainingAmounts.getValue(withdrawalId) - consumed
        }

        runCashTransaction(expenseDocRef, expenseDocument, withdrawalRefs, withdrawalUpdates, expectedRemainingAmounts)
    }

    /**
     * Validates caller inputs before touching Firestore:
     * - Every consumed withdrawal must have a snapshot value in [expectedRemainingAmounts].
     * - No tranche may consume more than its expected remaining amount.
     */
    private fun checkTranchePreconditions(
        trancheMap: Map<String, Long>,
        expectedRemainingAmounts: Map<String, Long>
    ) {
        require(expectedRemainingAmounts.keys.containsAll(trancheMap.keys)) {
            "expectedRemainingAmounts is missing entries for withdrawal IDs: " +
                (trancheMap.keys - expectedRemainingAmounts.keys)
        }
        for ((withdrawalId, consumed) in trancheMap) {
            require(consumed <= expectedRemainingAmounts.getValue(withdrawalId)) {
                "Consumed $consumed > expected remaining " +
                    "${expectedRemainingAmounts.getValue(withdrawalId)} for withdrawal $withdrawalId"
            }
        }
    }

    /**
     * Executes the Firestore transaction and handles the two-layer catch needed because
     * the SDK may wrap a [CashConflictException] thrown inside the transaction lambda
     * as the `cause` of another exception.
     */
    private suspend fun runCashTransaction(
        expenseDocRef: DocumentReference,
        expenseDocument: ExpenseDocument,
        withdrawalRefs: List<DocumentReference>,
        withdrawalUpdates: Map<String, Long>,
        expectedRemainingAmounts: Map<String, Long>
    ) {
        try {
            firestore.runTransaction { transaction ->
                val withdrawalDocs = withdrawalRefs.map { transaction.get(it) }
                verifyWithdrawalSnapshots(withdrawalRefs, withdrawalDocs, expectedRemainingAmounts)
                transaction.set(expenseDocRef, expenseDocument)
                applyWithdrawalUpdates(transaction, withdrawalRefs, withdrawalUpdates)
            }.await()
        } catch (e: Exception) {
            // The Firestore SDK may wrap a CashConflictException thrown inside the transaction
            // lambda as the cause of another exception; unwrap it before re-throwing.
            throw (e as? CashConflictException) ?: (e.cause as? CashConflictException) ?: e
        }
    }

    /**
     * Optimistic-locking check: verifies each withdrawal's server-side `remainingAmount`
     * against the caller's snapshot. Throws [CashConflictException] on any mismatch,
     * missing document, or missing field.
     */
    private fun verifyWithdrawalSnapshots(
        withdrawalRefs: List<DocumentReference>,
        withdrawalDocs: List<DocumentSnapshot>,
        expectedRemainingAmounts: Map<String, Long>
    ) {
        for ((ref, doc) in withdrawalRefs.zip(withdrawalDocs)) {
            val serverRemaining = doc.takeIf { it.exists() }?.getLong(FIELD_REMAINING_AMOUNT)
                ?: throw CashConflictException()
            if (serverRemaining != expectedRemainingAmounts.getValue(ref.id)) {
                throw CashConflictException()
            }
        }
    }

    /**
     * Writes the deducted `remainingAmount` for each consumed withdrawal inside the
     * ongoing transaction. Withdrawals with no entry in [withdrawalUpdates] are skipped
     * (they were read for optimistic-locking but not consumed by any tranche).
     */
    private fun applyWithdrawalUpdates(
        transaction: Transaction,
        withdrawalRefs: List<DocumentReference>,
        withdrawalUpdates: Map<String, Long>
    ) {
        for (ref in withdrawalRefs) {
            val newRemaining = withdrawalUpdates[ref.id] ?: continue
            transaction.update(ref, FIELD_REMAINING_AMOUNT, newRemaining)
        }
    }

    private fun createExpensesCollection(groupId: String) = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(ExpenseDocument.COLLECTION_PATH)

    private fun createExpenseListener(expensesCollection: CollectionReference, onUpdate: (QuerySnapshot) -> Unit) =
        expensesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(
                    error,
                    "Error listening to expenses"
                )
                return@addSnapshotListener
            }

            snapshot?.let(onUpdate)
        }

    private suspend fun loadExpensesFromCache(
        expensesCollection: CollectionReference,
        documents: List<DocumentSnapshot>
    ): List<Expense> = documents
        .mapNotNull { doc ->
            loadSingleExpenseFromCache(
                expensesCollection,
                doc.id
            )
        }
        .sortedByDescending { it.createdAt ?: it.lastUpdatedAt }

    private suspend fun loadSingleExpenseFromCache(
        expensesCollection: CollectionReference,
        expenseId: String
    ): Expense? = try {
        @Suppress("kotlin:S6518")
        val cachedDoc = expensesCollection
            .document(expenseId)
            .get(Source.CACHE)
            .await()

        if (cachedDoc.exists()) {
            cachedDoc
                .toObject(ExpenseDocument::class.java)
                ?.toDomain()
        } else {
            null
        }
    } catch (_: Exception) {
        Timber.d("Cache miss for expense $expenseId")
        null
    }

    private suspend fun loadExpensesFromServer(
        expensesCollection: CollectionReference,
        expenseIds: List<String>
    ): List<Expense> = try {
        expenseIds
            .chunked(FIRESTORE_WHERE_IN_LIMIT)
            .flatMap { batch ->
                expensesCollection
                    .whereIn(FieldPath.documentId(), batch)
                    .get(Source.SERVER)
                    .await()
                    .documents
                    .mapNotNull { it.toObject(ExpenseDocument::class.java)?.toDomain() }
            }
    } catch (e: Exception) {
        Timber.e(e, "Failed to load expenses from server")
        emptyList()
    }

    private companion object {
        const val FIRESTORE_WHERE_IN_LIMIT = 30
        const val FIELD_REMAINING_AMOUNT = "remainingAmount"
    }
}
