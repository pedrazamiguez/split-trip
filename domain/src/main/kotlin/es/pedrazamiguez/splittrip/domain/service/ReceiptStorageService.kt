package es.pedrazamiguez.splittrip.domain.service

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment

/**
 * Copies a user-selected file from the source URI into app-specific storage and,
 * for images, compresses it to WebP to reduce disk usage.
 *
 * The interface lives in `:domain` so the ViewModel can depend on [AttachReceiptUseCase]
 * without any Android dependency. The Android-aware implementation lives in `:data:local`.
 */
interface ReceiptStorageService {

    /**
     * Reads the file at [sourceUri] (a content:// or file:// URI), copies it to
     * `filesDir/receipts/`, and returns a [ReceiptAttachment] with the stable local path.
     *
     * Images are re-encoded to WebP (lossless) to reduce storage. PDFs and other
     * document types are copied verbatim.
     *
     * @throws IllegalArgumentException if [sourceUri] cannot be resolved.
     * @throws IllegalStateException if the file could not be written to app storage.
     */
    suspend fun copyAndCompress(sourceUri: String): ReceiptAttachment

    /**
     * Downloads the file from [remoteUrl] and stores it locally under `filesDir/receipts/`.
     *
     * @param remoteUrl The download URL of the remote receipt file.
     * @return A [ReceiptAttachment] with the stable `file://` local URI.
     */
    suspend fun downloadAndStore(remoteUrl: String): ReceiptAttachment

    /**
     * Deletes the local receipt file at the specified [localUri].
     *
     * @param localUri Stable local URI (file://) of the receipt.
     */
    suspend fun deleteLocalFile(localUri: String)
}
