package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashTransferDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionDelta
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionFullSnapshot
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.observeCollectionDeltaFlow
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashTransferDataSource
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreCashTransferDataSourceImpl(
    private val firestore: FirebaseFirestore
) : CloudCashTransferDataSource {

    override suspend fun upsertCashTransfer(transfer: CashTransfer): Result<Unit> {
        return try {
            createTransfersCollection(transfer.groupId)
                .document(transfer.id)
                .set(transfer.toDocument())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCashTransfer(groupId: String, transferId: String): Result<Unit> {
        return try {
            createTransfersCollection(groupId)
                .document(transferId)
                .set(
                    FirestoreSyncFields.buildSoftDeleteTombstone(groupId = groupId),
                    SetOptions.merge()
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGroupCashTransfersFullSnapshot(
        groupId: String
    ): DeltaSyncBatch<CashTransfer> =
        fetchCollectionFullSnapshot(
            collectionRef = createTransfersCollection(groupId),
            mapDocument = { it.toObject(CashTransferDocument::class.java)?.toDomain(it.id) },
            sort = { list -> list.sortedByDescending { it.createdAt } }
        )

    override suspend fun fetchGroupCashTransfersDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<CashTransfer> =
        fetchCollectionDelta(
            collectionRef = createTransfersCollection(groupId),
            sinceWatermark = sinceWatermark,
            mapDocument = { it.toObject(CashTransferDocument::class.java)?.toDomain(it.id) },
            sort = { list -> list.sortedByDescending { it.createdAt } }
        )

    override fun observeGroupCashTransfersDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<CashTransfer>> =
        observeCollectionDeltaFlow(
            collectionRef = createTransfersCollection(groupId),
            sinceWatermark = sinceWatermark,
            entityLabel = "cash transfers",
            mapDocument = { it.toObject(CashTransferDocument::class.java)?.toDomain(it.id) },
            sort = { list -> list.sortedByDescending { it.createdAt } }
        )

    override suspend fun verifyCashTransferOnServer(groupId: String, transferId: String): Boolean {
        val doc = createTransfersCollection(groupId)
            .document(transferId)
            .get(Source.SERVER)
            .await()
        return doc.exists() && doc.getBoolean(FirestoreSyncFields.IS_DELETED) != true
    }

    private fun createTransfersCollection(groupId: String): CollectionReference = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(CashTransferDocument.COLLECTION_PATH)
}
