package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toSettlementRecord
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionDelta
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionFullSnapshot
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.observeCollectionDeltaFlow
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSettlementDataSource
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SettlementRecord
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreSettlementDataSourceImpl(
    private val firestore: FirebaseFirestore
) : CloudSettlementDataSource {

    override suspend fun fetchSettlementsFullSnapshot(groupId: String): DeltaSyncBatch<SettlementRecord> =
        fetchCollectionFullSnapshot(
            collectionRef = createSettlementsCollection(groupId),
            mapDocument = { it.toSettlementRecord() }
        )

    override suspend fun fetchSettlementsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<SettlementRecord> =
        fetchCollectionDelta(
            collectionRef = createSettlementsCollection(groupId),
            sinceWatermark = sinceWatermark,
            mapDocument = { it.toSettlementRecord() }
        )

    override fun observeSettlementsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<SettlementRecord>> =
        observeCollectionDeltaFlow(
            collectionRef = createSettlementsCollection(groupId),
            sinceWatermark = sinceWatermark,
            entityLabel = "settlements for group $groupId",
            mapDocument = { it.toSettlementRecord() }
        )

    override suspend fun upsertSettlement(groupId: String, record: SettlementRecord) {
        createSettlementsCollection(groupId)
            .document(record.id)
            .set(record.toDocument(), SetOptions.merge())
            .await()
    }

    override suspend fun deleteSettlement(groupId: String, id: String) {
        createSettlementsCollection(groupId)
            .document(id)
            .set(
                FirestoreSyncFields.buildSoftDeleteTombstone(groupId = groupId),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun verifySettlementOnServer(groupId: String, id: String): Boolean {
        val doc = createSettlementsCollection(groupId)
            .document(id)
            .get(Source.SERVER)
            .await()
        return doc.exists() && doc.getBoolean(FirestoreSyncFields.IS_DELETED) != true
    }

    override suspend fun sendDebtorNudge(
        groupId: String,
        settlementId: String,
        fromUserId: String,
        toUserId: String,
        amountCents: Long?,
        currency: String?
    ) {
        val nudgeId = UUID.randomUUID().toString()
        val nudgeDocument = hashMapOf<String, Any?>(
            "id" to nudgeId,
            "settlementId" to settlementId,
            "groupId" to groupId,
            "fromUserId" to fromUserId,
            "toUserId" to toUserId,
            "createdAt" to Timestamp.now()
        ).apply {
            amountCents?.let { put("amountCents", it.toString()) }
            currency?.let { put("currency", it) }
        }

        firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(NUDGES_COLLECTION)
            .document(nudgeId)
            .set(nudgeDocument)
            .await()
    }

    private fun createSettlementsCollection(groupId: String) = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(SETTLEMENTS_COLLECTION)

    private companion object {
        const val SETTLEMENTS_COLLECTION = "settlements"
        const val NUDGES_COLLECTION = "nudges"
    }
}
