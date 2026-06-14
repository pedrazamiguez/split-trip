package es.pedrazamiguez.splittrip.domain.datasource.cloud

/**
 * Uploads receipts (images or PDFs) to remote object storage, returning a stable
 * download URL that other devices can use to fetch the file.
 *
 * Implemented in `:data:firebase` using Firebase Cloud Storage.
 */
interface CloudStorageDataSource {

    /**
     * Uploads the file at [localPath] to the `receipts/{expenseId}/` prefix in
     * Cloud Storage and returns a public download URL.
     *
     * The call is idempotent — uploading the same file twice for the same expense
     * replaces the previous object.
     *
     * @param expenseId Unique expense identifier used as the storage path prefix.
     * @param localPath Absolute filesystem path to the local file.
     * @param mimeType  MIME type of the file (e.g. `image/webp`, `application/pdf`).
     * @return A publicly accessible HTTPS download URL.
     */
    suspend fun uploadReceipt(expenseId: String, localPath: String, mimeType: String): String

    /**
     * Deletes all remote receipt objects associated with the given [expenseId].
     *
     * @param expenseId Unique expense identifier.
     */
    suspend fun deleteReceipt(expenseId: String)

    /**
     * Uploads the user avatar at [localPath] to the `avatars/{userId}/` path in Cloud Storage.
     * @return Public HTTPS download URL.
     */
    suspend fun uploadAvatar(userId: String, localPath: String, mimeType: String): String

    /**
     * Deletes the user avatar associated with [userId].
     */
    suspend fun deleteAvatar(userId: String)

    /**
     * Uploads the group image at [localPath] to the `groups/{groupId}/` path in Cloud Storage.
     * @return Public HTTPS download URL.
     */
    suspend fun uploadGroupImage(groupId: String, localPath: String, mimeType: String): String

    /**
     * Deletes the group cover image associated with [groupId].
     */
    suspend fun deleteGroupImage(groupId: String)
}
