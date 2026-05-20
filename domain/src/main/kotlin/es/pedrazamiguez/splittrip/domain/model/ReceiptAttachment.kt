package es.pedrazamiguez.splittrip.domain.model

/**
 * Holds the persisted references for a receipt file attached to an expense.
 *
 * @param localUri   Stable `file://` URI inside app-specific storage. Non-empty once the file
 *                   has been copied by [AttachReceiptUseCase]. Empty string indicates the file
 *                   was loaded from the cloud only (another device) and has not been downloaded.
 * @param mimeType   MIME type of the file (e.g. `image/webp`, `application/pdf`).
 * @param capturedAtMillis  Epoch millis when the file was attached to the expense draft.
 * @param remoteUrl  Firebase Storage download URL. Null until the background upload completes.
 */
data class ReceiptAttachment(
    val localUri: String,
    val mimeType: String,
    val capturedAtMillis: Long,
    val remoteUrl: String? = null
)
