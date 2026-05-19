package es.pedrazamiguez.splittrip.data.firebase.storage

import com.google.firebase.storage.FirebaseStorage
import es.pedrazamiguez.splittrip.domain.datasource.cloud.CloudStorageDataSource
import java.io.File
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Uploads receipt files to Firebase Cloud Storage under the `receipts/{expenseId}/` prefix
 * and returns a long-lived download URL.
 *
 * The upload is idempotent: calling with the same [expenseId] replaces the previous object
 * rather than creating a new one, which prevents orphaned objects on retry.
 */
internal class CloudStorageDataSourceImpl(
    private val storage: FirebaseStorage
) : CloudStorageDataSource {

    override suspend fun uploadReceipt(
        expenseId: String,
        localPath: String,
        mimeType: String
    ): String {
        val file = File(localPath)
        val extension = file.extension.ifBlank { "bin" }
        val ref = storage.reference.child("$RECEIPTS_PREFIX/$expenseId/receipt.$extension")

        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()

        ref.putFile(android.net.Uri.fromFile(file), metadata).await()
        val downloadUrl = ref.downloadUrl.await().toString()
        Timber.d("Receipt uploaded for expense $expenseId → $downloadUrl")
        return downloadUrl
    }

    private companion object {
        const val RECEIPTS_PREFIX = "receipts"
    }
}
