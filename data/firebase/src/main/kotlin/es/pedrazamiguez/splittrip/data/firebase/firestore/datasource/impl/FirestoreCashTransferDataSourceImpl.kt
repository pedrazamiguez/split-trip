package es.pedrazamiguez.splittrip.data.firebase.firestore.datasource.impl

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.CashTransferDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.document.GroupDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDocument
import es.pedrazamiguez.splittrip.data.firebase.firestore.mapper.toDomain
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudCashTransferDataSource
import es.pedrazamiguez.splittrip.domain.model.CashTransfer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class FirestoreCashTransferDataSourceImpl(
    private val firestore: FirebaseFirestore
) : CloudCashTransferDataSource {

    override suspend fun upsertCashTransfer(transfer: CashTransfer): Result<Unit> {
        return try {
            firestore
                .collection(GroupDocument.COLLECTION_PATH)
                .document(transfer.groupId)
                .collection(CashTransferDocument.COLLECTION_PATH)
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
            firestore
                .collection(GroupDocument.COLLECTION_PATH)
                .document(groupId)
                .collection(CashTransferDocument.COLLECTION_PATH)
                .document(transferId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchGroupCashTransfers(groupId: String): List<CashTransfer> {
        val snapshot = createTransfersCollection(groupId)
            .get()
            .await()

        return snapshot.documents
            .mapNotNull { it.toObject(CashTransferDocument::class.java)?.toDomain(it.id) }
            .sortedByDescending { it.createdAt }
    }

    override fun getGroupCashTransfersFlow(groupId: String): Flow<List<CashTransfer>> = callbackFlow {
        val transfersCollection = createTransfersCollection(groupId)

        val listener = createTransferListener(transfersCollection) { snapshot ->
            launch {
                val cachedTransfers = loadTransfersFromCache(
                    transfersCollection,
                    snapshot.documents
                )

                trySend(cachedTransfers)

                val cachedIds = cachedTransfers.map { it.id }.toSet()
                val missingIds = snapshot.documents
                    .map { it.id }
                    .filter { it !in cachedIds }

                if (missingIds.isNotEmpty()) {
                    val serverTransfers = loadTransfersFromServer(
                        transfersCollection,
                        missingIds
                    )
                    val allTransfers =
                        (cachedTransfers + serverTransfers).sortedByDescending { it.createdAt }
                    trySend(allTransfers)
                }
            }
        }

        awaitClose { listener.remove() }
    }

    override suspend fun verifyCashTransferOnServer(groupId: String, transferId: String): Boolean {
        val doc = firestore
            .collection(GroupDocument.COLLECTION_PATH)
            .document(groupId)
            .collection(CashTransferDocument.COLLECTION_PATH)
            .document(transferId)
            .get(Source.SERVER)
            .await()
        return doc.exists()
    }

    private fun createTransfersCollection(groupId: String): CollectionReference = firestore
        .collection(GroupDocument.COLLECTION_PATH)
        .document(groupId)
        .collection(CashTransferDocument.COLLECTION_PATH)

    private fun createTransferListener(
        transfersCollection: CollectionReference,
        onUpdate: (QuerySnapshot) -> Unit
    ) = transfersCollection.addSnapshotListener { snapshot, error ->
        if (error != null) {
            Timber.e(error, "Error listening to cash transfers")
            return@addSnapshotListener
        }
        snapshot?.let(onUpdate)
    }

    private suspend fun loadTransfersFromCache(
        transfersCollection: CollectionReference,
        documents: List<DocumentSnapshot>
    ): List<CashTransfer> = documents
        .mapNotNull { doc ->
            loadSingleTransferFromCache(transfersCollection, doc.id)
        }
        .sortedByDescending { it.createdAt }

    @Suppress("kotlin:S6518")
    private suspend fun loadSingleTransferFromCache(
        transfersCollection: CollectionReference,
        transferId: String
    ): CashTransfer? = try {
        val cachedDoc = transfersCollection
            .document(transferId)
            .get(Source.CACHE)
            .await()

        if (cachedDoc.exists()) {
            cachedDoc.toObject(CashTransferDocument::class.java)?.toDomain(cachedDoc.id)
        } else {
            null
        }
    } catch (e: Exception) {
        Timber.d(e, "Cache miss for cash transfer $transferId, will load from server")
        null
    }

    private suspend fun loadTransfersFromServer(
        transfersCollection: CollectionReference,
        missingIds: List<String>
    ): List<CashTransfer> = try {
        missingIds
            .chunked(FIRESTORE_WHERE_IN_LIMIT)
            .flatMap { batch ->
                transfersCollection
                    .whereIn(FieldPath.documentId(), batch)
                    .get(Source.SERVER)
                    .await()
                    .documents
                    .mapNotNull { it.toObject(CashTransferDocument::class.java)?.toDomain(it.id) }
            }
    } catch (e: Exception) {
        Timber.w(e, "Failed to load cash transfers from server")
        emptyList()
    }

    private companion object {
        const val FIRESTORE_WHERE_IN_LIMIT = 30
    }
}
