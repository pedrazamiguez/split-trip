package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.usecase.expense.AttachReceiptUseCase

class AttachReceiptUseCaseImpl(
    private val receiptStorageService: ReceiptStorageService
) : AttachReceiptUseCase {

    /**
     * @param sourceUri The content:// or file:// URI returned by the OS picker.
     * @return A [ReceiptAttachment] with a stable `file://` local URI, MIME type, and timestamp.
     */
    override suspend operator fun invoke(sourceUri: String): Result<ReceiptAttachment> = runCatching {
        receiptStorageService.copyAndCompress(sourceUri)
    }
}
