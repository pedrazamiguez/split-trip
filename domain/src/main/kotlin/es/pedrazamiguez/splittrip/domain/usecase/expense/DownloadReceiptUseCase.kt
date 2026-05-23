package es.pedrazamiguez.splittrip.domain.usecase.expense

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService

/**
 * Downloads a receipt file from [remoteUrl] and persists its local path [localUri]
 * in Room for the given [expenseId].
 *
 * This ensures subsequent access to the receipt (thumbnail preview or full viewer)
 * runs offline-first.
 */
class DownloadReceiptUseCase(
    private val receiptStorageService: ReceiptStorageService,
    private val expenseRepository: ExpenseRepository
) {
    /**
     * Downloads the remote file and updates the repository's local URI.
     *
     * @param expenseId The unique ID of the expense.
     * @param remoteUrl The remote download URL of the receipt.
     * @return The updated [ReceiptAttachment] containing the new local path.
     */
    suspend operator fun invoke(expenseId: String, remoteUrl: String): Result<ReceiptAttachment> = runCatching {
        val attachment = receiptStorageService.downloadAndStore(remoteUrl)
        expenseRepository.updateReceiptLocalUri(expenseId, attachment.localUri)
        attachment
    }
}
