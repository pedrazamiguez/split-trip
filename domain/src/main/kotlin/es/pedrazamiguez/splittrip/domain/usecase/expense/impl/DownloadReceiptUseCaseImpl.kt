package es.pedrazamiguez.splittrip.domain.usecase.expense.impl

import es.pedrazamiguez.splittrip.domain.model.ReceiptAttachment
import es.pedrazamiguez.splittrip.domain.repository.ExpenseRepository
import es.pedrazamiguez.splittrip.domain.service.ReceiptStorageService
import es.pedrazamiguez.splittrip.domain.usecase.expense.DownloadReceiptUseCase

class DownloadReceiptUseCaseImpl(
    private val receiptStorageService: ReceiptStorageService,
    private val expenseRepository: ExpenseRepository
) : DownloadReceiptUseCase {

    /**
     * Downloads the remote file and updates the repository's local URI.
     *
     * @param expenseId The unique ID of the expense.
     * @param remoteUrl The remote download URL of the receipt.
     * @return The updated [ReceiptAttachment] containing the new local path.
     */
    override suspend operator fun invoke(
        expenseId: String,
        remoteUrl: String
    ): Result<ReceiptAttachment> = runCatching {
        val attachment = receiptStorageService.downloadAndStore(remoteUrl)
        expenseRepository.updateReceiptLocalUri(expenseId, attachment.localUri)
        attachment
    }
}
