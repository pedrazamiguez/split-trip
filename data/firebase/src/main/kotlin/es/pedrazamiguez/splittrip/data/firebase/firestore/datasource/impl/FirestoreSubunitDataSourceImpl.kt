package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.SubunitDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionDelta
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionFullSnapshot
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.observeCollectionDeltaFlow
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudSubunitDataSource
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.Subunit
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreSubunitDataSourceImpl(
    private val firestore: FirebaseFirestore,
    private val authenticationService: AuthenticationService
) : CloudSubunitDataSource {

    override suspend fun addSubunit(groupId: String, subunit: Subunit) {
        upsertSubunit(groupId, subunit)
    }

    override suspend fun updateSubunit(groupId: String, subunit: Subunit) {
        upsertSubunit(groupId, subunit)
    }

    override suspend fun deleteSubunit(groupId: String, subunitId: String) {
        createSubunitsCollection(groupId)
            .document(subunitId)
            .set(
                FirestoreSyncFields.buildSoftDeleteTombstone(
                    groupId = groupId,
                    deletedByUserId = authenticationService.currentUserId()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun fetchSubunitsFullSnapshot(groupId: String): DeltaSyncBatch<Subunit> =
        fetchCollectionFullSnapshot(
            collectionRef = createSubunitsCollection(groupId),
            mapDocument = { it.toObject(SubunitDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedBy { it.name } }
        )

    override suspend fun fetchSubunitsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<Subunit> =
        fetchCollectionDelta(
            collectionRef = createSubunitsCollection(groupId),
            sinceWatermark = sinceWatermark,
            mapDocument = { it.toObject(SubunitDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedBy { it.name } }
        )

    override fun observeSubunitsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<Subunit>> =
        observeCollectionDeltaFlow(
            collectionRef = createSubunitsCollection(groupId),
            sinceWatermark = sinceWatermark,
            entityLabel = "subunits",
            mapDocument = { it.toObject(SubunitDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedBy { it.name } }
        )

    override suspend fun verifySubunitOnServer(groupId: String, subunitId: String): Boolean {
        val doc = createSubunitsCollection(groupId)
            .document(subunitId)
            .get(Source.SERVER)
            .await()
        return doc.exists() && doc.getBoolean(FirestoreSyncFields.IS_DELETED) != true
    }

    private suspend fun upsertSubunit(groupId: String, subunit: Subunit) {
        val userId = authenticationService.requireUserId()
        val subunitDocument = subunit.toDocument(subunit.id, groupId, userId)
        createSubunitsCollection(groupId)
            .document(subunit.id)
            .set(subunitDocument)
            .await()
    }

    private fun createSubunitsCollection(groupId: String) = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(SubunitDocument.COLLECTION_PATH)
}
