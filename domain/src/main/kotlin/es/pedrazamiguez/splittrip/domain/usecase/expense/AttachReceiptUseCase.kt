package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService

/**
 * Copies the file at [sourceUri] into app-specific storage, optionally compressing
 * images to WebP, and returns a [ReceiptAttachment] that the form state can store.
 *
 * This use case is the clean boundary between the ViewModel and Android file I/O.
 * The ViewModel injects this use case and never touches [android.content.ContentResolver]
 * or platform file APIs directly.
 *
 * Called from [FormEventHandler] when the user picks a receipt via camera, gallery,
 * or document picker and the resulting URI is handed back to the Feature.
 */
class AttachReceiptUseCase(
    private val receiptStorageService: ReceiptStorageService
) {
    /**
     * @param sourceUri The content:// or file:// URI returned by the OS picker.
     * @return A [ReceiptAttachment] with a stable `file://` local URI, MIME type, and timestamp.
     */
    suspend operator fun invoke(sourceUri: String): Result<ReceiptAttachment> = runCatching {
        receiptStorageService.copyAndCompress(sourceUri)
    }
}
