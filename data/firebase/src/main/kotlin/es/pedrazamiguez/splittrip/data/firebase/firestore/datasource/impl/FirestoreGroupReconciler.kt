package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Transaction
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupMemberDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.UserDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirestoreGroupReconciler(private val firestore: FirebaseFirestore) {

    suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String) {
        retryOnPermissionDenied("reconcileUnregisteredUser") {
            executeReconcileUnregisteredUser(pendingUserId, activeUserId)
        }
    }

    private suspend fun executeReconcileUnregisteredUser(pendingUserId: String, activeUserId: String) {
        val matchingGroupsSnapshot = firestore.collection(GroupDocument.COLLECTION_PATH)
            .whereArrayContains("memberIds", pendingUserId)
            .get()
            .await()

        for (groupDoc in matchingGroupsSnapshot.documents) {
            val groupId = groupDoc.id
            reconcileGroupData(groupId, groupDoc.reference, pendingUserId, activeUserId)
        }

        // Delete users/$pendingUserId document
        firestore.collection(UserDocument.COLLECTION_PATH).document(pendingUserId).delete().await()
    }

    private suspend fun reconcileGroupData(
        groupId: String,
        groupDocRef: DocumentReference,
        pendingUserId: String,
        activeUserId: String
    ) {
        val memberDocRef = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(GroupMemberDocument.SUBCOLLECTION_PATH)
            .document(pendingUserId)

        val activeMemberDocRef = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(GroupMemberDocument.SUBCOLLECTION_PATH)
            .document(activeUserId)

        val expenseDocs = fetchActiveDocuments(
            firestore.collection(GroupDocument.COLLECTION_PATH)
                .document(groupId)
                .collection(ExpenseDocument.COLLECTION_PATH)
        )

        val contributionDocs = fetchActiveDocuments(
            firestore.collection(GroupDocument.COLLECTION_PATH)
                .document(groupId)
                .collection(ContributionDocument.COLLECTION_PATH)
        )

        val withdrawalDocs = fetchActiveDocuments(
            firestore.collection(GroupDocument.COLLECTION_PATH)
                .document(groupId)
                .collection(CashWithdrawalDocument.COLLECTION_PATH)
        )

        firestore.runTransaction { transaction ->
            // --- ALL READS FIRST ---
            val freshGroupDoc = transaction.get(groupDocRef)
            val pendingMemberSnap = transaction.get(memberDocRef)
            val freshExpenseSnaps = expenseDocs.map { transaction.get(it.reference) }
            val freshContributionSnaps = contributionDocs.map { transaction.get(it.reference) }
            val freshWithdrawalSnaps = withdrawalDocs.map { transaction.get(it.reference) }

            // --- ALL WRITES AFTER ---
            updateGroupMembersInTransaction(transaction, groupDocRef, freshGroupDoc, pendingUserId, activeUserId)

            migrateMemberDocumentInTransaction(
                transaction,
                memberDocRef,
                activeMemberDocRef,
                pendingMemberSnap,
                activeUserId
            )

            updateExpensesInTransaction(transaction, freshExpenseSnaps, pendingUserId, activeUserId)

            updateContributionsInTransaction(transaction, freshContributionSnaps, pendingUserId, activeUserId)

            updateWithdrawalsInTransaction(transaction, freshWithdrawalSnaps, pendingUserId, activeUserId)
        }.await()
    }

    private fun updateGroupMembersInTransaction(
        transaction: Transaction,
        groupDocRef: DocumentReference,
        freshGroupDoc: DocumentSnapshot,
        pendingUserId: String,
        activeUserId: String
    ) {
        val memberIds = freshGroupDoc.get("memberIds") as? List<*> ?: emptyList<Any>()
        val updatedMemberIds = memberIds.map { if (it == pendingUserId) activeUserId else it }
        transaction.update(groupDocRef, "memberIds", updatedMemberIds)
    }

    private fun migrateMemberDocumentInTransaction(
        transaction: Transaction,
        memberDocRef: DocumentReference,
        activeMemberDocRef: DocumentReference,
        pendingMemberSnap: DocumentSnapshot,
        activeUserId: String
    ) {
        if (pendingMemberSnap.exists()) {
            val memberDoc = pendingMemberSnap.toObject(GroupMemberDocument::class.java)
            if (memberDoc != null) {
                val activeMemberDoc = memberDoc.copy(
                    memberId = activeUserId,
                    userId = activeUserId
                )
                transaction.set(activeMemberDocRef, activeMemberDoc)
                transaction.delete(memberDocRef)
            }
        }
    }

    private suspend fun fetchActiveDocuments(
        collectionRef: CollectionReference
    ): List<DocumentSnapshot> {
        val activeDocs = mutableListOf<DocumentSnapshot>()
        var lastDoc: DocumentSnapshot? = null
        do {
            var query = collectionRef
                .orderBy(FieldPath.documentId())
                .limit(FirestoreSyncFields.MAX_QUERY_LIMIT)
            if (lastDoc != null) {
                query = query.startAfter(lastDoc)
            }
            val page = query.get().await()
            page.documents
                .filter { it.getBoolean(FirestoreSyncFields.IS_DELETED) != true }
                .let(activeDocs::addAll)
            lastDoc = page.documents.lastOrNull()
        } while (page.documents.size >= FirestoreSyncFields.MAX_QUERY_LIMIT && lastDoc != null)
        return activeDocs
    }

    private fun updateExpensesInTransaction(
        transaction: Transaction,
        expenseSnaps: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String
    ) = updateDocumentsInTransaction(transaction, expenseSnaps, ExpenseDocument::class.java) { expense ->
        expense.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)
    }

    private fun updateContributionsInTransaction(
        transaction: Transaction,
        contributionSnaps: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String
    ) = updateDocumentsInTransaction(transaction, contributionSnaps, ContributionDocument::class.java) { contribution ->
        contribution.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)
    }

    private fun updateWithdrawalsInTransaction(
        transaction: Transaction,
        withdrawalSnaps: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String
    ) = updateDocumentsInTransaction(transaction, withdrawalSnaps, CashWithdrawalDocument::class.java) { withdrawal ->
        withdrawal.getUpdatedIfNeedsUpdate(pendingUserId, activeUserId)
    }

    private fun <T : Any> updateDocumentsInTransaction(
        transaction: Transaction,
        snapshots: List<DocumentSnapshot>,
        clazz: Class<T>,
        transform: (T) -> T?
    ) {
        snapshots.forEach { snap ->
            if (snap.getBoolean(FirestoreSyncFields.IS_DELETED) != true) {
                val updated = snap.toObject(clazz)?.let(transform)
                if (updated != null) {
                    transaction.set(snap.reference, updated)
                }
            }
        }
    }

    private suspend inline fun <T> retryOnPermissionDenied(
        methodName: String,
        crossinline block: suspend () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: FirebaseFirestoreException) {
                if (e.code != FirebaseFirestoreException.Code.PERMISSION_DENIED || ++attempt >= MAX_RETRY_ATTEMPTS) {
                    throw e
                }
                Timber.w("$methodName PERMISSION_DENIED. Retrying (attempt $attempt/$MAX_RETRY_ATTEMPTS)")
                delay(RETRY_DELAY_MS)
            }
        }
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
        const val RETRY_DELAY_MS = 500L
    }
}
