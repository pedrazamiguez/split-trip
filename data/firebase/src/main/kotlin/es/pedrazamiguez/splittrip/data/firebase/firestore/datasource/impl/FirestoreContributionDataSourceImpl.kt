package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.ContributionDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.FirestoreSyncFields
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionDelta
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.fetchCollectionFullSnapshot
import es.pedrazamiguez.splittrip.data.firebase.firestore.sync.observeCollectionDeltaFlow
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudContributionDataSource
import es.pedrazamiguez.splittrip.domain.model.Contribution
import es.pedrazamiguez.splittrip.domain.model.DeltaSyncBatch
import es.pedrazamiguez.splittrip.domain.model.SyncWatermark
import es.pedrazamiguez.splittrip.domain.service.AuthenticationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class FirestoreContributionDataSourceImpl(
    private val firestore: FirebaseFirestore,
    private val authenticationService: AuthenticationService
) : CloudContributionDataSource {

    override suspend fun addContribution(groupId: String, contribution: Contribution) {
        val userId = authenticationService.requireUserId()
        val contributionDocument = contribution.toDocument(contribution.id, groupId, userId)
        createContributionsCollection(groupId)
            .document(contribution.id)
            .set(contributionDocument)
            .await()
    }

    override suspend fun deleteContribution(groupId: String, contributionId: String) {
        createContributionsCollection(groupId)
            .document(contributionId)
            .set(
                FirestoreSyncFields.buildSoftDeleteTombstone(
                    groupId = groupId,
                    deletedByUserId = authenticationService.currentUserId()
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun fetchContributionsFullSnapshot(groupId: String): DeltaSyncBatch<Contribution> =
        fetchCollectionFullSnapshot(
            collectionRef = createContributionsCollection(groupId),
            mapDocument = { it.toObject(ContributionDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override suspend fun fetchContributionsDelta(
        groupId: String,
        sinceWatermark: SyncWatermark
    ): DeltaSyncBatch<Contribution> =
        fetchCollectionDelta(
            collectionRef = createContributionsCollection(groupId),
            sinceWatermark = sinceWatermark,
            mapDocument = { it.toObject(ContributionDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override fun observeContributionsDeltaFlow(
        groupId: String,
        sinceWatermark: SyncWatermark?
    ): Flow<DeltaSyncBatch<Contribution>> =
        observeCollectionDeltaFlow(
            collectionRef = createContributionsCollection(groupId),
            sinceWatermark = sinceWatermark,
            entityLabel = "contributions",
            mapDocument = { it.toObject(ContributionDocument::class.java)?.toDomain() },
            sort = { list -> list.sortedByDescending { it.createdAt ?: it.lastUpdatedAt } }
        )

    override suspend fun verifyContributionOnServer(
        groupId: String,
        contributionId: String
    ): Boolean {
        val doc = createContributionsCollection(groupId)
            .document(contributionId)
            .get(Source.SERVER)
            .await()
        return doc.exists() && doc.getBoolean(FirestoreSyncFields.IS_DELETED) != true
    }

    private fun createContributionsCollection(groupId: String) = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(ContributionDocument.COLLECTION_PATH)
}
