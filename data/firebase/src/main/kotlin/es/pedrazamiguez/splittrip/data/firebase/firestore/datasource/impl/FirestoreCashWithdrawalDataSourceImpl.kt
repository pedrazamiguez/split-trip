package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashWithdrawalDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionDelta
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionFullSnapshot
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.observeCollectionDeltaFlow
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashWithdrawalDataSource
import es.pedrazamiguez.splittrip.domain.model.CashWithdrawal
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreCashWithdrawalDataSourceImpl(
    private val firestore: FirebaseFirestore,
    private val authenticationService: AuthenticationService
) : CloudCashWithdrawalDataSource {

    override suspend fun addWithdrawal(groupId: String, withdrawal: CashWithdrawal) {
        upsertWithdrawal(groupId, withdrawal)
    }

    override suspend fun updateWithdrawal(groupId: String, withdrawal: CashWithdrawal) {
        upsertWithdrawal(groupId, withdrawal)
    }

    override suspend fun deleteWithdrawal(groupId: String, withdrawalId: String) {
        createWithdrawalsCollection(groupId)
            .document(withdrawalId)
            .set(
                FirestoreSyncFields.buildSoftDeleteTombstone(
                    groupId = groupId,
                    deletedByUserId = authenticationService.currentUserId()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun fetchWithdrawalsFullSnapshot(groupId: String): DeltaSyncBatch<CashWithdrawal> =
        fetchCollectionFullSnapshot(
            collectionRef = createWithdrawalsCollection(groupId),
            mapDocument = { it.toObject(CashWithdrawalDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override suspend fun fetchWithdrawalsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<CashWithdrawal> =
        fetchCollectionDelta(
            collectionRef = createWithdrawalsCollection(groupId),
            sinceWatermark = sinceWatermark,
            mapDocument = { it.toObject(CashWithdrawalDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override fun observeWithdrawalsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<CashWithdrawal>> =
        observeCollectionDeltaFlow(
            collectionRef = createWithdrawalsCollection(groupId),
            sinceWatermark = sinceWatermark,
            entityLabel = "cash withdrawals",
            mapDocument = { it.toObject(CashWithdrawalDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override suspend fun verifyWithdrawalOnServer(
        groupId: String,
        withdrawalId: String
    ): Boolean {
        val doc = createWithdrawalsCollection(groupId)
            .document(withdrawalId)
            .get(Source.SERVER)
            .await()
        return doc.exists() && doc.getBoolean(FirestoreSyncFields.IS_DELETED) != true
    }

    private suspend fun upsertWithdrawal(groupId: String, withdrawal: CashWithdrawal) {
        val userId = authenticationService.requireUserId()
        val withdrawalDocument = withdrawal.toDocument(withdrawal.id, groupId, userId)
        createWithdrawalsCollection(groupId)
            .document(withdrawal.id)
            .set(withdrawalDocument)
            .await()
    }

    private fun createWithdrawalsCollection(groupId: String) = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(CashWithdrawalDocument.COLLECTION_PATH)
}
