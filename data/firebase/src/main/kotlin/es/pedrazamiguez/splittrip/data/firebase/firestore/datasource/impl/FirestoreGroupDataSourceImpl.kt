package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ExpenseSplitDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupMemberDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.UserDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toAdminMemberDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toRegularMemberDocument
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudGroupDataSource
import es.pedrazamiguez.splittrip.domain.model.Group
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

@Suppress("TooManyFunctions")
class FirestoreGroupDataSourceImpl(
    private val firestore: FirebaseFirestore,
    private val authenticationService: AuthenticationService,
    private val groupLoader: FirestoreGroupLoader = FirestoreGroupLoader(firestore)
) : CloudGroupDataSource {

    override suspend fun createGroup(group: Group): String {
        val userId = authenticationService.requireUserId()
        val groupId = group.id

        val groupsCollection = firestore.collection(GroupDocument.COLLECTION_PATH)
        val groupDocRef = groupsCollection.document(groupId)

        // Ensure the creator is included in denormalized memberIds
        val groupWithCreator = if (userId !in group.members) {
            group.copy(members = group.members + userId)
        } else {
            group
        }

        val groupDocument = groupWithCreator.toDocument(
            groupId,
            userId
        )
        val adminMemberDocument = toAdminMemberDocument(
            groupDocRef,
            userId
        )

        val batch = firestore
            .batch()
            .apply {
                set(
                    groupDocRef,
                    groupDocument
                )
                // Creator as ADMIN member
                set(
                    firestore
                        .collection(GroupMemberDocument.collectionPath(groupId))
                        .document(userId),
                    adminMemberDocument
                )
                // Additional members (non-creator) as MEMBER role
                groupWithCreator.members
                    .filter { it != userId }
                    .forEach { memberId ->
                        val memberDocRef = firestore
                            .collection(GroupMemberDocument.collectionPath(groupId))
                            .document(memberId)
                        val memberDocument = toRegularMemberDocument(
                            groupDocRef,
                            memberId,
                            addedBy = userId
                        )
                        set(memberDocRef, memberDocument)
                    }
            }

        batch
            .commit()
            .await()

        return groupId
    }

    override suspend fun getGroupById(groupId: String): Group? {
        // Try cache first
        val cachedGroup = groupLoader.loadSingleGroupFromCache(groupId)
        if (cachedGroup != null) {
            return cachedGroup
        }

        // If not in cache, fetch from server
        return try {
            val groupDoc = firestore
                .collection(GroupDocument.COLLECTION_PATH)
                .document(groupId)
                .get()
                .await()

            if (groupDoc.exists()) {
                groupDoc
                    .toObject(GroupDocument::class.java)
                    ?.toDomain()
            } else {
                Timber.w("Group not found: $groupId")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching group $groupId from server")
            null
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        // 1. Delete all member documents in the subcollection FIRST.
        // This is critical for real-time sync: the snapshotListener on group_members
        // collectionGroup fires when member docs are removed, causing other devices
        // to stop seeing this group. Firestore does NOT auto-delete subcollections
        // when a parent document is deleted.
        val membersCollection = firestore
            .collection(GroupMemberDocument.collectionPath(groupId))
        val memberDocs = membersCollection.get().await()
        memberDocs.documents.forEach { doc ->
            doc.reference.delete().await()
        }

        // 2. Delete the group document itself
        firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .delete()
            .await()
    }

    /**
     * Signals Firestore to initiate a server-side cascading group deletion.
     *
     * Uses a WriteBatch to atomically:
     * 1. Set `deletionRequested = true` on the group document — triggers the
     *    `onGroupDeletionRequested` Cloud Function for cascade cleanup.
     * 2. Delete the current user's member document — prevents entity resurrection.
     *
     * The early member-doc deletion is critical for offline create→delete flows:
     * the snapshot listener on `group_members` uses `MetadataChanges.INCLUDE`,
     * which fires when pending writes are confirmed. Without the member-doc
     * deletion, the member doc persists until the Cloud Function cascades,
     * causing a brief resurrection in the snapshot (the listener sees the member
     * doc, loads the group, and upserts it into Room). By including the member
     * deletion in the same batch, Firestore's latency compensation excludes the
     * member doc from snapshot results, preventing the group from reappearing.
     *
     * The Cloud Function handles missing member docs gracefully (Firestore
     * `batch.delete()` on non-existent docs is a no-op).
     */
    override suspend fun requestGroupDeletion(groupId: String) {
        val userId = authenticationService.requireUserId()
        val groupDocRef = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
        val memberDocRef = firestore
            .collection(GroupMemberDocument.collectionPath(groupId))
            .document(userId)

        firestore.batch()
            .apply {
                update(
                    groupDocRef,
                    mapOf(
                        "deletionRequested" to true,
                        "deletedBy" to userId,
                        "deletedAt" to FieldValue.serverTimestamp()
                    )
                )
                delete(memberDocRef)
            }
            .commit()
            .await()
    }

    override suspend fun verifyGroupOnServer(groupId: String): Boolean {
        val doc = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .get(Source.SERVER)
            .await()
        return doc.exists()
    }

    override suspend fun fetchAllGroups(): List<Group> {
        val userId = authenticationService.requireUserId()

        val memberSnapshot = firestore
            .collectionGroup(GroupMemberDocument.SUBCOLLECTION_PATH)
            .whereEqualTo(GroupMemberDocument.USER_ID_FIELD, userId)
            .get()
            .await()

        val groupRefs = extractGroupReferences(memberSnapshot.documents)
        if (groupRefs.isEmpty()) return emptyList()

        val groupIds = groupRefs.map { it.id }
        return groupLoader.loadGroupsFromServer(groupIds)
            .sortedByDescending { it.lastUpdatedAt }
    }

    override fun getAllGroupsFlow(): Flow<List<Group>> = callbackFlow {
        val userId = authenticationService.requireUserId()

        val listener = createGroupMemberListener(userId) { groupRefs ->
            if (groupRefs.isEmpty()) {
                trySend(emptyList())
                return@createGroupMemberListener
            }

            launch {
                val groupIds = groupRefs.map { it.id }
                val cachedGroups = groupLoader.loadGroupsFromCache(groupIds)

                trySend(cachedGroups)

                val missingGroupIds = groupIds.filter { groupId ->
                    cachedGroups.none { it.id == groupId }
                }

                if (missingGroupIds.isNotEmpty()) {
                    val serverGroups = groupLoader.loadGroupsFromServer(missingGroupIds)
                    val allGroups =
                        (cachedGroups + serverGroups).sortedByDescending { it.lastUpdatedAt }
                    trySend(allGroups)
                }
            }
        }

        awaitClose { listener.remove() }
    }

    private fun createGroupMemberListener(userId: String, onUpdate: (List<DocumentReference>) -> Unit) = firestore
        .collectionGroup(GroupMemberDocument.SUBCOLLECTION_PATH)
        .whereEqualTo(
            GroupMemberDocument.USER_ID_FIELD,
            userId
        )
        // MetadataChanges.INCLUDE ensures the listener fires when Firestore
        // confirms pending local writes (hasPendingWrites transitions to false).
        // Without this, offline-created groups would stay PENDING_SYNC indefinitely
        // because the listener only fires on data changes, not metadata changes.
        .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Timber.e(
                    error,
                    "Error listening to group members"
                )
                return@addSnapshotListener
            }

            snapshot?.let { snap ->
                val groupRefs = extractGroupReferences(snap.documents)
                onUpdate(groupRefs)
            }
        }

    private fun extractGroupReferences(documents: List<DocumentSnapshot>) = documents.mapNotNull { doc ->
        doc.getDocumentReference(GroupMemberDocument.FIELD_GROUP_REF)
            ?: doc.reference.parent.parent
    }

    override suspend fun reconcileUnregisteredUser(pendingUserId: String, activeUserId: String) {
        val userRef = firestore.collection(UserDocument.COLLECTION_PATH).document(activeUserId)

        val matchingGroupsSnapshot = firestore.collection(GroupDocument.COLLECTION_PATH)
            .whereArrayContains("memberIds", pendingUserId)
            .get()
            .await()

        for (groupDoc in matchingGroupsSnapshot.documents) {
            val groupId = groupDoc.id
            reconcileGroupData(groupId, groupDoc.reference, pendingUserId, activeUserId, userRef)
        }

        // Delete users/$pendingUserId document
        firestore.collection(UserDocument.COLLECTION_PATH).document(pendingUserId).delete().await()
    }

    private suspend fun reconcileGroupData(
        groupId: String,
        groupDocRef: DocumentReference,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ) {
        val memberDocRef = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(GroupMemberDocument.SUBCOLLECTION_PATH)
            .document(pendingUserId)

        val activeMemberDocRef = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(GroupMemberDocument.SUBCOLLECTION_PATH)
            .document(activeUserId)

        val expensesQuery = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(ExpenseDocument.COLLECTION_PATH)
            .get()
            .await()

        val contributionsQuery = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(ContributionDocument.COLLECTION_PATH)
            .get()
            .await()

        val withdrawalsQuery = firestore.collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(CashWithdrawalDocument.COLLECTION_PATH)
            .get()
            .await()

        firestore.runTransaction { transaction ->
            // 1. Group members list update
            val freshGroupDoc = transaction.get(groupDocRef)
            val memberIds = freshGroupDoc.get("memberIds") as? List<*> ?: emptyList<Any>()
            val updatedMemberIds = memberIds.map { if (it == pendingUserId) activeUserId else it }
            transaction.update(groupDocRef, "memberIds", updatedMemberIds)

            // 2. Member subcollection document migration
            migrateMemberDocument(transaction, memberDocRef, activeMemberDocRef, activeUserId, userRef)

            // 3. Expenses updates
            updateExpensesInTransaction(transaction, expensesQuery.documents, pendingUserId, activeUserId, userRef)

            // 4. Contributions updates
            updateContributionsInTransaction(
                transaction,
                contributionsQuery.documents,
                pendingUserId,
                activeUserId,
                userRef
            )

            // 5. Cash withdrawals updates
            updateWithdrawalsInTransaction(
                transaction,
                withdrawalsQuery.documents,
                pendingUserId,
                activeUserId,
                userRef
            )
        }.await()
    }

    private fun migrateMemberDocument(
        transaction: com.google.firebase.firestore.Transaction,
        memberDocRef: DocumentReference,
        activeMemberDocRef: DocumentReference,
        activeUserId: String,
        userRef: DocumentReference
    ) {
        val pendingMemberSnap = transaction.get(memberDocRef)
        if (pendingMemberSnap.exists()) {
            val memberDoc = pendingMemberSnap.toObject(GroupMemberDocument::class.java)
            if (memberDoc != null) {
                val activeMemberDoc = memberDoc.copy(
                    memberId = activeUserId,
                    userId = activeUserId,
                    userRef = userRef
                )
                transaction.set(activeMemberDocRef, activeMemberDoc)
                transaction.delete(memberDocRef)
            }
        }
    }

    private fun updateExpensesInTransaction(
        transaction: com.google.firebase.firestore.Transaction,
        expenseDocs: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ) {
        for (expSnap in expenseDocs) {
            val freshExpSnap = transaction.get(expSnap.reference)
            val expense = freshExpSnap.toObject(ExpenseDocument::class.java) ?: continue
            val updatedExpense = getUpdatedExpenseIfNeedsUpdate(expense, pendingUserId, activeUserId, userRef)
            if (updatedExpense != null) {
                transaction.set(expSnap.reference, updatedExpense)
            }
        }
    }

    private fun getUpdatedExpenseIfNeedsUpdate(
        expense: ExpenseDocument,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ): ExpenseDocument? {
        var needsUpdate = false
        var payerId = expense.payerId
        var payerRef = expense.payerRef
        var createdBy = expense.createdBy
        var createdByRef = expense.createdByRef

        if (payerId == pendingUserId) {
            payerId = activeUserId
            payerRef = userRef
            needsUpdate = true
        }
        if (createdBy == pendingUserId) {
            createdBy = activeUserId
            createdByRef = userRef
            needsUpdate = true
        }

        val updatedSplits = expense.splits.map { split ->
            val updatedSplit = getUpdatedSplitIfNeedsUpdate(split, pendingUserId, activeUserId, userRef)
            if (updatedSplit != null) {
                needsUpdate = true
                updatedSplit
            } else {
                split
            }
        }

        return if (needsUpdate) {
            expense.copy(
                payerId = payerId,
                payerRef = payerRef,
                createdBy = createdBy,
                createdByRef = createdByRef,
                splits = updatedSplits
            )
        } else {
            null
        }
    }

    private fun getUpdatedSplitIfNeedsUpdate(
        split: ExpenseSplitDocument,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ): ExpenseSplitDocument? {
        var splitUpdated = false
        var sUserId = split.userId
        var sUserRef = split.userRef
        var sCoveredById = split.isCoveredById
        var sCoveredByRef = split.isCoveredByRef

        if (sUserId == pendingUserId) {
            sUserId = activeUserId
            sUserRef = userRef
            splitUpdated = true
        }
        if (sCoveredById == pendingUserId) {
            sCoveredById = activeUserId
            sCoveredByRef = userRef
            splitUpdated = true
        }

        return if (splitUpdated) {
            split.copy(
                userId = sUserId,
                userRef = sUserRef,
                isCoveredById = sCoveredById,
                isCoveredByRef = sCoveredByRef
            )
        } else {
            null
        }
    }

    private fun updateContributionsInTransaction(
        transaction: com.google.firebase.firestore.Transaction,
        contributionDocs: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ) {
        for (contrSnap in contributionDocs) {
            val freshContrSnap = transaction.get(contrSnap.reference)
            val contribution = freshContrSnap.toObject(ContributionDocument::class.java)
            if (contribution != null) {
                var needsUpdate = false
                var cUserId = contribution.userId
                var cCreatedBy = contribution.createdBy
                var cCreatedByRef = contribution.createdByRef

                if (cUserId == pendingUserId) {
                    cUserId = activeUserId
                    needsUpdate = true
                }
                if (cCreatedBy == pendingUserId) {
                    cCreatedBy = activeUserId
                    cCreatedByRef = userRef
                    needsUpdate = true
                }

                if (needsUpdate) {
                    val updatedContribution = contribution.copy(
                        userId = cUserId,
                        createdBy = cCreatedBy,
                        createdByRef = cCreatedByRef
                    )
                    transaction.set(contrSnap.reference, updatedContribution)
                }
            }
        }
    }

    private fun updateWithdrawalsInTransaction(
        transaction: com.google.firebase.firestore.Transaction,
        withdrawalDocs: List<DocumentSnapshot>,
        pendingUserId: String,
        activeUserId: String,
        userRef: DocumentReference
    ) {
        for (withdSnap in withdrawalDocs) {
            val freshWithdSnap = transaction.get(withdSnap.reference)
            val withdrawal = freshWithdSnap.toObject(CashWithdrawalDocument::class.java)
            if (withdrawal != null) {
                var needsUpdate = false
                var wWithdrawnBy = withdrawal.withdrawnBy
                var wCreatedBy = withdrawal.createdBy
                var wCreatedByRef = withdrawal.createdByRef

                if (wWithdrawnBy == pendingUserId) {
                    wWithdrawnBy = activeUserId
                    needsUpdate = true
                }
                if (wCreatedBy == pendingUserId) {
                    wCreatedBy = activeUserId
                    wCreatedByRef = userRef
                    needsUpdate = true
                }

                if (needsUpdate) {
                    val updatedWithdrawal = withdrawal.copy(
                        withdrawnBy = wWithdrawnBy,
                        createdBy = wCreatedBy,
                        createdByRef = wCreatedByRef
                    )
                    transaction.set(withdSnap.reference, updatedWithdrawal)
                }
            }
        }
    }
}
